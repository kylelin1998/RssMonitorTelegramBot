### [简体中文](./README.md) | English

![License](https://img.shields.io/badge/license-MIT-green)
[![release](https://img.shields.io/github/v/release/kylelin1998/RssMonitorTelegramBot)](https://github.com/kylelin1998/RssMonitorTelegramBot/releases/latest)

## Introduction
Monitor articles for RSS

Send messages of an up-to-date article to your set-up Telegram channel,  group, or personal if you have up-to-date.

Support custom message content to your decision.

Support RSS article auto sync to Telegraph platform.

Support custom Webhook invocation.

## Specific Features
#### Notifications
Not only can you receive notifications on Telegram, but you can also customize webhook notifications to any platform you desire.
#### Notification Targets
You have the option to set global notification targets, and each individual plan can also have its unique notification targets. If a plan doesn't have specific notification targets configured, it will default to the global ones. This provides great flexibility.
#### Unlimited Plans
You can add an unlimited number of RSS monitoring plans, with no restrictions, for easy management.
#### Notification Template Variables
Highly configurable notification text, with support for numerous template variables.
#### Resource Crawling
You can enable resource crawling for your plans. Once enabled, it will automatically grab an image from the RSS content, combining it with the notification text and sending it to Telegram.
#### Translation of Your Choice
Template variables support translation to the language of your choice. For example, you can translate the title into English, Thai, Russian, German, etc.
#### Convenient Upgrades
The bot includes a built-in upgrade feature. After initial deployment, you can simply click on the upgrade button within the bot for future upgrades.

## Deploy
The bot's deploy steps based on the Docker, its upgrade feature also based on the Docker, so please use the Docker to deploy it in case appear error.

### Deployment method 1 (recommended)
⭐ Youtube: https://youtu.be/mNg6TFyozZk

⭐ 哔哩哔哩： https://www.bilibili.com/video/BV1qF411f7pg/

#### One-click deployment
```
docker run --name rssb -d -v $(pwd)/config:/app/config -e BOT_ADMIN_ID=AdminChatId -e BOT_NAME=BotUsername -e BOT_TOKEN=BotToken --log-opt max-size=10MB --log-opt max-file=5 --restart=always kylelin1998/rss-tg-bot
```
#### One-click deployment (with proxy enabled)
```
docker run --name rssb -d -v $(pwd)/config:/app/config -e BOT_ADMIN_ID=AdminChatId -e BOT_NAME=BotUsername -e BOT_TOKEN=BotToken -e PROXY=true -e PROXY_HOST=127.0.0.1 -e PROXY_PORT=7890 --log-opt max-size=10MB --log-opt max-file=5 --restart=always kylelin1998/rss-tg-bot
```

### Deployment method 2 (not recommended)
Youtube：https://youtu.be/CiDxb1ESijQ

哔哩哔哩： https://www.bilibili.com/video/BV1Ts4y1S7bn/

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
bot admin's main role is to set it so that only you can trigger commands.
* on_proxy -> Enable proxy or not
* bot_admin_id -> Chat ID of the Bot administrator
* bot_name -> Bot username
* bot_token -> Bot token
* interval_minute -> Monitoring interval (in minutes)
* chatIdArray -> List of Chat IDs to send notifications to
* permission_chat_id_array -> Only allow the use of the bot by the chat IDs in this list. You can fill in your personal chat ID or group chat IDs.
* translate_youdao_key -> Youdao translation application ID
* translate_youdao_secret -> Youdao translation secret key

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

## Usage
**Commands:**
```
create - Create plan
list - Plan list
exit - Exit
language - Change language
admin - Admin
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
* ${description} -> Article description
* ${translate|zh|title} -> Translate the title into Chinese
* ${translate|zh|description} -> Translate the description into Chinese
* ${translate|en|title} -> Translate the title into English
* ${translate|en|description} -> Translate the description into English
* You can modify the code in between to translate whatever you want... and so on...

For example, automatically replace the variable:
```
${title}

Telegraph： ${telegraph}

Original article： ${link}
```

![3fdb60f99c4cb66084eea0a8116b7342d96a1039.png](https://openimg.kylelin1998.com/img/3fdb60f99c4cb66084eea0a8116b7342d96a1039.png)
