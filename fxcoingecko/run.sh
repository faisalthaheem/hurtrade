#!/bin/bash

export REDIS_SERVER=127.0.0.1
export MQ_HOST=127.0.0.1
export MQ_USERNAME=svc
export MQ_PASSWORD=svc
export DB_HOST=127.0.0.1
export DB_USERNAME=postgres
export DB_PASSWORD=faisal123

java -jar ./target/fxcoingecko-1.0-jar-with-dependencies.jar --db-port 5400 --mq-username guest --mq-password guest