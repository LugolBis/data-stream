# Stage 1 : Builder
FROM eclipse-temurin:21-jdk-jammy AS builder

ARG SBT_VERSION=1.10.6

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v${SBT_VERSION}/sbt-${SBT_VERSION}.tgz" \
    | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY build.sbt            ./
COPY project/build.properties project/
COPY project/plugins.sbt  project/
RUN sbt update

COPY src/ src/

RUN sbt assembly && \
    find target -name "*.jar" \
    ! -name "*javadoc*" \
    ! -name "*sources*" \
    ! -name "*original*" \
    -ls

RUN find /build/target -name "data-stream.jar" -exec cp {} /build/app.jar \; && \
    test -f /build/app.jar || \
    (echo "ERROR: data-stream.jar doesn't exist" && exit 1)


# Stage 2 : Runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/app.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-cp", "app.jar"]

CMD ["consumer.Main"]