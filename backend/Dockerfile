FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
ARG RELEASE_COMMIT=development
ENV RELEASE_COMMIT=$RELEASE_COMMIT
WORKDIR /app
COPY requirements.txt .
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*
RUN pip install --no-cache-dir -r requirements.txt && useradd --uid 10001 --create-home layali
COPY . .
RUN python fetch_live_sdk.py
RUN mkdir -p staticfiles media && chown -R layali:layali /app
USER layali
EXPOSE 8000
# The named static volume may persist across image upgrades. Refresh on every start
# and fail loudly if the LiveKit SDK never reaches the files served by Caddy.
CMD ["sh", "-ec", "python manage.py collectstatic --noinput && python fetch_live_sdk.py --verify-static && exec gunicorn config.wsgi:application --bind 0.0.0.0:8000 --workers 2 --timeout 90 --access-logfile -"]
