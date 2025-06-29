#!/usr/bin/env bash
set -eo pipefail

# 0) don't run as root
if [ "$EUID" -eq 0 ]; then
  echo "⚠️  Run as your normal user (no sudo)." >&2
  exit 1
fi

# detect shell profile
if [ -n "$ZSH_VERSION" ]; then
  PROFILE="$HOME/.zshrc"
elif [ -n "$BASH_VERSION" ]; then
  PROFILE="$HOME/.bashrc"
else
  PROFILE="$HOME/.profile"
fi

echo "🛠️  1) Installing OpenJDK 17..."
sudo apt update
sudo apt install -y openjdk-17-jdk

echo "🛠️  2) Installing SDKMAN & Gradle 8.12..."
curl -s "https://get.sdkman.io" | bash
# load SDKMAN
set +e
source "$HOME/.sdkman/bin/sdkman-init.sh"
set -e
sdk install gradle 8.12

echo "🛠️  3) Installing Android cmdline-tools..."
ANDROID_SDK_ROOT=/opt/android-sdk
sudo mkdir -p "$ANDROID_SDK_ROOT"
sudo chown "$USER":"$USER" "$ANDROID_SDK_ROOT"

WORKDIR=$(mktemp -d)
pushd "$WORKDIR" >/dev/null
wget -q https://dl.google.com/android/repository/commandlinetools-linux-9123335_latest.zip -O cmdline-tools.zip
unzip -q cmdline-tools.zip
rm cmdline-tools.zip
sudo mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools/latest"
sudo cp -r cmdline-tools/* "$ANDROID_SDK_ROOT/cmdline-tools/latest/"
popd >/dev/null
rm -rf "$WORKDIR"

echo "🛠️  4) Installing platform-tools + accepting licenses..."
export ANDROID_SDK_ROOT
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"

# Install platform-tools
yes | sdkmanager "platform-tools"

# Auto-accept all licenses
yes | sdkmanager --licenses > /dev/null

echo "🛠️  5) Updating $PROFILE..."
ENV_BLOCK=$(cat << 'EOF'
# — Android SDK —
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
EOF
)
grep -qxF 'export ANDROID_SDK_ROOT=/opt/android-sdk' "$PROFILE" || \
  echo "$ENV_BLOCK" >> "$PROFILE"

echo "🔍 6) Verifying…"
java -version    # expect 17.0.15+
gradle --version # expect 8.12
sdkmanager --version # expect 19.x
adb version      # expect 1.0.41+

echo "✅ Done, bro! Reload shell or run: source $PROFILE"
