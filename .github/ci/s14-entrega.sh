#!/usr/bin/env bash
# Corre dentro de android-emulator-runner, con el emulador ya encendido.
# Sesion 14, paso 5: la VERSION DE ENTREGA (minificada con R8) recorre el flujo completo.
# Si R8 rompio la serializacion o Koin, el fallo aparece al pedir el primer dato.
# Una compilacion de entrega no es depurable: no hay run-as ni registro HTTP, asi que la
# evidencia son las capturas, el resultado de cada flujo y el buffer de cierres (crash).
set -u
cd "$EVID"
APP=pe.edu.upeu.pharmamobil
export PATH="$PATH:$HOME/.maestro/bin"
export MAESTRO_CLI_NO_ANALYTICS=1
FLUJOS="$GITHUB_WORKSPACE/.github/ci/maestro"
CI="$GITHUB_WORKSPACE/.github/ci"

adb shell getprop ro.build.version.release > android-version.txt
adb shell getprop ro.product.model >> android-version.txt
adb install -r "$APK_ENTREGA" > adb-install.log 2>&1 || echo "::warning title=adb install::$(tail -3 adb-install.log | tr '\n' ' ')"
{
  echo "\$ adb shell dumpsys package $APP | grep -E 'versionCode|versionName|flags='"
  adb shell dumpsys package "$APP" | grep -E "versionCode|versionName|pkgFlags" | head -4
  echo "\$ adb shell run-as $APP ls   # una version de entrega no es depurable"
  adb shell "run-as $APP ls" 2>&1 | head -2
} > paquete-instalado.txt 2>&1

# Mismo arreglo que en la autonoma 13: el dialogo de ANR del launcher tapa la app.
adb shell settings put global hide_error_dialogs 1 || true
sin_dialogos() { adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS > /dev/null 2>&1 || true; }
sleep 20; sin_dialogos

marca() {
  printf '%s UTC | %s Lima | %s\n' "$(date -u '+%F %T')" "$(TZ=America/Lima date '+%T')" "$1" >> bitacora-tiempos.txt
}
correr() {
  local flujo="$1" intentos="${2:-1}"
  for intento in $(seq 1 "$intentos"); do
    sin_dialogos
    if maestro test -e APP_ID="$APP" -e PLATAFORMA=android "$FLUJOS/$flujo.yaml" > "maestro-$flujo-$intento.log" 2>&1; then
      echo "::notice title=Maestro (entrega)::$flujo completado"; marca "flujo $flujo: completado"; return 0
    fi
    echo "::warning title=Maestro (entrega)::$flujo fallo (intento $intento): $(tail -4 "maestro-$flujo-$intento.log" | tr '\n' ' ' | cut -c1-600)"
    marca "flujo $flujo: FALLO (intento $intento)"
  done
  return 1
}
avion() {
  if [ "$1" = on ]; then adb shell cmd connectivity airplane-mode enable; sleep 8
  else adb shell cmd connectivity airplane-mode disable
    for i in $(seq 1 30); do adb shell ping -c 1 -W 1 10.0.2.2 > /dev/null 2>&1 && break; sleep 2; done
  fi
  marca "modo avion: $1"
}

adb logcat -c || true
adb logcat -b crash -v time > cierres.txt 2>/dev/null &
CRASH_PID=$!
adb logcat -v time AndroidRuntime:E '*:S' > androidruntime.txt 2>/dev/null &
RT_PID=$!
marca "inicio: version de entrega instalada"

correr s13a-p01-primera-apertura 2
correr s13a-p02-login 2
correr s13a-p03-listado 2
correr s13a-p04-crear-con-red 1
avion on
correr s13a-p05-crear-sin-red 1
avion off
correr s13a-p06-sincronizar 2
correr s13a-p07-validacion 1
# El paso 8 usa un extra que solo existe en depuracion («acciones=todas»): no aplica aqui.
bash "$CI/backend.sh" 1 archivo > backend-1min.txt 2>&1
marca "PharmaSoft reiniciado con tokens de 1 minuto"
correr s13a-p09-token-vencido-1 1
sleep 75; marca "token vencido (75 s)"
correr s13a-p09-token-vencido-2 1
bash "$CI/backend.sh" 60 archivo > backend-60min.txt 2>&1
correr s13a-p10-cerrar-sesion 1

sleep 2
kill "$CRASH_PID" "$RT_PID" 2>/dev/null || true
if [ -s cierres.txt ] && grep -q "FATAL EXCEPTION" cierres.txt; then
  echo "::error title=Cierres en la version de entrega::$(grep -m1 -A3 'FATAL EXCEPTION' cierres.txt | tr '\n' ' ' | cut -c1-600)"
else
  echo "(sin cierres de la app durante el recorrido)" >> cierres.txt
fi
exit 0
