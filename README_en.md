### [简体中文](./README.md) | English

![License](https://img.shields.io/badge/license-MIT-green)
[![release](https://img.shields.io/github/v/release/kylelin1998/RssMonitorTelegramBot)](https://github.com/kylelin1998/RssMonitorTelegramBot/releases/latest)

## Introduction
Monitor articles for RSS

Send messages of an up-to-date article to your set-up Telegram channel,  group, or personal if you have up-to-date.

Support custom message content to your decision.

## Install & Deploy
Dockerfile and Jar file to save the same directory for building docker image.
```
docker build -t rssb .
```
You need to build logs, config directory on your personal server.

Then, Need to create a file named config.json in config directory.
```
docker run --name rssb -d -v /var/project/RssMonitorBot/logs:/logs -v /var/project/RssMonitorBot/rss-monitor-for-telegram-universal.jar:/app.jar -v /var/project/RssMonitorBot/config:/config  --restart=always rssb
```
## About
My Telegram: <https://t.me/KyleLin1998>

My Telegram Channel(Software, if have a new version, will be in this channel to notify everyone. Welcome to subscribe to it.): <https://t.me/KyleLin1998Channel>

My email: email@kylelin1998.com

## Usage
config.json for example:
```json
{
  "on_proxy": false,
  "proxy_host": "127.0.0.1",
  "proxy_port": 7890,
  "bot_admin_username": "xxxx",
  "bot_admin_id": "xxxx",
  "bot_name": "xxx",
  "bot_token": "xxx",
  "interval_minute": 10,
  "chatIdArray": ["xxxxx"]
}
```
Bot Admin mainly means only you can trigger command to manage monitor plans
* bot_admin_username -> Bot admins username
* bot_admin_id -> Bot admins chat id
* bot_name -> Bot username
* bot_token -> Bot Token
* interval_minute -> Monitor interval(Minute)
* chatIdArray -> Send to chat id list

You need to send commands in the chat interface of the bot to manage monitor plans, command for example:
* /language
* /cmd create \<monitor name>
* /cmd delete \<monitor name>
* /cmd list
* /cmd get \<monitor name>
* /cmd update \<monitor name>
* /cmd on \<monitor name>
* /cmd off \<monitor name>
* /cmd test \<monitor name>
* /cmd exit

Monitor config description
* webPagePreview -> Web page preview
* notification -> Notification switch
* zeroDelay -> Zero delays to monitor

template:
Support custom message content
* ${link} -> Article website URL
* ${title} -> Article title
* ${author} -> Article author

For example, automatically replace the variable:
```
${title}

${link}
```

![3fdb60f99c4cb66084eea0a8116b7342d96a1039.png](https://openimg.kylelin1998.com/img/3fdb60f99c4cb66084eea0a8116b7342d96a1039.png)