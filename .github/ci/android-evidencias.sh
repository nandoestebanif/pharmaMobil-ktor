#!/usr/bin/env bash
# Corre dentro de android-emulator-runner, con el emulador ya encendido.
# Sesion 10: instala la app, recorre los flujos de autenticacion y guarda el
# Logcat (lineas KtorHttp, con la cabecera Authorization saneada).
set -u
cd "$EVID"
adb shell getprop ro.build.version.release > android-version.txt
adb shell getprop ro.product.model >> android-version.txt
adb install -r "$APK" > adb-install.log 2>&1 || echo "::warning title=adb install::$(tail -3 adb-install.log | tr '\n' ' ')"
adb logcat -c || true

bash "$GITHUB_WORKSPACE/.github/ci/flujos-s10.sh" android pe.edu.upeu.pharmamobil

adb exec-out screencap -p > 09-android-estado-final.png || true
adb logcat -d > logcat-completo.txt 2>/dev/null || true
grep -E "KtorHttp" logcat-completo.txt > android-ktor.log || true
grep -E "Authorization" android-ktor.log | head -20 > android-authorization.log || true
echo "::notice title=Logcat Authorization::$(head -c 600 android-authorization.log | tr '\n' ' ')"
exit 0
