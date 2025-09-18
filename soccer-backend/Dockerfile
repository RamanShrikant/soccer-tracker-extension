# Use an official JDK image
FROM eclipse-temurin:21-jdk

# Set working directory
WORKDIR /app

# Copy Maven wrapper & pom.xml first (for caching dependencies)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy the rest of the source code
COPY src src

# Build the JAR (skip tests to save time)
RUN ./mvnw clean package -DskipTests

# Run the JAR
CMD ["java", "-jar", "target/soccer-backend-0.0.1-SNAPSHOT.jar"]
