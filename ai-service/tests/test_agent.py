from pathlib import Path
from types import SimpleNamespace
import pytest
from fastapi.testclient import TestClient
from app.config import Settings
from app.main import create_app
from app.agent import KnowledgeAgent

TOKEN = "test-internal-token-for-learnflow-123456"
HEADERS = {"X-Internal-Token": TOKEN}

@pytest.fixture
def client(tmp_path: Path):
    with TestClient(create_app(Settings(ai_service_token=TOKEN, chroma_dir=tmp_path, ai_provider="local"))) as client:
        yield client

def index(client, user=1, note=10, revision=1, content="Spring 事务发生运行时异常时会回滚。"):
    return client.post("/internal/index", headers=HEADERS,
                       json={"user_id": user, "note_id": note, "revision": revision,
                             "title": "事务笔记", "content": content})

def ask(client, user=1, note=10, revision=1):
    return client.post("/internal/query", headers=HEADERS,
                      json={"user_id": user, "question": "Spring 事务如何回滚",
                            "notes": [{"note_id": note, "revision": revision}]})

def test_real_local_index_and_citations(client):
    assert index(client).status_code == 200
    answer = ask(client).json()
    assert answer["mode"] == "local"
    assert answer["sources"][0]["noteId"] == 10
    assert "回滚" in answer["answer"]

def test_user_and_manifest_isolation(client):
    index(client)
    # 即便内部请求错误地带了其他人的 noteId，用户过滤仍阻止泄漏。
    assert ask(client, user=2).json()["sources"] == []
    assert client.post("/internal/query", headers=HEADERS,
                       json={"user_id": 1, "question": "事务回滚", "notes": []}).json()["sources"] == []

def test_stale_revision_is_never_used(client):
    index(client, revision=2)
    assert ask(client, revision=1).json()["sources"] == []
    assert index(client, revision=1, content="不应出现的旧内容").json()["indexed"] is False
    assert "不应出现" not in ask(client, revision=2).json()["answer"]

def test_deleted_note_cannot_be_resurrected(client):
    index(client)
    assert client.post("/internal/delete", headers=HEADERS,
                       json={"user_id": 1, "note_id": 10, "revision": 2}).status_code == 200
    assert index(client, revision=1).json()["indexed"] is False
    assert ask(client).json()["sources"] == []

def test_internal_endpoints_require_token(client):
    assert client.post("/internal/query", json={"user_id": 1, "question": "事务", "notes": []}).status_code == 401
    assert client.get("/health").json()["status"] == "UP"


def test_model_embeddings_use_raw_text_and_supported_batch_size(tmp_path: Path):
    # 替换 SDK 请求端，验证兼容接口拿到原文且每批不超过 10 条，不产生模型费用。
    agent = KnowledgeAgent(Settings(ai_service_token=TOKEN, chroma_dir=tmp_path,
        ai_provider="openai", openai_api_key="test-api-key-placeholder"))
    assert agent.chain is not None
    calls = []

    def embed(*, input, **kwargs):
        calls.append(input)
        return {"data": [{"embedding": [1.0, 0.0]} for _ in input]}

    agent.store.embeddings.client = SimpleNamespace(create=embed)
    texts = [f"第 {i} 段事务笔记" for i in range(11)]
    vectors = agent.store.embeddings.embed_documents(texts)
    assert calls == [texts[:10], texts[10:]]
    assert vectors == [[1.0, 0.0]] * len(texts)
