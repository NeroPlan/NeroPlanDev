from typing import Optional, TypedDict


class TaskItem(TypedDict):
    task_id: str
    title: str
    created_at: str
    deadline: Optional[str]
    estimated_minutes: int
    actual_minutes: int
    completed: bool
    completed_at: Optional[str]
    category: str
    ai_priority: int
    user_actual_priority: int
