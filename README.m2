


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


java -jar tester-0.0.1.jar --tester.execution.mode=COLLECTION --tester.execution.output.file-path=./report.txt


так, значит что делаем по порядку

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


_______________________________________________________________________________
ИНФО
в целом коллектор готов, принимает пишет, с него хватит
аггрегатор реализовал ИИ, проверю позже
анализатор,..........

_______________________________________________________________________________
доделать

event-service:
Поле views у мероприятия нужно заменить на rating с типом данных double.
Оно будет отражать рейтинг мероприятия,
который необходимо запрашивать у сервиса рекомендаций через gRPC-клиент сервиса Analyzer.

Необходимо добавить новые эндпоинты:
GET /events/recommendations — возвращает рекомендации мероприятий для пользователя.
Идентификатор пользователя передается в HTTP-заголовке X-EWM-USER-ID.


удалить рандомный отправитель из события
____________________________________________________________________________________






















