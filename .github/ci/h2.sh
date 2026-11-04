#!/usr/bin/env bash
# Sesion 10 (autonoma): ejecuta SQL sobre la base H2 de PharmaSoft mientras corre
# (backend.sh N archivo). Uso: h2.sh "UPDATE usuarios SET estado = FALSE WHERE username = 'vendedor'"
# La consola web de H2 (/h2-console) no esta disponible en esta version de Spring Boot,
# por eso se usa la herramienta de linea de comandos org.h2.tools.Shell.
set -eu
JAR=$(find "$HOME/.m2/repository/com/h2database/h2" -name "h2-*.jar" ! -name "*sources*" | sort | tail -1)
java -cp "$JAR" org.h2.tools.Shell -url "jdbc:h2:file:${BD_DIR:-/tmp/pharmasoft-bd}/pharmadb;AUTO_SERVER=TRUE" \
  -user sa -password "" -sql "$1" 2>&1 | grep -v "JAVA_TOOL_OPTIONS"
