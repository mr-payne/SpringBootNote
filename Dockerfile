# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Build the application
#
# This stage uses a full JDK image to build the application with Gradle.
# It leverages Docker's layer caching to speed up subsequent builds.
FROM eclipse-temurin:21-jdk-jammy as build

WORKDIR /app

# Copy the Gradle wrapper files
COPY gradlew gradlew
COPY gradle gradle/

# Copy the build configuration files.
# By copying these first, Docker will cache the dependency resolution step
# unless these files change.
# ---
# CORRECTED LINE: Now copies the .kts files for Kotlin DSL
COPY build.gradle.kts settings.gradle.kts ./
# ---

# Resolve and download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy the rest of the source code
COPY src src/

# Build the application, creating the executable JAR
RUN ./gradlew build --no-daemon -x test

################################################################################
# Stage 2: Create the final, minimal runtime image
#
# This stage uses a lightweight JRE image and copies only the built JAR
# from the 'build' stage, resulting in a smaller and more secure final image.
FROM eclipse-temurin:21-jre-jammy AS final

WORKDIR /app

# Create a non-privileged user that the app will run under.
# See https://docs.docker.com/go/dockerfile-user-best-practices/
ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
USER appuser

# Copy the executable JAR from the "build" stage.
# The JAR is typically found in build/libs/
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8050

ENTRYPOINT [ "java", "-jar", "app.jar" ]