#!/usr/bin/env bash
# Compila DowPaper sin Gradle: aapt2 + kotlinc + proguard + dx + zipalign + apksigner.
# Uso: ./build.sh            (requiere ANDROID_JAR y KOTLIN_LIB, ver README)
set -euo pipefail
cd "$(dirname "$0")"

ANDROID_JAR="${ANDROID_JAR:-/opt/android-sdk/platforms/android-35/android.jar}"
# aapt2 de algunas distros no lee la tabla compacta del API 35; los recursos se enlazan contra API 34.
RES_JAR="${RES_JAR:-/opt/android-sdk/platforms/android-34/android.jar}"
KOTLIN_LIB="${KOTLIN_LIB:-/opt/kotlinc}"
KVER="${KVER:-2.0.21}"
VERSION_CODE="${VERSION_CODE:-1}"
VERSION_NAME="${VERSION_NAME:-1.0.0}"
KEYSTORE="${KEYSTORE:-keystore/dowpaper-release.jks}"
KS_PASS="${KS_PASS:-dowpaper}"
KS_ALIAS="${KS_ALIAS:-dowpaper}"
SRC=app/src/main
OUT=build
APK_NAME="DowPaper-v${VERSION_NAME}.apk"

STDLIB="$KOTLIN_LIB/kotlin-stdlib-$KVER.jar"
KCP="$KOTLIN_LIB/kotlin-compiler-embeddable-$KVER.jar:$STDLIB:$KOTLIN_LIB/kotlin-script-runtime-$KVER.jar:$KOTLIN_LIB/kotlin-reflect-1.6.10.jar:$KOTLIN_LIB/kotlin-daemon-embeddable-$KVER.jar:$KOTLIN_LIB/trove4j-1.0.20200330.jar:$KOTLIN_LIB/kotlinx-coroutines-core-jvm-1.6.4.jar:$KOTLIN_LIB/annotations-13.0.jar"

rm -rf "$OUT"; mkdir -p "$OUT"/{res,gen,classes,dex}

echo "▸ [1/6] Recursos (aapt2)"
aapt2 compile --dir "$SRC/res" -o "$OUT/res/res.zip"
aapt2 link -I "$RES_JAR" --manifest "$SRC/AndroidManifest.xml" \
  --min-sdk-version 26 --target-sdk-version 35 \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  --java "$OUT/gen" -o "$OUT/base.apk" "$OUT/res/res.zip" --auto-add-overlay

echo "▸ [2/6] R.java"
javac -nowarn -source 8 -target 8 -Xlint:-options -d "$OUT/classes" $(find "$OUT/gen" -name '*.java')

echo "▸ [3/6] Kotlin"
java -Xmx2g -cp "$KCP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -no-stdlib -no-reflect -jvm-target 1.8 -Xlambdas=class -Xsam-conversions=class \
  -Xno-param-assertions -Xno-call-assertions -Xno-receiver-assertions \
  -classpath "$ANDROID_JAR:$STDLIB:$OUT/classes" -d "$OUT/classes" \
  $(find "$SRC/kotlin" -name '*.kt')

echo "▸ [4/6] ProGuard (shrink)"
(cd "$OUT/classes" && jar cf ../app-classes.jar .)
proguard -injars "$OUT/app-classes.jar" \
  -injars "$STDLIB(!META-INF/**)" \
  -outjars "$OUT/app-min.jar" -libraryjars "$ANDROID_JAR" \
  @proguard-rules.pro

echo "▸ [5/6] DEX"
dalvik-exchange --dex --min-sdk-version=26 --output="$OUT/dex/classes.dex" "$OUT/app-min.jar"

echo "▸ [6/6] Empaquetar, alinear y firmar"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q -X ../unsigned.apk classes.dex)
zipalign -p -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
if [ ! -f "$KEYSTORE" ]; then
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair -v -keystore "$KEYSTORE" -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass "$KS_PASS" -keypass "$KS_PASS" -dname "CN=DowPaper, O=DowPaper, C=CL" >/dev/null 2>&1
fi
apksigner sign --ks "$KEYSTORE" --ks-key-alias "$KS_ALIAS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --min-sdk-version 26 --out "$OUT/$APK_NAME" "$OUT/aligned.apk"
apksigner verify "$OUT/$APK_NAME"
echo "✔ APK listo: $OUT/$APK_NAME ($(du -h "$OUT/$APK_NAME" | cut -f1))"
