-- 집중 타이머로 실제로 집중한 시간(분) - 여러 번 나눠 집중하면 누적, 타이머를 쓰지 않은 계획은 null
-- 예상 소요시간(estimated_minutes)과 비교해 계획 오류(planning fallacy)를 분석 페이지에서 보여줌
ALTER TABLE plan ADD COLUMN IF NOT EXISTS actual_minutes integer;
