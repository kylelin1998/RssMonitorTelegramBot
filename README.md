### 简体中文 | [English](./README_en.md)

![License](https://img.shields.io/badge/license-MIT-green)
[![release](https://img.shields.io/github/v/release/kylelin1998/RssMonitorTelegramBot)](https://github.com/kylelin1998/RssMonitorTelegramBot/releases/latest)

## 简介
RSS监控最新文章， 如果有监控到最新文章会通知到您设置好的Telegram群聊， 频道， 或者个人号上

支持自定义消息通知， 由你掌控内容

支持RSS同步到Telegraph

支持自定义Webhook调用

## 具体功能
#### 通知
不仅可以通知到TG， 你还可以自定义Webhook通知到任意你想要的地方
#### 通知对象
拥有全局通知对象， 方案本身也可单独配置通知对象， 如果方案本身没有通知对象配置默认采用全局通知对象， 非常自由
#### 无限方案
你可以添加任意数量监控RSS方案， 不限制， 好管理
#### 通知模板变量
高度可配置通知文本， 支持众多通知模板变量
#### 抓取资源
你可以为你的方案配置开启抓取资源功能， 开启之后默认会抓取RSS内容的一张图片结合通知文本发送到TG
#### 翻译你想翻译的
模板变量支持翻译到你想要的， 比如你可以翻译标题成英文， 泰语， 俄语， 德语...等等
#### 好升级
机器人内部内置升级功能， 一次部署， 后续升级在机器人内点击升级即可

## 部署
机器人的部署步骤是基于 Docker 的，其机器人升级功能也基于 Docker，因此请使用 Docker 进行部署，以防出现错误

### 部署方式1 (推荐)
⭐ Youtube: https://youtu.be/mNg6TFyozZk

⭐ 哔哩哔哩： https://www.bilibili.com/video/BV1qF411f7pg/

#### 一键部署
```
docker run --name rssb -d -v $(pwd)/config:/app/config -e BOT_ADMIN_ID=管理者的ChatId -e BOT_NAME=机器人的username -e BOT_TOKEN=机器人token --log-opt max-size=10MB --log-opt max-file=5 --restart=always kylelin1998/rss-tg-bot
```
#### 一键部署(开启代理)
```
docker run --name rssb -d -v $(pwd)/config:/app/config -e BOT_ADMIN_ID=管理者的ChatId -e BOT_NAME=机器人的username -e BOT_TOKEN=机器人token -e PROXY=true -e PROXY_HOST=127.0.0.1 -e PROXY_PORT=7890 --log-opt max-size=10MB --log-opt max-file=5 --restart=always kylelin1998/rss-tg-bot
```

### 部署方式2 (不推荐)
Youtube：https://youtu.be/CiDxb1ESijQ

哔哩哔哩： https://www.bilibili.com/video/BV1Ts4y1S7bn/

首先，在您的服务器上创建一个文件夹

然后，在其中创建名为 config 的另一个文件夹，config文件夹下必须包含名为 config.json 的JSON文件

接着，将 rss-monitor-for-telegram-universal.jar, run.sh 和 Dockerfile 传输到该文件夹中

### config.json
```json
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
bot admin主要作用是设置成只有你才能触发命令
* on_proxy -> 是否开启代理
* bot_admin_id -> Bot的管理者chat id
* bot_name -> Bot 用户名
* bot_token -> Bot token
* interval_minute -> 监控间隔(分钟)
* chatIdArray -> 需要发送的Chat Id列表
* permission_chat_id_array -> 你只能允许列表下的这些chat id使用机器人， 可以填写个人的，或者是群的chat id
* translate_youdao_key -> 有道翻译应用id
* translate_youdao_secret -> 有道翻译密钥

### 第一步:
编译镜像
```
docker build -t rssb .
```

### 第二步:
运行容器镜像
```
docker run --name rssb -d -v $(pwd):/app --restart=always rssb
```

## 使用说明
**机器人命令:**
```
create - 创建计划
list - 计划列表
exit - 退出
language - 切换语言
admin - 管理命令
restart - 重启机器人
upgrade - 升级机器人
help - 帮助
```

监控部分属性说明
* webPagePreview -> 消息web预览
* notification -> 通知开关
* zeroDelay -> 零延迟监控开关， 不受间隔时间限制

template说明:
支持自定义发送通知消息文本
* ${link} -> 文章地址
* ${title} -> 文章标题
* ${author} -> 文章作者
* ${telegraph} -> Telegraph文章地址
* ${description} -> 文章内容
* ${translate|zh|title} -> 将标题翻译成中文
* ${translate|zh|description} -> 将描述翻译成中文
* ${translate|en|title} -> 将标题翻译成英文
* ${translate|en|description} -> 将描述翻译成英文
* 翻译中间的代码可以更改为自己想要翻译的...以此类推...

例子, 会自动替换对应内容:
```
${title}

Telegraph： ${telegraph}

原文： ${link}
```

![d529dad53bfee8844d66330e837912a869f427af.png](https://openimg.kylelin1998.com/img/d529dad53bfee8844d66330e837912a869f427af.png)
