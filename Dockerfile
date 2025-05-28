FROM gradle:8.14.0-jdk21

# Create app directory
WORKDIR /app

COPY . /app

RUN ./gradlew build

ENTRYPOINT ["java", "-jar", "build/libs/kitchen-application-0.0.1-SNAPSHOT.jar"]