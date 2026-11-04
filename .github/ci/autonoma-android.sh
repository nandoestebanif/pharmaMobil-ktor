#!/usr/bin/env bash
# Corre dentro de android-emulator-runner, con el emulador ya encendido.
# Sesion 10, actividad autonoma: escenarios de token, almacenamiento seguro,
# cierre de sesion y matriz de permisos en la app real. Cada paso deja su
# captura (Maestro), el almacen cifrado (adb run-as) y el Logcat saneado.
set -u
cd "$EVID"
APP=pe.edu.upeu.pharmamobil
export PATH="$PATH:$HOME/.maestro/bin"
export MAESTRO_CLI_NO_ANALYTICS=1
FLUJOS="$GITHUB_WORKSPACE/.github/ci/maestro"
CI="$GITHUB_WORKSPACE/.github/ci"

adb shell getprop ro.build.version.release > android-version.txt
adb shell getprop ro.product.model >> android-version.txt
adb install -r "$APK" > adb-install.log 2>&1 || echo "::warning title=adb install::$(tail -3 adb-install.log | tr '\n' ' ')"

correr() {
  local flujo="$1" intentos="${2:-2}"
  for intento in $(seq 1 "$intentos"); do
    if maestro test -e APP_ID="$APP" -e PLATAFORMA=android "$FLUJOS/$flujo.yaml" > "maestro-$flujo-$intento.log" 2>&1; then
      echo "::notice title=Maestro android::$flujo completado"
      return 0
    fi
    echo "::warning title=Maestro android::$flujo fallo (intento $intento): $(tail -4 "maestro-$flujo-$intento.log" | tr '\n' ' ' | cut -c1-700)"
  done
  return 1
}

# Almacen cifrado: lista y contenido del archivo shared_prefs (run-as: apk de depuracion).
almacen() {
  local etiqueta="$1"
  {
    echo "\$ adb shell run-as $APP ls -l shared_prefs"
    adb shell "run-as $APP ls -l shared_prefs" 2>&1
    echo
    echo "\$ adb shell run-as $APP cat shared_prefs/sesion_segura.xml"
    adb shell "run-as $APP cat shared_prefs/sesion_segura.xml" 2>&1
    echo
    echo "\$ grep -c 'eyJ' sesion_segura.xml   # cabecera base64 de todo JWT (eyJhbGciOi...)"
    adb shell "run-as $APP cat shared_prefs/sesion_segura.xml" 2>/dev/null | grep -c 'eyJ'
  } > "almacen-$etiqueta.txt"
}

# Registro de la app (Logcat) desde el ultimo adb logcat -c, solo lineas KtorHttp.
registro() {
  adb logcat -d 2>/dev/null | grep "KtorHttp" > "ktor-$1.log" || true
  adb logcat -c || true
}

adb logcat -c || true

# ---- Escenario 1 + almacenamiento: login, token valido, archivo cifrado ----
correr s10a-valido
almacen 1-sesion-abierta
registro esc1-valido
correr s10a-reabrir
registro persistencia

# ---- Escenario 3: token alterado (extra de depuracion) ----
correr s10a-alterado 1
registro esc3-alterado
almacen 2-tras-token-alterado

# ---- Escenario 6: usuario deshabilitado con la sesion abierta ----
correr s10a-login-vendedor
registro esc6-previo
{
  echo "SQL> UPDATE usuarios SET estado = FALSE WHERE username = 'vendedor';"
  bash "$CI/h2.sh" "UPDATE usuarios SET estado = FALSE WHERE username = 'vendedor'"
  bash "$CI/h2.sh" "SELECT username, estado FROM usuarios ORDER BY username"
} > sql-deshabilitar.txt 2>&1
correr s10a-deshabilitado 1
registro esc6-deshabilitado
{
  echo "SQL> UPDATE usuarios SET estado = TRUE WHERE username = 'vendedor';"
  bash "$CI/h2.sh" "UPDATE usuarios SET estado = TRUE WHERE username = 'vendedor'"
  bash "$CI/h2.sh" "SELECT username, estado FROM usuarios ORDER BY username"
} > sql-reactivar.txt 2>&1

# ---- Cierre de sesion ----
correr s10a-login-admin
almacen 3-antes-de-cerrar-sesion
adb logcat -c || true
correr s10a-logout 1
almacen 4-despues-de-cerrar-sesion
registro cierre
correr s10a-otro-usuario 1
registro otro-usuario

# ---- Matriz de permisos en la interfaz ----
correr s10a-matriz

# ---- Escenario 2: token expirado (tokens de 1 minuto) ----
bash "$CI/backend.sh" 1 archivo
adb logcat -c || true
if correr s10-expira; then
  sleep 75
  correr s10-expira-2 1
fi
registro esc2-expirado
almacen 5-tras-expirar
bash "$CI/backend.sh" 60 archivo

adb exec-out screencap -p > 99-android-estado-final.png || true
exit 0
