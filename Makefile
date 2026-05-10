# =============================================================================
# Makefile — Aura Launcher
# Usage: make <target>
# Requires: openjdk-17, Android SDK (run ./setup_sdk.sh first)
# =============================================================================

GRADLEW     := ./gradlew
APK_RELEASE := app/build/outputs/apk/release/app-release.apk
APK_DEBUG   := app/build/outputs/apk/debug/app-debug.apk
DEVICE_DEST := /data/local/tmp/aura.apk
SERIAL      ?=   # Optional: SERIAL=emulator-5554 make install

ADB         := adb$(if $(SERIAL), -s $(SERIAL),)

.PHONY: all build build-debug install install-debug clean lint check \
        sign generate-keystore push log

# ─── Default ─────────────────────────────────────────────────────────────────
all: build

# ─── Build ───────────────────────────────────────────────────────────────────
build:
	@echo "==> Building release APK..."
	$(GRADLEW) assembleRelease --stacktrace
	@echo ""
	@echo "✓ APK: $(APK_RELEASE)"
	@du -sh $(APK_RELEASE)

build-debug:
	@echo "==> Building debug APK..."
	$(GRADLEW) assembleDebug
	@echo "✓ APK: $(APK_DEBUG)"

# ─── Install ─────────────────────────────────────────────────────────────────
install: build
	@echo "==> Pushing to device..."
	$(ADB) push $(APK_RELEASE) $(DEVICE_DEST)
	$(ADB) shell pm install -r --bypass-low-target-sdk-block $(DEVICE_DEST)
	$(ADB) shell rm $(DEVICE_DEST)
	@echo "✓ Installed on device."

install-debug: build-debug
	$(ADB) install -r $(APK_DEBUG)

# ─── Signing ─────────────────────────────────────────────────────────────────
generate-keystore:
	@echo "==> Generating release keystore (aura.jks)..."
	keytool -genkey -v \
	    -keystore app/aura.jks \
	    -alias aura \
	    -keyalg RSA \
	    -keysize 2048 \
	    -validity 10000
	@echo "==> Writing keystore.properties..."
	@echo "storeFile=aura.jks"        >  keystore.properties
	@echo "keyAlias=aura"             >> keystore.properties
	@read -sp "Store password: " SP; echo "storePassword=$$SP" >> keystore.properties
	@read -sp "Key password:   " KP; echo "keyPassword=$$KP"   >> keystore.properties
	@echo "✓ keystore.properties written. Do not commit this file."

sign: build
	@echo "==> APK is signed as part of assembleRelease via signingConfig."
	@echo "    To verify: apksigner verify --verbose $(APK_RELEASE)"
	apksigner verify --verbose $(APK_RELEASE)

# ─── Quality ─────────────────────────────────────────────────────────────────
lint:
	$(GRADLEW) lint
	@echo "Report: app/build/reports/lint-results-release.html"

check:
	$(GRADLEW) check

# ─── Baseline Profile ────────────────────────────────────────────────────────
baseline-profile:
	@echo "==> Generating Baseline Profile (requires connected device)..."
	$(GRADLEW) generateBaselineProfile
	@echo "✓ Profile written to app/src/main/baseline-prof.txt"

# ─── Utilities ───────────────────────────────────────────────────────────────
clean:
	$(GRADLEW) clean
	@echo "✓ Build outputs removed."

push: build
	$(ADB) push $(APK_RELEASE) $(DEVICE_DEST)
	@echo "✓ APK pushed to $(DEVICE_DEST). Install with: adb shell pm install -r $(DEVICE_DEST)"

log:
	$(ADB) logcat -s AuraLauncher:V AndroidRuntime:E

size: build
	@echo "==> APK size breakdown:"
	@du -sh $(APK_RELEASE)
	@$(ANDROID_HOME)/build-tools/$$(ls $(ANDROID_HOME)/build-tools/ | tail -1)/apkanalyzer \
	    apk summary $(APK_RELEASE) 2>/dev/null || echo "(apkanalyzer not found — check ANDROID_HOME)"
