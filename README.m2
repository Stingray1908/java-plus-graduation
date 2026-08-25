1. Убрать порты перед сдачей

docker run -d --name postgres -e POSTGRES_DB=ewm_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5432:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name stats-postgres -e POSTGRES_DB=stats_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5433:5432 --restart unless-stopped postgres:15-alpine

