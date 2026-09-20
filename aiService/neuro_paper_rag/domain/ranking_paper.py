"""뇌과학 논문 후보의 원시 점수를 계산하는 로직"""

import math
from datetime import datetime
from typing import Any, Dict, List
from shared.settings import config

CURRENT_YEAR = datetime.now().year


def compute_recency_score(published_year: int, decay_rate: float = 0.15) -> float:
    """출판 연도가 최근일수록 높은 점수를 주는 최신성 점수입니다."""
    age = max(0, CURRENT_YEAR - published_year)
    return math.exp(-decay_rate * age)


def compute_plan_relevance_score(
    paper_text: str,
    user_plan_keywords: List[str],
) -> float:
    """논문 내용이 사용자 계획 키워드와 얼마나 겹치는지 계산합니다."""
    if not user_plan_keywords:
        return 0.5

    paper_lower = paper_text.lower()
    matched = sum(1 for keyword in user_plan_keywords if keyword.lower() in paper_lower)
    return matched / len(user_plan_keywords)


def score_papers(
    papers: List[Dict[str, Any]],
    user_plan_keywords: List[str] = None,
    cfg=None,
) -> List[Dict[str, Any]]:
    """후보 논문에 복합 점수를 계산해 정렬 가능한 목록으로 만듭니다."""
    cfg = cfg or config.scoring
    user_plan_keywords = user_plan_keywords or []

    scored = []
    for paper in papers:
        semantic_sim = float(paper.get("score", 0.0))
        recency = compute_recency_score(paper.get("published_year", 2015))
        plan_relevance = compute_plan_relevance_score(
            f"{paper.get('title', '')} {paper.get('summary', '')}",
            user_plan_keywords,
        )
        raw_score = (
            cfg.semantic_similarity_weight * semantic_sim
            + cfg.recency_weight * recency
            + cfg.relevance_to_plan_weight * plan_relevance
        )
        scored.append({
                **paper,
                "_raw_score": raw_score,
                "_score_breakdown": {  # ← 추가
                    "semantic_sim": semantic_sim,
                    "recency": recency,
                    "plan_relevance": plan_relevance,
                },
        })

    scored.sort(key=lambda item: item["_raw_score"], reverse=True)
    return scored


def rank_papers(
    papers: List[Dict[str, Any]],
    user_plan_keywords: List[str] = None,
    top_n: int = None,
    cfg=None,
) -> List[Dict[str, Any]]:
    """복합 점수로 정렬한 뒤 top_n개를 그대로 골라 최종 추천 목록을 만듭니다."""
    cfg = cfg or config.scoring
    top_n = top_n or cfg.top_n_recommend

    scored = score_papers(papers=papers, user_plan_keywords=user_plan_keywords, cfg=cfg)

    selected_papers = []
    for item in scored[:top_n]:
        paper = item.copy()
        paper["final_score"] = float(paper.pop("_raw_score"))
        selected_papers.append(paper)

    return selected_papers