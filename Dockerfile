FROM openjdk:8-jdk-alpine
# 系统编码
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

# 增加字体，解决验证码没有字体报空指针问题
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories && \
    apk --no-cache add ttf-dejavu fontconfig tzdata

RUN /bin/cp -f /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
WORKDIR /app
COPY target/rss-monitor-for-telegram-universal.jar ./app.jar

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar","app.jar"]

