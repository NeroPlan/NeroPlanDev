"""뇌과학 논문 도메인 규칙"""

import re
from typing import List, Optional

NEURO_KEYWORDS = [
    "brain",
    "neuroscience",
    "neural",
    "cognitive",
    "fmri",
    "eeg",
    "cortex",
    "synapse",
    "dopamine",
    "prefrontal",
    "hippocampus",
    "memory",
    "attention",
    "perception",
    "learning",
    "sleep",
    "neuroplasticity",
    "executive function",
    "working memory",
]


def normalize_text(text: str) -> str:
    """제목과 초록의 공백을 정리해 검색 친화적인 문자열로 만듭니다."""
    return re.sub(r"\s+", " ", (text or "").strip())


def classify_domain_for_neuroscience(
    title: str,
    summary: str,
    categories: Optional[List[str]] = None,
) -> str:
    """카테고리와 키워드를 함께 보고 뇌과학 도메인 여부를 판정합니다."""
    text = f"{title} {summary}".lower()
    categories = categories or []

    if "q-bio.NC" in categories:
        return "neuroscience"

    neuro_hits = 0
    for keyword in NEURO_KEYWORDS:
        if keyword in text:
            neuro_hits += 1
        if neuro_hits >= 2:
            return "neuroscience"

    if "cs.NE" in categories:
        return "cs.NE"

    return "general"
