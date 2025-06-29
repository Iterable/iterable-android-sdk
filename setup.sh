#!/usr/bin/env bash
set -eo pipefail

# — detect shell profile —
if [ -n "$ZSH_VERSION" ]; then
  PROFILE="$HOME/.zshrc"
elif [ -n "$BASH_VERSION" ]; then
  PROFILE="$HOME/.bashrc"
else
  PROFILE="$HOME/.profile"
fi

# — 1) install Java 17 —
echo "🛠️  Installing OpenJDK 17..."
sudo apt update
sudo apt install -y openjdk-17-jdk

# — 2) install SDKMAN & Gradle 8.12 —
echo "🛠️  Installing SDKMAN & Gradle 8.12..."
curl -s "https://get.sdkman.io" | bash
# load SDKMAN in this session
set +e
source "$HOME/.sdkman/bin/sdkman-init.sh"
set -e
sdk install gradle 8.12

# — 3) install Android cmdline-tools (sdkmanager 19.x) —
echo "🛠️  Installing Android cmdline-tools..."
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

# — 4) install platform-tools 36.0.0 (adb) —
echo "🛠️  Installing platform-tools 36.0.0..."
export ANDROID_SDK_ROOT
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
yes | sdkmanager "platform-tools;36.0.0"

# — 5) append env to dotfile if missing —
echo "🛠️  Updating $PROFILE..."
ENV_BLOCK=$(cat << 'EOF'
# — Android SDK —
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
EOF
)
grep -qxF 'export ANDROID_SDK_ROOT=/opt/android-sdk' "$PROFILE" || \
  echo "$ENV_BLOCK" >> "$PROFILE"

# — 6) verify installs —
echo "🔍 Verifying..."
java -version    # expect 17.0.15+
gradle --version # expect 8.12
sdkmanager --version # expect 19.x
adb version      # expect 1.0.41 (36.0.0)

echo "✅ All set, bro! Reload your shell or run: source $PROFILE"
