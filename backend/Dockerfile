FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B clean package

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system algoarena \
    && useradd --system --gid algoarena --home-dir /app algoarena \
    && mkdir -p /tmp/algoarena \
    && chown algoarena:algoarena /tmp/algoarena

WORKDIR /app
COPY --from=build --chown=algoarena:algoarena /workspace/target/platform-0.0.1-SNAPSHOT.jar /app/app.jar

USER algoarena
EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.io.tmpdir=/tmp", "-jar", "/app/app.jar"]
