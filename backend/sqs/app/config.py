from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    opensearch_host: str = "localhost"
    opensearch_port: int = 9200
    opensearch_user: str = "admin"
    opensearch_password: str = "admin"
    opensearch_use_ssl: bool = True
    opensearch_verify_certs: bool = False

    class Config:
        env_file = ".env"


settings = Settings()
