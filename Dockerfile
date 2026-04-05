# IPTV Pro Android - Docker Build Environment
FROM eclipse-temurin:11-jdk-jammy

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Install dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Download Android Command Line Tools
RUN mkdir -p /opt/android-sdk/cmdline-tools && \
    cd /opt/android-sdk/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip -q commandlinetools-linux-*.zip && \
    rm commandlinetools-linux-*.zip && \
    mv cmdline-tools latest

# Accept licenses and install SDK
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-28" "build-tools;28.0.3" && \
    sdkmanager "platforms;android-19"

# Set working directory
WORKDIR /app

# Default command
CMD ["/bin/bash"]
