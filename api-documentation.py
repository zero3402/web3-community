# API Document for Web3 Community Platform (FastAPI)

# app.py: FastAPI Application Factory
# router.py: API Router and Routes
# models.py: Pydantic Models and DTOs
# database.py: Database Connection and Configuration

version: '1.0.0'
description: 'FastAPI backend for Web3 Community Platform'

type: module
scripts:
  dev: "uvicorn main:app --reload --host 0.0.0.0 --port 8000"
  start: "uvicorn main:app --host 0.0.0.0 --port 8000"
  test: "pytest -xvs"
  lint: "flake8 ."
  format: "black ."
  type-check: "mypy ."

dependencies:
  # FastAPI Core
  "fastapi": "^0.104.0"
  "uvicorn[standard]": "^0.23.0"
  "pydantic": "^2.3.0"
  "pydantic-settings": "^2.0.0"

  # Database
  "sqlalchemy": "^2.0.0"
  "asyncpg": "^0.29.0"
  "alembic": "^1.12.0"
  "databases[postgresql]": "^0.8.0"

  # Authentication & Security
  "python-jose[cryptography]": "^3.3.0"
  "passlib[bcrypt]": "^1.7.4"
  "python-multipart": "^0.0.6"

  # HTTP Client
  "httpx": "^0.25.0"
  "aiohttp": "^3.9.0"

  # Validation & Serialization
  "email-validator": "^2.0.0"
  "python-dateutil": "^2.8.2"

  # Monitoring
  "prometheus-client": "^0.17.0"

  # Utilities
  "python-dotenv": "^1.0.0"
  "pytz": "^2023.3"

devDependencies:
  # Testing
  "pytest": "^7.4.0"
  "pytest-asyncio": "^0.21.0"
  "pytest-mock": "^3.12.0"
  "httpx": "^0.25.0"

  # Code Quality
  "black": "^23.7.0"
  "flake8": "^6.0.0"
  "mypy": "^1.5.0"
  "isort": "^5.12.0"

# Production ASGI Server
# gunicorn: Used for production deployment of FastAPI apps with multiple workers
# websockets: Required for WebSocket support in FastAPI
# prometheus-client: Exposes metrics for Prometheus monitoring