FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /tmp/uploads && chown -R spring:spring /tmp/uploads

USER spring:spring
WORKDIR /app

COPY --from=build /app/target/scheduling-nails-pro-0.0.1-SNAPSHOT.jar /app/app.jar

ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:TieredStopAtLevel=1 -Xss512k -XX:MaxMetaspaceSize=160m -XX:MaxRAMPercentage=75.0"

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]