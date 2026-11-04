#!/usr/bin/env bash
# Sesion 10: recorre los flujos de Maestro en una plataforma y deja las
# capturas en $EVID. Uso: flujos-s10.sh android|ios APP_ID [UDID]
# Ningun fallo corta el script: se publica lo que se haya capturado.
set -u
PLATAFORMA="$1"
APP_ID="$2"
DISPOSITIVO="${3:-}"
export PATH="$PATH:$HOME/.maestro/bin"
export MAESTRO_CLI_NO_ANALYTICS=1
FLUJOS="$GITHUB_WORKSPACE/.github/ci/maestro"
OPCIONES=()
[ -n "$DISPOSITIVO" ] && OPCIONES=(--device "$DISPOSITIVO")
cd "$EVID"

correr() {
  local flujo="$1" intentos="${2:-2}"
  for intento in $(seq 1 "$intentos"); do
    if maestro ${OPCIONES[@]+"${OPCIONES[@]}"} test -e APP_ID="$APP_ID" -e PLATAFORMA="$PLATAFORMA" \
         "$FLUJOS/$flujo.yaml" > "maestro-$flujo-$intento.log" 2>&1; then
      echo "::notice title=Maestro $PLATAFORMA::$flujo completado"
      return 0
    fi
    echo "::warning title=Maestro $PLATAFORMA::$flujo fallo (intento $intento): $(tail -4 "maestro-$flujo-$intento.log" | tr '\n' ' ' | cut -c1-700)"
  done
  return 1
}

correr s10-login
correr s10-permisos
correr s10-403

# Expiracion real: tokens de 1 minuto, login, espera de 75 s y nueva peticion.
bash "$GITHUB_WORKSPACE/.github/ci/backend.sh" 1
if correr s10-expira; then
  sleep 75
  correr s10-expira-2 1
fi
bash "$GITHUB_WORKSPACE/.github/ci/backend.sh" 60
exit 0
