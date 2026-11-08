FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && useradd --uid 10001 --create-home layali
COPY . .
RUN python fetch_live_sdk.py
RUN mkdir -p staticfiles media && chown -R layali:layali /app
USER layali
EXPOSE 8000
# The named static volume may persist across image upgrades. Refresh on every start
# and fail loudly if the LiveKit SDK never reaches the files served by Caddy.
CMD ["sh", "-ec", "python manage.py collectstatic --noinput && python fetch_live_sdk.py --verify-static && exec gunicorn config.wsgi:application --bind 0.0.0.0:8000 --workers 2 --timeout 90 --access-logfile -"]
