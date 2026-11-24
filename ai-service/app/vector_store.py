import hashlib
import sqlite3
import threading
import chromadb
from chromadb.config import Settings as ChromaSettings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from .config import Settings
from .embeddings import LocalHashEmbeddings
from .schemas import IndexRequest, NoteIdentity, QueryRequest, Source

class NoteVectorStore:
    def __init__(self, settings: Settings):
        self.settings = settings
        directory = settings.chroma_dir.resolve()
        directory.mkdir(parents=True, exist_ok=True)
        self.state_path = directory / "learnflow-index-state.sqlite3"
        self.lock = threading.RLock()
        if settings.ai_provider == "local":
            self.embeddings = LocalHashEmbeddings()
            namespace = "local-hash-v1"
        else:
            from langchain_openai import OpenAIEmbeddings
            self.embeddings = OpenAIEmbeddings(
                model=settings.embedding_model,
                api_key=settings.openai_api_key.get_secret_value(),
                base_url=settings.openai_base_url,
                # 已按 800 字符分块；兼容服务接收原文，不发送 OpenAI 的 token 编号。
                check_embedding_ctx_length=False,
                # 百炼 text-embedding-v4 一次最多接收 10 条文本。
                chunk_size=10,
                request_timeout=10, max_retries=0,
            )
            namespace = settings.openai_base_url + ":" + settings.embedding_model
        # 不同模型的向量维度/空间不能混用；切换模型后需在页面点击“重建索引”。
        self.collection_name = "learnflow-" + hashlib.sha256(namespace.encode()).hexdigest()[:16]
        self.client = chromadb.PersistentClient(
            path=str(directory), settings=ChromaSettings(anonymized_telemetry=False))
        self.collection = self.client.get_or_create_collection(
            self.collection_name, metadata={"hnsw:space": "cosine"}, embedding_function=None)
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=800, chunk_overlap=120,
            separators=["\n## ", "\n\n", "\n", "。", " ", ""],
        )
        with sqlite3.connect(self.state_path) as db:
            db.execute("""CREATE TABLE IF NOT EXISTS note_state (
                collection TEXT NOT NULL, user_id INTEGER NOT NULL, note_id INTEGER NOT NULL,
                revision INTEGER NOT NULL, deleted INTEGER NOT NULL,
                PRIMARY KEY(collection, user_id, note_id))""")

    def _revision(self, note: NoteIdentity) -> int:
        with sqlite3.connect(self.state_path) as db:
            row = db.execute(
                "SELECT revision FROM note_state WHERE collection=? AND user_id=? AND note_id=?",
                (self.collection_name, note.user_id, note.note_id)).fetchone()
        return row[0] if row else 0

    def _record(self, note: NoteIdentity, deleted: bool):
        with sqlite3.connect(self.state_path) as db:
            db.execute("INSERT OR REPLACE INTO note_state VALUES (?, ?, ?, ?, ?)",
                       (self.collection_name, note.user_id, note.note_id, note.revision, int(deleted)))

    @staticmethod
    def _where(user_id: int, note_id: int):
        return {"$and": [{"user_id": {"$eq": user_id}}, {"note_id": {"$eq": note_id}}]}

    def index(self, note: IndexRequest) -> dict:
        with self.lock:
            if self._revision(note) >= note.revision:
                return {"indexed": False, "reason": "already_processed"}
        chunks = self.splitter.split_text(note.content)
        vectors = self.embeddings.embed_documents(chunks)
        with self.lock:
            # Embedding 网络请求返回时可能已经收到更高版本，重新检查才能阻止旧内容复活。
            if self._revision(note) >= note.revision:
                return {"indexed": False, "reason": "superseded"}
            self.collection.delete(where=self._where(note.user_id, note.note_id))
            self.collection.upsert(
                ids=[f"{note.user_id}:{note.note_id}:{note.revision}:{i}" for i in range(len(chunks))],
                documents=chunks, embeddings=vectors,
                metadatas=[{"user_id": note.user_id, "note_id": note.note_id,
                            "revision": note.revision, "title": note.title} for _ in chunks],
            )
            self._record(note, False)
        return {"indexed": True, "chunks": len(chunks)}

    def delete(self, note: NoteIdentity) -> dict:
        with self.lock:
            if self._revision(note) >= note.revision:
                return {"deleted": False, "reason": "already_processed"}
            self.collection.delete(where=self._where(note.user_id, note.note_id))
            # 保留删除版本号的墓碑，晚到的旧索引请求不能重新创建被删笔记。
            self._record(note, True)
        return {"deleted": True}

    def retrieve(self, request: QueryRequest) -> list[Source]:
        allowed = {note.note_id: note.revision for note in request.notes}
        if not allowed:
            return []
        vector = self.embeddings.embed_query(request.question)
        with self.lock:
            count = self.collection.count()
            if count == 0:
                return []
            result = self.collection.query(
                query_embeddings=[vector], n_results=min(30, count),
                where={"$and": [{"user_id": {"$eq": request.user_id}},
                                {"note_id": {"$in": list(allowed)}}]},
                include=["documents", "metadatas", "distances"],
            )
        sources = []
        seen = set()
        for text, meta, distance in zip(result["documents"][0], result["metadatas"][0], result["distances"][0]):
            note_id = int(meta["note_id"])
            # 用户过滤 + 实时数据库版本白名单，双重排除跨用户、已删除和过期内容。
            if meta["user_id"] != request.user_id or allowed.get(note_id) != meta["revision"]:
                continue
            if note_id in seen or self.settings.ai_provider == "local" and distance > 0.95:
                continue
            seen.add(note_id)
            sources.append(Source(noteId=note_id, title=meta["title"], excerpt=text[:1200]))
            if len(sources) == 5:
                break
        return sources
