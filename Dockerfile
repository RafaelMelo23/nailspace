
FROM alpine:3.19 as agent-downloader
RUN apk add --no-cache curl
RUN curl -L https://repo1.maven.org/maven2/io/sentry/sentry-opentelemetry-agent/8.33.0/sentry-opentelemetry-agent-8.33.0.jar -o sentry-agent.jar


FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

RUN mkdir -p /var/nailspro/uploads && chown -R spring:spring /var/nailspro/uploads

USER spring:spring

WORKDIR /app

COPY --from=agent-downloader /sentry-agent.jar /app/sentry-agent.jar
COPY target/scheduling-nails-pro-0.0.1-SNAPSHOT.jar /app/app.jar

ENV SENTRY_AUTO_INIT=false \
JAVA_TOOL_OPTIONS="-XX:+UseParallelGC -XX:MaxRAMPercentage=75.0"

EXPOSE 8080
CMD ["java", "-javaagent:sentry-agent.jar", "-jar", "app.jar"]