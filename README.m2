


docker rm -f mainServ
docker rm -f statsServ
docker rm -f commentServ
docker rm -f requestServ
docker rm -f eventServ
docker rm -f userServ

docker run -d --name mainServ -e POSTGRES_DB=ewm_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5430:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name statsServ -e POSTGRES_DB=stats_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5431:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name commentServ -e POSTGRES_DB=comment_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5432:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name requestServ -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -p 5433:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name eventServ -e POSTGRES_DB=event_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5434:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name userServ -e POSTGRES_DB=user_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5435:5432 --restart unless-stopped postgres:15-alpine
