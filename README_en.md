### [简体中文](./README.md) | English

![License](https://img.shields.io/badge/license-MIT-green)
[![release](https://img.shields.io/github/v/release/kylelin1998/RssMonitorTelegramBot)](https://github.com/kylelin1998/RssMonitorTelegramBot/releases/latest)

## Introduction
Monitor articles for RSS

Send messages of an up-to-date article to your set-up Telegram channel,  group, or personal if you have up-to-date.

Support custom message content to your decision.

Support RSS article auto sync to Telegraph platform.

## Deploy
The bot's deploy steps based on the Docker, its upgrade feature also based on the Docker, so please use the Docker to deploy it in case appear error.

### Prepare

To start, create a folder named whatever you prefer on your server.

Then create another folder named config and the config folder must contains a json file named config.json in, then transfer rss-monitor-for-telegram-universal.jar, run.sh and Dockerfile to the folder.

### config.json
```
{
  "debug": false,
  "on_proxy": false,
  "proxy_host": "127.0.0.1",
  "proxy_port": 7890,
  "bot_admin_id": "xxxx",
  "bot_name": "xxx",
  "bot_token": "xxx",
  "interval_minute": 10,
  "chatIdArray": [
    "xxxxx"
  ],
  "permission_chat_id_array": [
    "xxxx"
  ]
}
```
```
on proxy -> Whether to open proxy
bot admin id -> Bot's admin, the id is chat id of Telegram.
bot name, 和 bot token -> @BotFather has given bot name,  bot token
permission chat id array -> Allow using the bot.
```

### First step:
Build a docker image for use.
```
docker build -t rssb .
```

### Second step:
Run the docker image of just then build.
```
docker run --name rssb -d -v $(pwd):/app --restart=always rssb
```

## About
My telegram: https://t.me/KyleLin1998

My telegram channel: https://t.me/KyleLin1998Channel

My email: email@kylelin1998.com

## Usage
**Commands:**
```
create - Create plan
list - Plan list
exit - Exit
language - Change language
restart - Restart the bot
upgrade - Upgrade the bot
help - Help
```

Monitor config description
* webPagePreview -> Web page preview
* notification -> Notification switch
* zeroDelay -> Zero delays to monitor

Template content:
Support custom message content
* ${link} -> Article website URL
* ${title} -> Article title
* ${author} -> Article author
* ${telegraph} -> Telegraph URL

For example, automatically replace the variable:
```
${title}

Telegraph： ${telegraph}

Original article： ${link}
```

![3fdb60f99c4cb66084eea0a8116b7342d96a1039.png](https://openimg.kylelin1998.com/img/3fdb60f99c4cb66084eea0a8116b7342d96a1039.png)