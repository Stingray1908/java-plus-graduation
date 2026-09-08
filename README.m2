


docker rm -f mainServ
docker rm -f statsServ
docker rm -f commentServ
docker rm -f requestServ
docker rm -f eventServ
docker rm -f userServ

docker run -d --name mainServ -e POSTGRES_DB=ewm_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5430:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name statsServ -e POSTGRES_DB=stats_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5431:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name commentServ -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -p 5432:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name requestServ -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -p 5433:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name eventServ -e POSTGRES_DB=event_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5434:5432 --restart unless-stopped postgres:15-alpine
docker run -d --name userServ -e POSTGRES_DB=user_db -e POSTGRES_USER=dbuser -e POSTGRES_PASSWORD=12345 -p 5435:5432 --restart unless-stopped postgres:15-alpine


так, значит что делаем по порядку

- добавляем модули коллектор агрегатор анализатор сериализация
- начинаем с схемы сериализации, прото авро
- затем коллектор сборщик проверяем что работает, принимает 3 вида событий

- агрегатор уже реализует логику рекомендаций, все в памяти, только в памяти

- анализатор, тут уже БД
- хранит взаимодействие пользователя с мероприятием




- в событиии теперь не просмотры, а рейтинг Double
- реализовать обработку просмотр, которая раньше была для статистики, теперь дате коэфф 0,4
- при регистрация 0,8
- добавить, этого нет лайк 1

- высчитывать рекомендации


- удалить все модули статистики кроме клиента



Запустил ДИСКАВЕРИ конфиг, ворота, БАЗЫ, стат, евент, коллектор, AGGREGATOR

шлю
