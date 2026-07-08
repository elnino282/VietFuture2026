# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-23-alpine AS builder

WORKDIR /build

# settings.xml mirrors every repo to https://repo.maven.apache.org/maven2
# (no aliyun, no third-party proxy). Maven Central direct is the single
# source of truth so the build no longer depends on a flaky mirror.
COPY settings.xml /root/.m2/settings.xml

# Cache dependencies in a BuildKit cache mount. The cache is keyed by the
# image name so different services share their pom.xml layer but keep their
# own /root/.m2/repository, so the second service to build only has to fetch
# the deps the first one didn't already have.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -s /root/.m2/settings.xml -T 1C -Dmaven.artifact.threads=12 \
        dependency:resolve -DskipTests || true

COPY src src

# Build the fat jar. Mounted cache layer means even on a fresh Docker daemon
# only the missing artifacts are pulled.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -s /root/.m2/settings.xml -T 1C -Dmaven.artifact.threads=12 \
        package -DskipTests

FROM eclipse-temurin:23-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8092

USER appuser

ENTRYPOINT ["java", "-jar", "app.jar"]
