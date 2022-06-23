# NFT-shop
## Запуск

1. `sbt stage` - the result is available under target/universal/stage
2. `sbt docker:stage` - generate a Dockerfile
3. `sbt docker:publishLocal` - the result is a docker image named with the same name as the project and with the same version as the project
4. `sbt docker:publishLocal` - verify that the base image has changed into Dockerfile
5. `docker-compose up` - run docker-compose file
