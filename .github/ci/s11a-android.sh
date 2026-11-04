#!/usr/bin/env bash
# Corre dentro de android-emulator-runner, con el emulador ya encendido.
# Actividad autonoma 11: bitacora de 5 escenarios offline en el emulador.
#   4  primera apertura sin red (antes que los demas: necesita la instalacion limpia)
#   1  apertura sin red con datos previos
#   2  registro sin red
#   3  reconexion y envio (+ Swagger)
#   -  «Actualizado hace...» con el reloj adelantado 3 h y 26 h
#   5  sesion expirada sin red (tokens de 1 minuto)
# En cada paso: hora (UTC y del emulador), estado de la red, base pharma.db copiada
# del emulador (tablas producto y sincronizacion) y peticiones KtorHttp del Logcat.
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
  local flujo="$1" intentos="${2:-1}"; shift 2 2>/dev/null || shift $#
  for intento in $(seq 1 "$intentos"); do
    if maestro test -e APP_ID="$APP" -e PLATAFORMA=android "$@" "$FLUJOS/$flujo.yaml" > "maestro-$flujo-$intento.log" 2>&1; then
      echo "::notice title=Maestro::$flujo completado"
      marca "flujo $flujo: completado"
      return 0
    fi
    echo "::warning title=Maestro::$flujo fallo (intento $intento): $(tail -4 "maestro-$flujo-$intento.log" | tr '\n' ' ' | cut -c1-700)"
    marca "flujo $flujo: FALLO (intento $intento)"
  done
  return 1
}

# Linea de la bitacora con la hora real (UTC y Lima) y la del emulador.
marca() {
  local disp
  disp=$(adb shell date "'+%Y-%m-%d %H:%M:%S %Z'" 2>/dev/null | tr -d '\r')
  printf '%s UTC | %s Lima | emulador: %s | %s\n' "$(date -u '+%F %T')" "$(TZ=America/Lima date '+%T')" "$disp" "$1" >> bitacora-tiempos.txt
}

