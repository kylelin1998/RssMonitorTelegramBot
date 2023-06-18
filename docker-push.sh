#!/bin/sh

docker build -t kylelin1998/rss-tg-bot .
docker tag kylelin1998/rss-tg-bot kylelin1998/rss-tg-bot:latest
docker push kylelin1998/rss-tg-bot:latest