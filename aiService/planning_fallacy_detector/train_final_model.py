"""
위험 예측 모델 학습 + 저장.
- 합성 페르소나 20명(클러스터 강건 추론 최소 권장치, Cameron & Miller 2015)의 과제 이력으로 학습
- GroupKFold(페르소나 단위 5분할) out-of-fold 확률로 성능(AUC, AP + 페르소나 클러스터 붓스트랩 95% CI) 평가,
  Platt scaling 보정 계수 학습, 판정 임계값(precision 0.8 이상인 최저값) 결정
- 최종 모델은 전체 데이터로 다시 학습해 보정 래퍼(PlattCalibratedModel)로 감싸 final_bias_model.joblib에 저장
- 확인용 합성 데이터는 synthetic_training_data.xlsx로 저장(data 시트 + 컬럼 설명 시트)

실행: python3 train_final_model.py
"""
import os

import joblib
import numpy as np
import pandas as pd
from openpyxl.styles import Alignment, Font
from bias_data_pipeline import (
    MODEL_FEATURES, PlattCalibratedModel, add_features, bootstrap_ci, build_pipeline, calibrate, fit_platt,
    generate_raw_events, make_random_personas,
)
from sklearn.metrics import average_precision_score, brier_score_loss, precision_recall_curve, roc_auc_score
from sklearn.model_selection import GroupKFold, cross_val_predict

N_PERSONAS = 20
SEED = 42
N_BOOTSTRAP = 2000
OUT_DIR = os.path.dirname(os.path.abspath(__file__))  # 결과 파일은 이 스크립트와 같은 폴더에 저장


def save_data_workbook(data: pd.DataFrame, path: str = os.path.join(OUT_DIR, "synthetic_training_data.xlsx")):
    """확인용 엑셀 저장: data 시트(원본 행) + 컬럼설명 시트(컬럼 뜻 + 산출식)."""
    with pd.ExcelWriter(path, engine="openpyxl") as writer:
        data.to_excel(writer, sheet_name="data", index=False)

        columns_info = [
            ("persona_id", "어떤 가상 사용자(페르소나)의 기록인지. persona_0이면 0번째로 생성된 페르소나."),
            ("task_index", "그 페르소나 안에서 몇 번째 과제인지(0부터 시작, 시간 순서)"),
            ("category", "과제 카테고리(운동/공부/업무/개인일정)"),
            ("time_slot", "합성 데이터에만 있는 시간대 값(모델·판정에는 쓰지 않음)"),
            ("estimated_minutes", "예상 소요시간(30/60/90/120분 중 하나)"),
            ("outcome", "실제 결과(완료/미룸/실패)"),
            ("y", "모델 학습용 타겟값. outcome이 완료면 0, 미룸이나 실패면 1(둘 다 \"안 지켜짐\"으로 묶어서 1)"),
            ("category_relative_rate", "이 과제 시점까지, 이 카테고리 완료율이 본인 전체 완료율보다 얼마나 높거나 낮은지(전체 누적 기록 기준, 카테고리 기록 3건 미만이면 0)"),
            ("recent_failure_streak", "이 과제 시점까지, 같은 카테고리에서 직전까지 연속으로 몇 번 안 지켰는지"),
            ("duration_bin", "estimated_minutes를 짧음(30분 이하)/보통(31~60분)/김(60분 초과) 3구간으로 나눈 값 — 모델 입력"),
            ("streak_bin", "recent_failure_streak를 0회/1~2회/3회 이상 3구간으로 나눈 값 — 모델 입력"),
        ]
        df_columns = pd.DataFrame(columns_info, columns=["컬럼", "뜻"])
        df_columns.to_excel(writer, sheet_name="컬럼설명", index=False, startrow=0)

        formula_rows = [
            "",
            "category_relative_rate 산출식",
            "category_relative_rate = cat_rate − overall_rate",
            "  cat_rate = mean(이 시점 직전까지, 같은 카테고리에서 전체 기록의 완료여부(완료=1, 그외=0))",
            "  overall_rate = mean(이 시점 직전까지, 카테고리 무관 전체 기록의 완료여부(완료=1, 그외=0))",
            "  단, 이 카테고리 기록이 3건 미만이면 → category_relative_rate = 0",
            "",
            "recent_failure_streak 산출식",
            "recent_failure_streak = 직전 같은 카테고리 과제부터 거슬러 올라가며 연속으로 '완료가 아님'이 이어진 횟수",
            "  완료(outcome=완료)가 한 번이라도 나오면 그 시점에서 0으로 리셋되고, 그 다음부터 다시 카운트",
            "  이력이 없으면(콜드스타트) → recent_failure_streak = 0",
        ]
        ws = writer.sheets["컬럼설명"]
        start = len(df_columns) + 3
        for i, text in enumerate(formula_rows):
            if text:
                ws.cell(row=start + i, column=1, value=text)

        ws.column_dimensions["A"].width = 26
        ws.column_dimensions["B"].width = 110
        for row in ws.iter_rows(min_row=1, max_row=len(df_columns) + 1, min_col=2, max_col=2):
            for cell in row:
                cell.alignment = Alignment(wrap_text=True, vertical="top")
        for cell in ws[1]:
            cell.font = Font(bold=True)

    print(f"합성데이터 저장 완료: {os.path.basename(path)} ({len(data)}행, 시트: data, 컬럼설명)")