# Copia la base del emulador (con su -wal) y lista las tablas producto y sincronizacion.
base() {
  local etiqueta="$1" dir="db-$1"
  mkdir -p "$dir"
  for f in pharma.db pharma.db-wal pharma.db-shm; do
    adb exec-out run-as "$APP" cat "databases/$f" > "$dir/$f" 2>/dev/null || rm -f "$dir/$f"
    [ -s "$dir/$f" ] || rm -f "$dir/$f"
  done
  python3 - "$dir/pharma.db" "$etiqueta" > "base-$etiqueta.txt" <<'PY'
import datetime, os, sqlite3, sys
ruta, etiqueta = sys.argv[1], sys.argv[2]
print(f"# base local del emulador: {etiqueta}")
if not os.path.exists(ruta):
    print("(la app todavia no creo pharma.db)")
    sys.exit(0)
con = sqlite3.connect(ruta)
filas = con.execute("SELECT id, nombre, precio, stock, activo, pendiente FROM producto ORDER BY pendiente DESC, nombre").fetchall()
print('$ sqlite3 pharma.db "SELECT id, nombre, precio, stock, activo, pendiente FROM producto"')
print(f"{'id':>4}  {'nombre':<24}{'precio':>7}{'stock':>7}{'activo':>8}{'pendiente':>11}")
for f in filas:
    print(f"{f[0]:>4}  {f[1]:<24}{f[2]:>7.2f}{f[3]:>7}{f[4]:>8}{f[5]:>11}")
print(f"({len(filas)} filas, {sum(1 for f in filas if f[5] == 1)} pendientes)")
print()
print('$ sqlite3 pharma.db "SELECT ultima FROM sincronizacion"')
try:
    marca = con.execute("SELECT ultima FROM sincronizacion WHERE id = 1").fetchone()
except sqlite3.Error as e:
    marca = None
    print(f"(error: {e})")
if marca is None:
    print("(sin filas: nunca sincronizado)")
else:
    utc = datetime.datetime.fromtimestamp(marca[0] / 1000, datetime.timezone.utc)
    lima = utc.astimezone(datetime.timezone(datetime.timedelta(hours=-5)))
    print(f"{marca[0]}   -> {utc:%Y-%m-%d %H:%M:%S} UTC ({lima:%H:%M:%S} Lima)")
PY
  marca "base copiada: base-$etiqueta.txt"
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

avion() {
  if [ "$1" = on ]; then
    adb shell cmd connectivity airplane-mode enable; sleep 8
  else
    adb shell cmd connectivity airplane-mode disable
    for i in $(seq 1 30); do adb shell ping -c 1 -W 1 10.0.2.2 > /dev/null 2>&1 && break; sleep 2; done
  fi
  marca "modo avion: $1"
}

# Reloj del emulador: necesita adb root (imagen google_apis) y sin hora automatica.
fijar_reloj() {
  local destino="$1" ahora
  adb shell "date \$(date -d @$destino +%m%d%H%M%Y.%S)" >> reloj.txt 2>&1
  ahora=$(adb shell date +%s | tr -d '\r')
  if [ $(( ahora - destino )) -gt 120 ] || [ $(( destino - ahora )) -gt 120 ]; then
    adb shell "date @$destino" >> reloj.txt 2>&1
    ahora=$(adb shell date +%s | tr -d '\r')
  fi
  echo "emulador: $(adb shell date | tr -d '\r') (epoch $ahora, pedido $destino)" >> reloj.txt
}

adb logcat -c || true
adb logcat -v time -s System.out:I > logcat-app.txt 2>/dev/null &
LOGCAT_PID=$!
marca "inicio: app instalada (instalacion limpia)"

# ---- Escenario 4: primera apertura sin red ----
avion on
red esc4-modo-avion
correr s11a-esc4-login-sin-red 2
base esc4-a-login-sin-red
registro esc4-login-sin-red
avion off
correr s11a-esc4-login-con-red 2
avion on
correr s11a-esc4-vacio
base esc4-b-inventario-vacio
registro esc4-inventario-vacio

# ---- Escenario 1: apertura sin red con datos previos ----
avion off
correr s11a-esc1-sincronizar 2
base esc1-a-sincronizado
registro esc1-sincronizar
adb shell am force-stop "$APP"
avion on
red esc1-modo-avion
sleep 65   # para que la barra diga «hace 1 minuto» y no «hace un momento»
correr s11a-esc1-reabrir-sin-red
base esc1-b-reabierta-sin-red
registro esc1-reabrir

# ---- Escenario 2: registro sin red ----
correr s11a-esc2-registro
base esc2-pendiente
registro esc2-registro

# ---- Escenario 3: reconexion y envio ----
avion off
red esc3-reconectado
adb logcat -c || true
correr s11a-esc3-reconectar 2
registro esc3-sincronizar
base esc3-sincronizado
/tmp/pw/bin/python "$CI/swagger_s11.py" http://localhost:8080 "$EVID/swagger-esc3" "Omeprazol 20mg" > swagger-esc3.log 2>&1 \
  || echo "::warning title=Swagger esc3::$(tail -3 swagger-esc3.log | tr '\n' ' ')"
marca "Swagger: GET del producto sincronizado"

# ---- Producto 3: «Actualizado hace...» con el reloj adelantado (en modo avion) ----
# adb root reinicia adbd: el logcat en segundo plano se corta y se vuelve a lanzar.
kill "$LOGCAT_PID" 2>/dev/null || true
adb root > reloj.txt 2>&1; sleep 3; adb wait-for-device
adb logcat -v time -s System.out:I >> logcat-app.txt 2>/dev/null &
LOGCAT_PID=$!
adb shell settings put global auto_time 0 >> reloj.txt 2>&1
BASE_RELOJ=$(adb shell date +%s | tr -d '\r')
echo "emulador antes: $(adb shell date | tr -d '\r') (epoch $BASE_RELOJ)" >> reloj.txt
avion on
fijar_reloj $(( BASE_RELOJ + 3 * 3600 + 60 ))
marca "reloj del emulador adelantado 3 h"
correr s11a-antiguedad 1 -e ESPERADO="Actualizado hace 3 horas" -e CAPTURA=a11-antiguedad-3-horas
base antiguedad-3-horas
fijar_reloj $(( BASE_RELOJ + 26 * 3600 ))
marca "reloj del emulador adelantado 26 h"
correr s11a-antiguedad 1 -e ESPERADO="Actualizado hace 1 día" -e CAPTURA=a12-antiguedad-1-dia
base antiguedad-1-dia
fijar_reloj "$(date +%s)"
adb shell settings put global auto_time 1 >> reloj.txt 2>&1
marca "reloj del emulador restaurado"
adb shell am force-stop "$APP"
avion off

# ---- Escenario 5: sesion expirada sin red ----
bash "$CI/backend.sh" 1 archivo > backend-1min.txt 2>&1
marca "PharmaSoft reiniciado con tokens de 1 minuto"
correr s11a-esc5-login 2
base esc5-a-sesion-nueva
registro esc5-login
adb shell am force-stop "$APP"
sleep 75   # el token vence
marca "token vencido (75 s)"
avion on
correr s11a-esc5-sin-red
base esc5-b-pendiente-con-token-vencido
registro esc5-sin-red
avion off
adb logcat -c || true
correr s11a-esc5-reconectar
registro esc5-reconectar-401
base esc5-c-tras-401
correr s11a-esc5-volver-a-entrar
registro esc5-volver-a-entrar
base esc5-d-reenviado
/tmp/pw/bin/python "$CI/swagger_s11.py" http://localhost:8080 "$EVID/swagger-esc5" "Ranitidina 150mg" > swagger-esc5.log 2>&1 \
  || echo "::warning title=Swagger esc5::$(tail -3 swagger-esc5.log | tr '\n' ' ')"
marca "Swagger: GET del producto enviado tras volver a entrar"

adb exec-out screencap -p > 99-android-estado-final.png || true
kill "$LOGCAT_PID" 2>/dev/null || true
grep "KtorHttp" logcat-app.txt > ktor-completo.log || true
exit 0
