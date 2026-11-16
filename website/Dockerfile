FROM python:3.13-alpine
WORKDIR /site
COPY index.html server.py /site/
COPY assets /site/assets
RUN addgroup -S app && adduser -S -G app app && mkdir /data && chown app:app /data
USER app
ENV RELAXIA_SITE_DB=/data/site.sqlite3 PORT=8080 PYTHONDONTWRITEBYTECODE=1
EXPOSE 8080
CMD ["python","-B","server.py"]
