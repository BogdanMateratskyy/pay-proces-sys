# Build context: project root (docker build -f bank-adapter-service/Dockerfile .)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG REVISION=0.0.13
WORKDIR /build
COPY pom.xml .
COPY shared-library shared-library
COPY bank-adapter-service bank-adapter-service
RUN sed -i "s/\${revision}/${REVISION}/g" pom.xml shared-library/pom.xml bank-adapter-service/pom.xml && \
    mvn -f pom.xml install -N -DskipTests -q && \
    mvn -f shared-library/pom.xml install -DskipTests -q && \
    mvn -f bank-adapter-service/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /build/bank-adapter-service/target/bank-adapter-service-*.jar app.jar
USER appuser
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
