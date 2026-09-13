import json
from pathlib import Path
from typing import Any

from shared.settings import config


def get_artifact_dir() -> Path:
    artifact_dir = Path(config.neuro_paper_rag_storage.artifact_dir)
    artifact_dir.mkdir(parents=True, exist_ok=True)
    return artifact_dir


def get_artifact_path(filename: str) -> Path:
    return get_artifact_dir() / filename


def save_json_artifact(filename: str, payload: Any) -> Path:
    path = get_artifact_path(filename)
    path.write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    return path


def save_text_artifact(filename: str, text: str) -> Path:
    path = get_artifact_path(filename)
    path.write_text(text, encoding="utf-8")
    return path
