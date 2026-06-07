#  Stage 1 : Builder
FROM eclipse-temurin:21-jdk-jammy AS builder

ARG SBT_VERSION=1.10.6

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
    | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY build.sbt ./
COPY project/build.properties project/
COPY project/plugins.sbt  project/

RUN sbt update

COPY src/ src/

RUN sbt assembly

#  Stage 2 : Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

# Limit user privileges
RUN addgroup -S appgroup && \
    adduser  -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/target/scala-3/data-stream.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-cp", "app.jar", \
    "consumer.Main"]