FROM maven:3.9-eclipse-temurin-23-alpine AS builder

WORKDIR /build

COPY pom.xml ./
COPY src src

RUN mvn package -DskipTests -B

FROM eclipse-temurin:23-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8086

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
