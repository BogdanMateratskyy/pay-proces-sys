# Build context: project root (docker build -f api-gateway/Dockerfile .)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
ARG REVISION=0.0.13
WORKDIR /build
COPY pom.xml .
COPY shared-library shared-library
COPY api-gateway api-gateway
RUN sed -i "s/\${revision}/${REVISION}/g" pom.xml shared-library/pom.xml api-gateway/pom.xml && \
    mvn -f pom.xml install -N -DskipTests -q && \
    mvn -f shared-library/pom.xml install -DskipTests -q && \
    mvn -f api-gateway/pom.xml package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /build/api-gateway/target/api-gateway-*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
