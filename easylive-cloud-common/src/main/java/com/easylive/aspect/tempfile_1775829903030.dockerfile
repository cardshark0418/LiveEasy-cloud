NEW_FILE_CODE
FROM openjdk:8-jdk-alpine

LABEL maintainer="easylive"
LABEL description="EasyLive Cloud Gateway Service"

WORKDIR /app

COPY target/easylive-cloud-gateway-1.0.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV SERVER_PORT=8080
ENV NACOS_SERVER_ADDR=127.0.0.1:8848

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar --spring.cloud.nacos.discovery.server-addr=$NACOS_SERVER_ADDR --spring.cloud.nacos.config.server-addr=$NACOS_SERVER_ADDR --server.port=$SERVER_PORT"]
