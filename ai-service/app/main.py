from contextlib import asynccontextmanager
import hmac
import logging
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from .agent import KnowledgeAgent
from .config import Settings
from .schemas import Answer, IndexRequest, NoteIdentity, QueryRequest

log = logging.getLogger("learnflow.ai")

def create_app(settings: Settings | None = None) -> FastAPI:
    @asynccontextmanager
    async def lifespan(app):
        app.state.settings = settings or Settings()
        app.state.agent = KnowledgeAgent(app.state.settings)
        yield

    app = FastAPI(title="LearnFlow Knowledge Agent", version="0.1.0", lifespan=lifespan,
                  docs_url=None, redoc_url=None, openapi_url=None)

    def authorize(request: Request, x_internal_token: str | None = Header(default=None)):
        expected = request.app.state.settings.ai_service_token.get_secret_value()
        if not x_internal_token or not hmac.compare_digest(x_internal_token, expected):
            raise HTTPException(401, "Invalid internal service token")

    @app.get("/health")
    def health(request: Request):
        return {"status": "UP", "mode": request.app.state.settings.ai_provider}

    @app.post("/internal/index", dependencies=[Depends(authorize)])
    def index(body: IndexRequest, request: Request):
        try:
            return request.app.state.agent.store.index(body)
        except Exception as ex:
            log.error("Index operation failed: %s", type(ex).__name__)
            raise HTTPException(503, "索引失败，请检查模型配置和服务日志") from ex

    @app.post("/internal/delete", dependencies=[Depends(authorize)])
    def delete(body: NoteIdentity, request: Request):
        return request.app.state.agent.store.delete(body)

    @app.post("/internal/query", response_model=Answer, dependencies=[Depends(authorize)])
    def query(body: QueryRequest, request: Request):
        try:
            return request.app.state.agent.answer(body)
        except Exception as ex:
            log.error("Question answering failed: %s", type(ex).__name__)
            raise HTTPException(503, "回答失败，请检查模型配置和服务日志") from ex
    return app

app = create_app()
