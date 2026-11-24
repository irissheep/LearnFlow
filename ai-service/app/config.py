from pathlib import Path
from typing import Literal
from pydantic import SecretStr, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    """配置来自环境变量；不把 API Key 写进 Python 源码。"""
    model_config = SettingsConfigDict(extra="ignore", hide_input_in_errors=True)
    ai_provider: Literal["local", "openai"] = "local"
    # Java 与 Python 的共享令牌，从 AI_SERVICE_TOKEN 环境变量读取；与模型 API Key 分开。
    ai_service_token: SecretStr
    chroma_dir: Path = Path("../work/chroma")
    openai_api_key: SecretStr | None = None
    openai_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    llm_model: str = "qwen3.8-27b"
    embedding_model: str = "text-embedding-v4"

    @model_validator(mode="after")
    def check_configuration(self):
        if len(self.ai_service_token.get_secret_value()) < 32:
            raise ValueError("AI_SERVICE_TOKEN 至少需要 32 字符，请运行 scripts/init-env.ps1")
        if self.ai_provider == "openai" and not self.openai_api_key:
            raise ValueError("openai 模式需要 OPENAI_API_KEY")
        return self
