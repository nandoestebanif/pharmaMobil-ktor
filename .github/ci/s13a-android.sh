#!/usr/bin/env bash
# Corre dentro de android-emulator-runner, con el emulador ya encendido.
# Actividad autonoma 13: mapa del flujo completo en 10 pasos, con el estado del sistema en cada uno.
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
print('$ sqlite3 pharma.db "SELECT usuario, nombre_completo, roles, permisos FROM perfil"')
try:
    for f in con.execute("SELECT usuario, nombre_completo, roles, permisos FROM perfil"):
        print(f"{f[0]} | {f[1]} | {f[2]} | {f[3]}")
    if con.execute("SELECT COUNT(*) FROM perfil").fetchone()[0] == 0:
        print("(sin filas)")
except sqlite3.Error as e:
    print(f"(error: {e})")
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

# Almacen cifrado del token (EncryptedSharedPreferences) tal como queda en el disco.
almacen() {
  {
    echo "\$ adb shell run-as $APP ls shared_prefs"
    adb shell "run-as $APP ls shared_prefs" 2>&1
    echo "\$ adb shell run-as $APP cat shared_prefs/sesion_segura.xml"
    adb shell "run-as $APP cat shared_prefs/sesion_segura.xml" 2>&1 | sed -E 's/(">)([^<]{36})[^<]{8,}(<\/string>)/\1\2...(recortado)\3/'
    echo "\$ grep -c 'eyJ' sesion_segura.xml   # cabecera de todo JWT en texto plano"
    adb shell "run-as $APP cat shared_prefs/sesion_segura.xml" 2>/dev/null | grep -c 'eyJ'
  } > "almacen-$1.txt" 2>&1
  marca "almacen: $1"
}

estado() { almacen "$1"; base "$1"; }

adb logcat -c || true
adb logcat -v time -s System.out:I > logcat-app.txt 2>/dev/null &
LOGCAT_PID=$!
marca "inicio: app instalada (instalacion limpia)"

correr s13a-p01-primera-apertura 2; estado p01; registro p01
correr s13a-p02-login 2;            estado p02; registro p02
correr s13a-p03-listado 2;          estado p03; registro p03
correr s13a-p04-crear-con-red 1;    estado p04; registro p04
avion on; red p05
correr s13a-p05-crear-sin-red 1;    estado p05; registro p05
avion off; red p06
adb logcat -c || true
correr s13a-p06-sincronizar 2;      estado p06; registro p06
correr s13a-p07-validacion 1;       estado p07; registro p07
correr s13a-p08-sin-privilegio 1;   estado p08; registro p08
bash "$CI/backend.sh" 1 archivo > backend-1min.txt 2>&1
marca "PharmaSoft reiniciado con tokens de 1 minuto"
correr s13a-p09-token-vencido-1 1
sleep 75; marca "token vencido (75 s)"
adb logcat -c || true
correr s13a-p09-token-vencido-2 1;  estado p09; registro p09
bash "$CI/backend.sh" 60 archivo > backend-60min.txt 2>&1
marca "PharmaSoft con tokens de 60 minutos"
correr s13a-p10-cerrar-sesion 1;    estado p10; registro p10

# Producto B: que pasa con el token al reinstalar (Android borra los datos de la app).
correr s13a-p02-login 1 > /dev/null 2>&1 || true
almacen r1-antes-de-reinstalar
adb uninstall "$APP" > reinstalar.txt 2>&1
adb install "$APK" >> reinstalar.txt 2>&1
almacen r2-despues-de-reinstalar
adb shell am start -n "$APP/.MainActivity" > /dev/null; sleep 8
adb exec-out screencap -p > f11-reinstalada-pide-login.png || true

kill "$LOGCAT_PID" 2>/dev/null || true
grep "KtorHttp" logcat-app.txt > ktor-completo.log || true
exit 0
