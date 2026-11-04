#!/usr/bin/env bash
# Sesion 10: descarga PharmaSoft del profesor y le agrega el perfil h2
# (driver H2 en el pom, application-h2.yaml y las migraciones de db/migration-h2).
set -euo pipefail
git clone --depth 1 https://github.com/dreyna/pharmaSoft.git backend
cp -R .github/ci/pharmasoft-h2/. backend/src/main/resources/
cd backend
perl -0pi -e 's#(<artifactId>ojdbc11</artifactId>\s*<scope>runtime</scope>\s*</dependency>)#$1\n\t\t<dependency>\n\t\t\t<groupId>com.h2database</groupId>\n\t\t\t<artifactId>h2</artifactId>\n\t\t\t<scope>runtime</scope>\n\t\t</dependency>#' pom.xml
grep -q "com.h2database" pom.xml
chmod +x mvnw
git log -1 --format='backend: %h %s' | tee -a "${EVID:-/tmp}/entorno.txt"
