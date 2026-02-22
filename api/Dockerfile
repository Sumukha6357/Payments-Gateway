# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS deps
WORKDIR /workspace
COPY api/pom.xml api/pom.xml
RUN mvn -f api/pom.xml -B -ntp dependency:go-offline

FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY api/pom.xml api/pom.xml
COPY --from=deps /root/.m2 /root/.m2
COPY api/src api/src
RUN --mount=type=cache,target=/root/.m2 \
  mvn -f api/pom.xml -B -ntp -DskipTests package && \
  cp api/target/payment-gateway-*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
RUN apt-get update && \
  apt-get install -y --no-install-recommends curl && \
  rm -rf /var/lib/apt/lists/* && \
  useradd -r -u 10001 -m appuser

COPY --from=builder /workspace/app.jar /app/app.jar
COPY api/entrypoint.sh /usr/local/bin/api-entrypoint.sh
RUN chmod +x /usr/local/bin/api-entrypoint.sh && chown -R appuser:appuser /app

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["api-entrypoint.sh"]
