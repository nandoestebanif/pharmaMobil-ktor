#!/usr/bin/env bash
# Publica la carpeta $EVID en la rama $RAMA_EVIDENCIAS del repositorio (un solo
# commit, reemplaza la ejecucion anterior). Se ve sin iniciar sesion en GitHub.
set -u
mkdir -p "$EVID/maestro-debug"
# Maestro guarda capturas y registros de depuracion fuera de la carpeta de trabajo.
find "$HOME/.maestro/tests" -type f \( -name "*.png" -o -name "*.log" -o -name "*.json" \) \
  -exec cp {} "$EVID/maestro-debug/" \; 2>/dev/null || true
find "$GITHUB_WORKSPACE/.github/ci/maestro" -name "*.png" -exec cp {} "$EVID/" \; 2>/dev/null || true
# Los registros completos pesan decenas de MB: no se publican.
find "$EVID" -type f -size +3M -delete
cd "$EVID"
{
  echo "# Evidencias ($GITHUB_JOB)"
  echo
  echo "- Ejecucion: $GITHUB_RUN_NUMBER (id $GITHUB_RUN_ID)"
  echo "- Commit: $GITHUB_SHA"
  echo "- Rama: $GITHUB_REF_NAME"
  echo "- Fecha (UTC): $(date -u '+%Y-%m-%d %H:%M:%S')"
  echo
  echo "## Archivos"
  ls -1 | sed 's/^/- /'
} > README.md
git init -q
git checkout -q -b "$RAMA_EVIDENCIAS"
git add -A
git -c user.name="github-actions[bot]" -c user.email="41898282+github-actions[bot]@users.noreply.github.com" \
  commit -qm "Evidencias de la ejecucion $GITHUB_RUN_NUMBER ($GITHUB_SHA)"
git push -f "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" "$RAMA_EVIDENCIAS"
echo "::notice title=Evidencias publicadas::https://github.com/${GITHUB_REPOSITORY}/tree/${RAMA_EVIDENCIAS}"