personas = make_random_personas(N_PERSONAS, seed=SEED)
raw = generate_raw_events(personas, seed=SEED + 1000)
data = add_features(raw)

# 확인용: 학습에 쓴 합성 데이터를 엑셀로 저장
save_data_workbook(data)

y = data["y"].values
groups = data["persona_id"].values
X = data[MODEL_FEATURES]

# 페르소나 단위 5분할 교차검증 out-of-fold 확률 (4묶음으로 학습, 안 본 1묶음 예측을 5번 반복)
gkf = GroupKFold(n_splits=5)
raw_oof = cross_val_predict(build_pipeline(), X, y, cv=gkf.split(X, y, groups), method="predict_proba")[:, 1]

# Platt scaling: class_weight="balanced"로 어긋난 확률을 실제 빈도에 맞게 보정.
# 단조변환이라 AUC·AP와 판정 순위는 그대로이고, 화면에 보이는 확률 숫자만 실제 빈도에 맞춰짐.
platt_a, platt_b = fit_platt(raw_oof, y)
probs_oof = calibrate(raw_oof, platt_a, platt_b)

precisions, recalls, thresholds = precision_recall_curve(y, probs_oof)
valid_idx = np.where(precisions[:-1] >= 0.8)[0]
threshold = thresholds[valid_idx[np.argmin(thresholds[valid_idx])]] if len(valid_idx) else 0.5

# 점추정치 + 페르소나 클러스터 붓스트랩 95% CI
auc, auc_boot_mean, auc_ci_low, auc_ci_high = bootstrap_ci(groups, y, probs_oof, roc_auc_score, n_boot=N_BOOTSTRAP, seed=1)
ap, ap_boot_mean, ap_ci_low, ap_ci_high = bootstrap_ci(groups, y, probs_oof, average_precision_score, n_boot=N_BOOTSTRAP, seed=2)

print(f"[페르소나 {N_PERSONAS}개 기준] OOF 성능 (점추정치, 95% CI)")
print(f"  AUC = {auc:.3f}  (95% CI {auc_ci_low:.3f}-{auc_ci_high:.3f})")
print(f"  AP  = {ap:.3f}  (95% CI {ap_ci_low:.3f}-{ap_ci_high:.3f})")
print(f"  채택임계값 = {threshold:.3f} (보정 확률 기준, precision-recall curve, precision>=0.8 최저점)")
print("[확률 보정(Platt scaling) 전/후]")
print(f"  실제 미완료 비율 = {y.mean():.3f}")
print(f"  평균 예측확률: 보정 전 {raw_oof.mean():.3f} → 보정 후 {probs_oof.mean():.3f}")
print(f"  Brier score(낮을수록 좋음): 보정 전 {brier_score_loss(y, raw_oof):.4f} → 보정 후 {brier_score_loss(y, probs_oof):.4f}")

# 최종 모델: 전체 데이터로 다시 학습 후 보정 래퍼로 감쌈
base_model = build_pipeline()
base_model.fit(X, y)
final_model = PlattCalibratedModel(base_model, platt_a, platt_b)

joblib.dump({
    "model": final_model,
    "threshold": float(threshold),
    "auc": float(auc), "auc_ci": [float(auc_ci_low), float(auc_ci_high)],
    "ap": float(ap), "ap_ci": [float(ap_ci_low), float(ap_ci_high)],
    "n_personas": N_PERSONAS,
    "calibration": {"method": "platt", "a": platt_a, "b": platt_b},
}, os.path.join(OUT_DIR, "final_bias_model.joblib"))
print("저장 완료: final_bias_model.joblib")