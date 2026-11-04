#!/usr/bin/env bash
# Corre dentro de android-emulator-runner, con el emulador ya encendido.
# Sesion 11, paso 6 de la guia en este orden exacto: con red -> modo avion ->
# listado sin red -> registro pendiente -> sin modo avion + Sincronizar -> Swagger.
# En cada paso copia la base pharma.db del emulador (adb run-as) para mostrar la
# marca pendiente, y guarda el Logcat de la sincronizacion (orden POST -> GET).
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

# Copia la base del emulador (con su -wal) y lista la tabla producto.
base() {
  local etiqueta="$1" dir="db-$1"
  mkdir -p "$dir"
  for f in pharma.db pharma.db-wal pharma.db-shm; do
    adb exec-out run-as "$APP" cat "databases/$f" > "$dir/$f" 2>/dev/null || rm -f "$dir/$f"
    [ -s "$dir/$f" ] || rm -f "$dir/$f"
  done
  python3 - "$dir/pharma.db" "$etiqueta" > "base-$etiqueta.txt" <<'PY'
import sqlite3, sys
ruta, etiqueta = sys.argv[1], sys.argv[2]
con = sqlite3.connect(ruta)
filas = con.execute("SELECT id, nombre, precio, stock, activo, pendiente FROM producto ORDER BY pendiente DESC, nombre").fetchall()
print(f"$ sqlite3 pharma.db \"SELECT id, nombre, precio, stock, activo, pendiente FROM producto ...\"   # {etiqueta}")
print(f"{'id':>4}  {'nombre':<24}{'precio':>7}{'stock':>7}{'activo':>8}{'pendiente':>11}")
for f in filas:
    print(f"{f[0]:>4}  {f[1]:<24}{f[2]:>7.2f}{f[3]:>7}{f[4]:>8}{f[5]:>11}")
print(f"({len(filas)} filas, {sum(1 for f in filas if f[5] == 1)} pendientes)")
PY
}

registro() {
  adb logcat -d 2>/dev/null | grep "KtorHttp" > "ktor-$1.log" || true
  adb logcat -c || true
}

red() {
  {
    echo "\$ adb shell settings get global airplane_mode_on"
    adb shell settings get global airplane_mode_on
    echo "\$ adb shell ping -c 1 -W 2 10.0.2.2   # PC (PharmaSoft) vista desde el emulador"
    adb shell ping -c 1 -W 2 10.0.2.2 2>&1 | tail -2
  } > "red-$1.txt" 2>&1
}

adb logcat -c || true
# Registro completo de la app durante toda la corrida (KtorHttp y Keychain/almacen).
adb logcat -v time -s System.out:I > logcat-app.txt 2>/dev/null &
LOGCAT_PID=$!

# 1. Con red: abrir, iniciar sesion y dejar que sincronice. Luego se cierra la app.
correr s11-con-red
base 1-con-red
registro 1-con-red
adb shell am force-stop "$APP"

# 2. Modo avion y la app otra vez: lista sin red, registro de un producto (pendiente).
adb shell cmd connectivity airplane-mode enable
sleep 8
red 2-modo-avion
correr s11-sin-red 1
base 2-sin-red-pendiente
registro 2-sin-red

# 3. Sin modo avion y Sincronizar: sube el pendiente y baja la lista.
adb shell cmd connectivity airplane-mode disable
for i in $(seq 1 30); do adb shell ping -c 1 -W 1 10.0.2.2 > /dev/null 2>&1 && break; sleep 2; done
red 3-reconectado
adb logcat -c || true
correr s11-reconectar
registro 3-sincronizar
base 3-sincronizado

# 4. Swagger: el producto existe en PharmaSoft con su id real.
/tmp/pw/bin/python "$CI/swagger_s11.py" http://localhost:8080 "$EVID" "Omeprazol 20mg" > swagger-s11.log 2>&1 \
  || echo "::warning title=Swagger S11::$(tail -3 swagger-s11.log | tr '\n' ' ')"

# 5. Con red pero PharmaSoft apagado: la app abre y lista igual.
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "PharmaBackendApplication" 2>/dev/null || true
sleep 5
curl -s -o /dev/null -w "PharmaSoft apagado: GET /api/health -> %{http_code}\n" http://localhost:8080/api/health > backend-apagado.txt 2>&1
adb logcat -c || true
correr s11-backend-apagado
registro 4-backend-apagado

adb exec-out screencap -p > 99-android-estado-final.png || true
kill "$LOGCAT_PID" 2>/dev/null || true
grep "KtorHttp" logcat-app.txt > ktor-completo.log || true
exit 0
