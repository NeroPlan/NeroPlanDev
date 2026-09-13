"""뇌과학 논문 후보에 MMR을 적용해 최종 추천을 고르는 로직 - 삭제 예정"""

import logging
from typing import Any, Dict, List

import numpy as np

from neuro_paper_rag.domain.ranking_paper import score_papers
from shared.settings import config

logger = logging.getLogger(__name__)


def maximal_marginal_relevance(
    query_vector: np.ndarray,
    candidate_vectors: np.ndarray,
    selected_indices: List[int],
    lambda_: float = 0.5,
) -> int:
    """관련성과 다양성을 함께 고려해 다음으로 고를 논문 인덱스를 정합니다."""
    remaining = [i for i in range(len(candidate_vectors)) if i not in selected_indices]
    if not remaining:
        return -1

    query_sims = np.dot(candidate_vectors[remaining], query_vector)
    if not selected_indices:
        return remaining[int(np.argmax(query_sims))]

    selected_vecs = candidate_vectors[selected_indices]
    redundancy = np.max(np.dot(candidate_vectors[remaining], selected_vecs.T), axis=1)
    mmr_scores = lambda_ * query_sims - (1 - lambda_) * redundancy
    best_local_index = int(np.argmax(mmr_scores))
    return remaining[best_local_index]


def rank_papers(
    papers: List[Dict[str, Any]],
    query_vector: np.ndarray,
    candidate_vectors: np.ndarray,
    user_plan_keywords: List[str] = None,
    top_n: int = None,
    cfg=None,
) -> List[Dict[str, Any]]:
    """복합 점수와 MMR을 함께 적용해 최종 추천 목록을 만듭니다."""
    cfg = cfg or config.scoring
    top_n = top_n or cfg.top_n_recommend
    scored = score_papers(
        papers=papers,
        candidate_vectors=candidate_vectors,
        user_plan_keywords=user_plan_keywords,
        cfg=cfg,
    )
    sorted_vectors = np.array([item["_vector"] for item in scored]) if scored else np.array([])

    selected_indices: List[int] = []
    selected_papers: List[Dict[str, Any]] = []
    for _ in range(min(top_n, len(scored))):
        next_index = maximal_marginal_relevance(
            query_vector=query_vector,
            candidate_vectors=sorted_vectors,
            selected_indices=selected_indices,
            lambda_=cfg.mmr_lambda,
        )
        if next_index < 0:
            break
        selected_indices.append(next_index)
        paper = scored[next_index].copy()
        paper["final_score"] = float(paper["_raw_score"])
        paper.pop("_raw_score", None)
        paper.pop("_vector", None)
        selected_papers.append(paper)

    logger.info(
        "Paper recommendation complete: %s candidates -> %s selected",
        len(scored),
        len(selected_papers),
    )
    # MMR은 다양성을 고려해 "무엇을 고를지"만 결정하고,
    # 화면에 보여줄 순서는 실제 관련도 점수(final_score) 내림차순으로 재정렬합니다.
    selected_papers.sort(key=lambda paper: paper["final_score"], reverse=True)
    return selected_papers
