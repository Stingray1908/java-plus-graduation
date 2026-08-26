1. Убрать порты перед сдачей

docker rm -f postgres
docker rm -f stats-postgres

docker run -d --name postgres -e POSTGRES_DB=ewm_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5432:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name stats-postgres -e POSTGRES_DB=stats_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5431:5432 --restart unless-stopped postgres:15-alpine

docker rm -f postgres
docker rm -f stats-postgres
