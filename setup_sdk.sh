#!/usr/bin/env bash
# =============================================================================
# setup_sdk.sh — Aura Launcher CLI Build Environment Bootstrap
# Tested on: Termux (Android 12+), Debian 12, Ubuntu 22.04
# Usage: chmod +x setup_sdk.sh && ./setup_sdk.sh
# =============================================================================
set -euo pipefail

# ─── Config ──────────────────────────────────────────────────────────────────
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_VERSION="11076708"   # cmdline-tools 11.0 — update as needed
BUILD_TOOLS_VERSION="35.0.0"
PLATFORM_VERSION="android-35"
JAVA_VERSION="17"

CMDLINE_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

# ─── Detect OS / Package Manager ─────────────────────────────────────────────
if command -v pkg &>/dev/null; then
    PKG_MANAGER="termux"
elif command -v apt-get &>/dev/null; then
    PKG_MANAGER="apt"
else
    echo "ERROR: Unsupported environment. Install openjdk-17 and unzip manually." >&2
    exit 1
fi

install_deps() {
    echo "==> Installing system dependencies..."
    if [ "$PKG_MANAGER" = "termux" ]; then
        pkg update -y
        pkg install -y openjdk-17 wget unzip zip
    else
        sudo apt-get update -qq
        sudo apt-get install -y openjdk-"$JAVA_VERSION"-jdk wget unzip zip curl
    fi
}

download_cmdline_tools() {
    echo "==> Downloading Android cmdline-tools..."
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
    TMP_ZIP=$(mktemp /tmp/cmdtools_XXXXXX.zip)
    wget -q --show-progress "$CMDLINE_URL" -O "$TMP_ZIP"
    unzip -q "$TMP_ZIP" -d "$ANDROID_SDK_ROOT/cmdline-tools"
    mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" \
       "$ANDROID_SDK_ROOT/cmdline-tools/latest" 2>/dev/null || true
    rm "$TMP_ZIP"
}

setup_env() {
    echo "==> Configuring environment variables..."
    local PROFILE_FILE="${HOME}/.bashrc"
    [ -f "${HOME}/.zshrc" ] && PROFILE_FILE="${HOME}/.zshrc"

    cat >> "$PROFILE_FILE" <<-EOF

# ── Android SDK (added by setup_sdk.sh) ──────────────────────────────────────
export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
export ANDROID_HOME="\$ANDROID_SDK_ROOT"
export PATH="\$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:\$ANDROID_SDK_ROOT/platform-tools:\$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION:\$PATH"
EOF
    export ANDROID_SDK_ROOT
    export ANDROID_HOME="$ANDROID_SDK_ROOT"
    export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/$BUILD_TOOLS_VERSION:$PATH"
}

accept_licenses() {
    echo "==> Accepting SDK licenses..."
    yes | sdkmanager --licenses >/dev/null 2>&1 || true
}

install_sdk_components() {
    echo "==> Installing SDK components (this may take a few minutes)..."
    sdkmanager --install \
        "platform-tools" \
        "platforms;$PLATFORM_VERSION" \
        "build-tools;$BUILD_TOOLS_VERSION"
}

install_gradle_wrapper() {
    echo "==> Setting up Gradle wrapper..."
    GRADLE_VERSION="8.9"
    GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

    mkdir -p gradle/wrapper
    cat > gradle/wrapper/gradle-wrapper.properties <<-EOF
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=$GRADLE_DIST_URL
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
EOF

    wget -q --show-progress \
        "https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}/gradle/wrapper/gradle-wrapper.jar" \
        -O gradle/wrapper/gradle-wrapper.jar 2>/dev/null || {
        echo "WARN: Could not download gradle-wrapper.jar directly."
        echo "      Run: gradle wrapper --gradle-version=$GRADLE_VERSION"
    }
}

verify() {
    echo ""
    echo "==> Verifying installation..."
    java -version 2>&1 | head -1
    sdkmanager --list_installed 2>/dev/null | grep -E "build-tools|platforms|platform-tools" || true
    echo ""
    echo "✓ Setup complete. Reload your shell: source ~/.bashrc"
    echo "  Then build: ./gradlew assembleRelease"
}

# ─── Main ─────────────────────────────────────────────────────────────────────
install_deps
download_cmdline_tools
setup_env
accept_licenses
install_sdk_components
install_gradle_wrapper
verify
