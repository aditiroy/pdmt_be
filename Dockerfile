# Run Stage
FROM e-cpos-docker-prod-local.docker.lowes.com/certified-images/alpine/v3.21/openjdk:v17
USER root
WORKDIR /app

# The application's jar file
ARG JAR_FILE=target/permit-data-management-*.jar

# Add the application's jar to the container
#COPY --from=build ims-mrv-dynamics-comment-api.jar .
ADD ${JAR_FILE} permit-data-management.jar

# Switch to user 10001
USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/permit-data-management.jar" ]