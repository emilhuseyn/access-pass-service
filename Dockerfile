FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests \
    && java -Djarmode=layertools -jar target/access-pass-service-*.jar extract --destination target/layers

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -G app -H -s /sbin/nologin app
WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/layers/dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/layers/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/target/layers/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/layers/application/ ./

USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
