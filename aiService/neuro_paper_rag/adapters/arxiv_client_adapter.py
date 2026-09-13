import logging
import time
from typing import Any, Dict, List, Optional

from neuro_paper_rag.domain.neuro_paper_rules import (
    classify_domain_for_neuroscience,
    normalize_text,
)
from shared.settings import config

logger = logging.getLogger(__name__)


class ArxivPaperClient:

    def __init__(
        self,
        page_size: int = 100,
        delay_seconds: Optional[float] = None,
        num_retries: int = 3,
    ):

        self.page_size = page_size
        self.delay_seconds = (
            config.arxiv.request_delay_sec if delay_seconds is None else delay_seconds
        )
        self.num_retries = num_retries

    def fetch_recent_papers(
        self,
        categories: Optional[List[str]] = None,
        max_results_per_category: Optional[int] = None,
    ) -> List[Dict[str, Any]]:

        import arxiv

        categories = categories or config.arxiv.categories
        max_results = max_results_per_category or config.arxiv.max_results_per_category
        client = arxiv.Client(
            page_size=self.page_size,
            delay_seconds=self.delay_seconds,
            num_retries=self.num_retries,
        )

        papers: List[Dict[str, Any]] = []
        for category in categories:
            search = arxiv.Search(
                query=f"cat:{category}",
                max_results=max_results,
                sort_by=arxiv.SortCriterion.SubmittedDate,
                sort_order=arxiv.SortOrder.Descending,
            )

            for result in client.results(search):
                papers.append(self._paper_from_result(result))

            if self.delay_seconds:
                time.sleep(self.delay_seconds)

        logger.info("ArXiv fetch complete: %s papers", len(papers))
        return papers

    def _paper_from_result(self, result: Any) -> Dict[str, Any]:
        categories = list(getattr(result, "categories", []) or [])
        title = normalize_text(getattr(result, "title", ""))
        summary = normalize_text(getattr(result, "summary", ""))
        authors = ", ".join(author.name for author in getattr(result, "authors", []))
        published = getattr(result, "published", None)
        published_year = published.year if published else 0
        domain = classify_domain_for_neuroscience(title, summary, categories)

        return {
            "source_id": getattr(result, "entry_id", ""),
            "title": title,
            "summary": summary,
            "published_year": published_year,
            "authors": authors,
            "domain": domain,
            "pdf_url": getattr(result, "pdf_url", "") or "",
            "primary_category": categories[0] if categories else "",
            "categories": categories,
        }
