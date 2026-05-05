# Build context: project root (docker build -f processing-service/Dockerfile .)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG REVISION=0.0.13
WORKDIR /build
COPY pom.xml .
COPY shared-library shared-library
COPY processing-service processing-service
RUN sed -i "s/\${revision}/${REVISION}/g" pom.xml shared-library/pom.xml processing-service/pom.xml && \
    mvn -f pom.xml install -N -DskipTests -q && \
    mvn -f shared-library/pom.xml install -DskipTests -q && \
    mvn -f processing-service/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /build/processing-service/target/processing-service-*.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
