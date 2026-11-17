# Référence à intégrer au lot 02, après construction réelle du JAR au lot 01.
# --build-arg JAVA_RUNTIME_IMAGE=<digest résolu par lock-images.py>
ARG JAVA_RUNTIME_IMAGE
FROM ${JAVA_RUNTIME_IMAGE}
WORKDIR /app
COPY --chown=10001:10001 target/maintenance-api.jar /app/application.jar
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
