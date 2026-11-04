#!/usr/bin/env bash
# Sesion 11 en el simulador de iPhone. iOS no tiene modo avion en el simulador: la
# falta de red se provoca APAGANDO PharmaSoft (la app no llega al servidor).
# Uso: s11-ios.sh BUNDLE_ID UDID
set -u
BUNDLE="$1"
UDID="$2"
cd "$EVID"
export PATH="$PATH:$HOME/.maestro/bin"
export MAESTRO_CLI_NO_ANALYTICS=1
FLUJOS="$GITHUB_WORKSPACE/.github/ci/maestro"
CI="$GITHUB_WORKSPACE/.github/ci"

correr() {
  local flujo="$1" intentos="${2:-2}"
  for intento in $(seq 1 "$intentos"); do
    if maestro --device "$UDID" test -e APP_ID="$BUNDLE" -e PLATAFORMA=ios "$FLUJOS/$flujo.yaml" > "maestro-$flujo-$intento.log" 2>&1; then
      echo "::notice title=Maestro ios::$flujo completado"
      return 0
    fi
    echo "::warning title=Maestro ios::$flujo fallo (intento $intento): $(tail -4 "maestro-$flujo-$intento.log" | tr '\n' ' ' | cut -c1-700)"
  done
  return 1
}

apagar() {
  pkill -f "spring-boot:run" 2>/dev/null || true
  pkill -f "PharmaBackendApplication" 2>/dev/null || true
  sleep 5
  curl -s -o /dev/null -w "GET /api/health -> %{http_code}\n" http://localhost:8080/api/health >> backend-estado.txt 2>&1
}

# Registro unificado del simulador: en iOS la app escribe el registro KtorHttp con NSLog.
xcrun simctl spawn "$UDID" log stream --style compact --level debug \
  --predicate 'processImagePath CONTAINS "PharmaMobil"' > ios-registro.log 2>&1 &
LOG_PID=$!

# 1. Con red (el flujo hace clearState: base vacia).
correr s11-con-red
cp "$EVID/backend-token-60min.log" "$EVID/backend-1-con-red.log" 2>/dev/null || true
xcrun simctl terminate "$UDID" "$BUNDLE" || true

# 2. PharmaSoft apagado: la app abre sin servidor y registra un pendiente.
echo "== PharmaSoft apagado" >> backend-estado.txt
apagar
correr s11-sin-red 1

echo "== PharmaSoft encendido" >> backend-estado.txt
bash "$CI/backend.sh" 60 >> backend-estado.txt 2>&1
correr s11-reconectar
cp "$EVID/backend-token-60min.log" "$EVID/backend-3-reconectar.log" 2>/dev/null || true

xcrun simctl io "$UDID" screenshot "$EVID/99-ios-estado-final.png" || true
kill "$LOG_PID" 2>/dev/null || true
grep "KtorHttp" ios-registro.log > ktor-ios.log 2>/dev/null || true
exit 0
