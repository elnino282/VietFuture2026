FROM maven:3.9-eclipse-temurin-23-alpine AS builder

WORKDIR /build

COPY pom.xml ./
COPY src src

RUN mkdir -p /root/.m2 && printf '<?xml version="1.0" encoding="UTF-8"?><settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"><mirrors><mirror><id>aliyun</id><name>aliyun</name><url>https://maven.aliyun.com/repository/public</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml

 RUN mvn package -s /root/.m2/settings.xml -DskipTests -B

FROM eclipse-temurin:23-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8000

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
