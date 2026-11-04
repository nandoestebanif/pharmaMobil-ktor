#!/usr/bin/env bash
# Corre dentro de android-emulator-runner, con el emulador ya encendido.
# Sesion 13 (guia practica): permisos nativos y sincronizacion refinada en el emulador.
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
filas = con.execute("SELECT id, nombre, precio, stock, activo, pendiente, intentos, ultimo_error FROM producto ORDER BY pendiente DESC, nombre").fetchall()
print('$ sqlite3 pharma.db "SELECT id, nombre, stock, pendiente, intentos, ultimo_error FROM producto"')
print(f"{'id':>4}  {'nombre':<20}{'stock':>6}{'pend.':>7}{'intentos':>10}  ultimo_error")
for f in filas:
    print(f"{f[0]:>4}  {f[1]:<20}{f[3]:>6}{f[5]:>7}{f[6]:>10}  {f[7] or ''}")
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

# Permiso de notificaciones tal como lo ve el sistema.
permiso() {
  { echo "\$ adb shell dumpsys package $APP | grep POST_NOTIFICATIONS"
    adb shell dumpsys package "$APP" | grep -m2 "POST_NOTIFICATIONS" | sed 's/^ *//'
  } > "permiso-$1.txt" 2>&1
  marca "permiso: $1"
}

# La cortina de notificaciones, para ver el aviso que mando la app.
cortina() {
  adb shell cmd statusbar expand-notifications; sleep 3
  adb exec-out screencap -p > "$1.png" || true
  adb shell cmd statusbar collapse; sleep 1
  marca "captura de la cortina: $1"
}

adb logcat -c || true
adb logcat -v time -s System.out:I > logcat-app.txt 2>/dev/null &
LOGCAT_PID=$!
marca "inicio: app instalada (instalacion limpia)"

# ---- Paso 5: los tres estados del permiso ----
correr s13-inicio 2
permiso 1-sin-pedir
correr s13-permiso-rechazar 1 -e CAPTURA_DIALOGO=b02-dialogo-del-sistema -e CAPTURA_TARJETA=b03-denegado-una-vez -e ESPERADO="Activar avisos"
permiso 2-denegado-una-vez
correr s13-permiso-rechazar 1 -e CAPTURA_DIALOGO=b04-dialogo-segunda-vez -e CAPTURA_TARJETA=b05-bloqueado -e ESPERADO="Avisos bloqueados"
permiso 3-denegado-para-siempre
correr s13-permiso-ajustes 1
# En Ajustes el usuario activa las notificaciones (aqui con pm grant, que hace lo mismo).
adb shell pm grant "$APP" android.permission.POST_NOTIFICATIONS
marca "pm grant POST_NOTIFICATIONS (como el interruptor de Ajustes)"
correr s13-permiso-concedido 1
permiso 4-concedido
cortina b08-notificacion-de-prueba

# ---- Pasos 3 y 4: pendientes, fallidas y fallo transitorio ----
adb shell am force-stop "$APP"
avion on
red 1-modo-avion
adb shell am start -n "$APP/.MainActivity" > /dev/null; sleep 6
correr s13-sin-red 1
base c1-dos-pendientes
registro c1-sin-red
avion off
red 2-reconectado
adb logcat -c || true
correr s13-sincronizar 2
registro c2-sincronizar
base c2-una-fallida
cortina c03-notificacion-envio-fallido

# Fallo transitorio: PharmaSoft se reemplaza por un servidor que responde 503 a todo.
pkill -f "spring-boot:run" 2>/dev/null || true
pkill -f "PharmaBackendApplication" 2>/dev/null || true
for i in $(seq 1 30); do curl -sf http://localhost:8080/api/health > /dev/null || break; sleep 1; done
python3 "$CI/servidor_503.py" 8080 > servidor-503.log 2>&1 &
SRV=$!
sleep 2
curl -s -o /dev/null -w "servidor de prueba: POST /api/v1/productos -> %{http_code}\n" -X POST http://localhost:8080/api/v1/productos > servidor-503.txt
marca "PharmaSoft reemplazado por un servidor que responde 503"
adb logcat -c || true
correr s13-transitorio 1
registro c4-transitorio
base c4-transitorio
kill "$SRV" 2>/dev/null || true
bash "$CI/backend.sh" 60 archivo > backend-recuperado.txt 2>&1
marca "PharmaSoft de vuelta"
adb logcat -c || true
correr s13-recuperado 2
registro c5-recuperado
base c5-recuperado
correr s13-corregir 1
registro c6-corregido
base c6-corregido

adb exec-out screencap -p > 99-android-estado-final.png || true
kill "$LOGCAT_PID" 2>/dev/null || true
grep "KtorHttp" logcat-app.txt > ktor-completo.log || true
exit 0
