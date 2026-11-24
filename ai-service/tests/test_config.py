import pytest
from pydantic import ValidationError
from app.config import Settings


def test_internal_token_is_loaded_from_its_own_environment_variable(monkeypatch):
    token = "test-internal-token-for-learnflow-123456"
    monkeypatch.setenv("AI_SERVICE_TOKEN", token)
    monkeypatch.setenv("OPENAI_API_KEY", "test-model-api-key")
    monkeypatch.setenv("AI_PROVIDER", "local")
    settings = Settings()
    assert settings.ai_service_token.get_secret_value() == token
    assert settings.openai_api_key.get_secret_value() == "test-model-api-key"


def test_invalid_configuration_does_not_print_secret_input():
    token = "test-token-too-short"
    with pytest.raises(ValidationError) as error:
        Settings(ai_service_token=token, ai_provider="local")
    assert "AI_SERVICE_TOKEN" in str(error.value)
    assert token not in str(error.value)
