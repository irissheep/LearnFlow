from pydantic import BaseModel, Field

class NoteIdentity(BaseModel):
    user_id: int = Field(gt=0)
    note_id: int = Field(gt=0)
    revision: int = Field(gt=0)

class IndexRequest(NoteIdentity):
    title: str = Field(min_length=1, max_length=200)
    content: str = Field(min_length=1, max_length=100000)

class ManifestEntry(BaseModel):
    note_id: int = Field(gt=0)
    revision: int = Field(gt=0)

class QueryRequest(BaseModel):
    user_id: int = Field(gt=0)
    question: str = Field(min_length=1, max_length=2000)
    notes: list[ManifestEntry]

class Source(BaseModel):
    # camelCase 与 Java / Vue 的响应契约一致。
    noteId: int
    title: str
    excerpt: str

class Answer(BaseModel):
    answer: str
    sources: list[Source]
    mode: str
