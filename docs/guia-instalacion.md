# Guía de instalación y uso de PharmaMobil 1.0.0

Para alguien que no conoce el proyecto. Al terminar tendrás el servicio **PharmaSoft** corriendo en tu
PC con PostgreSQL y la app **PharmaMobil** instalada en un celular Android, conectada a ese servicio.

Son dos repositorios:

| Pieza | Repositorio | Etiqueta |
|---|---|---|
| App (Kotlin Multiplatform: Android e iOS) | https://github.com/nandoestebanif/pharmaMobil-ktor | `v1.0-final` |
| Servicio REST (Spring Boot + PostgreSQL) | https://github.com/nandoestebanif/pharmaSoft-ktor | `v1.0-final` |

Tiempo estimado: 30 a 45 minutos la primera vez (la mayor parte son descargas).

---

## 1. Requisitos (versiones con las que se probó)

| Herramienta | Versión probada | Para qué |
|---|---|---|
| Windows | 11 | PC de desarrollo |
| Git | 2.x | Clonar los repositorios |
| Docker Desktop | con Docker Compose v2 | PostgreSQL 17 en un contenedor |
| PostgreSQL | 17 (imagen `postgres:17`) | Base de datos del servicio |
| JDK | 21 (el JBR que trae Android Studio) | Servicio y compilación de la app |
| Maven | 3.9.16 (incluido como `mvnw`) | Arrancar el servicio |
| Android Studio | con Android SDK 36 | Solo si compilas la app (paso 4B) |
| Gradle | 9.1.0 (incluido como `gradlew`) | Compilar la app |
| Android Gradle Plugin / Kotlin | 9.0.1 / 2.4.10 | Lo descarga Gradle |
| Celular Android | 7.0 o superior (minSdk 24; probado en Android 16 y 17) | Correr la app |
| Xcode (opcional) | 26.6 | Solo para iOS (sección 5) |

El celular y la PC deben estar **en la misma red** (WiFi o el hotspot de un celular). Las redes de las
universidades suelen aislar los equipos entre sí: si el celular no llega a la PC, usa un hotspot.

---

## 2. Levantar el servicio PharmaSoft

1. Clona el servicio y entra a su carpeta:
   ```powershell
   git clone --branch v1.0-final https://github.com/nandoestebanif/pharmaSoft-ktor.git
   cd pharmaSoft-ktor
   ```
2. Abre **Docker Desktop** y espera a que abajo diga **Engine running**.
3. Crea el archivo de credenciales (no se sube a Git):
   ```powershell
   copy .env.example .env
   notepad .env
   ```
   Completa `DB_PASSWORD` con una contraseña que elijas y `JWT_SECRET` con una clave nueva, generada así:
   ```powershell
   [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }))
   ```
4. Levanta PostgreSQL y espera a que quede sano:
   ```powershell
   docker compose up -d
   docker compose ps        # columna STATUS: healthy
   ```
5. Arranca el servicio (la primera vez descarga dependencias, 2–5 min):
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"   # o tu JDK 21
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
   ```
   Está listo cuando la consola dice `Started PharmaBackendApplication`. Deja esa ventana abierta.
6. Comprueba que responde, desde otra ventana:
   ```powershell
   curl.exe http://localhost:8080/api/health
   ```
   Debe devolver `Backend funcionando correctamente`. La documentación de la API está en
   http://localhost:8080/swagger-ui.html.
7. Abre el puerto 8080 en el firewall (una sola vez, **PowerShell como administrador**). Sin esto el celular no llega:
   ```powershell
   New-NetFirewallRule -DisplayName "PharmaSoft 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Any
   ```

### Datos de prueba

Flyway crea al arrancar el esquema, 10 productos (uno de baja), 2 clientes y **tres usuarios de prueba**:
`admin` (rol ADMIN), `vendedor` (VENDEDOR) y `almacen` (ALMACENERO). Sus contraseñas de prueba están en el
README del servicio, sección **Datos de prueba**. Los datos se conservan al reiniciar; para volver a los
de ejemplo: `docker compose down -v` y repetir desde el paso 4.

---

## 3. Averiguar la dirección de tu PC

El celular no puede usar `localhost` ni `10.0.2.2` (eso solo sirve en el emulador). Necesita la IP de tu PC:

```powershell
ipconfig
```

En el adaptador de la red a la que está conectado el celular (*LAN inalámbrica Wi-Fi*), copia la
**Dirección IPv4**, por ejemplo `192.168.1.40`. La dirección del servicio será
`http://192.168.1.40:8080/api/v1/`. Puedes probarla desde el navegador del celular:
`http://192.168.1.40:8080/api/health`.

---

## 4. Instalar la app en Android

La dirección del servicio se fija **al compilar** la app. Hay dos caminos:

### 4A. Usar el instalable entregado (`IstanaFlores_ProyectoFinal_Android.apk`)

Está firmado y apunta a `http://10.115.77.218:8080/api/v1/`, la red de la demostración. Solo sirve si el
servicio corre en una PC con esa IP. Si no es tu caso, usa el camino 4B.

### 4B. Compilar la app apuntando a tu PC

1. Clona la app:
   ```powershell
   git clone --branch v1.0-final https://github.com/nandoestebanif/pharmaMobil-ktor.git
   cd pharmaMobil-ktor
   ```
2. Ábrela una vez en **Android Studio** (File › Open) para que cree `local.properties` con la ruta del SDK.
   Ciérralo cuando termine de sincronizar.
3. Agrega al final de `local.properties` la dirección del paso 3 (este archivo no se sube a Git):
   ```properties
   pharmasoft.url=http://192.168.1.40:8080/api/v1/
   ```
   La app solo permite HTTP sin cifrar hacia esa IP y las direcciones de desarrollo; todo lo demás exige HTTPS.
4. Compila el instalable:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
   .\gradlew.bat :androidApp:assembleDebug
   ```
   El archivo queda en `androidApp\build\outputs\apk\debug\androidApp-debug.apk`.
   (La versión de entrega, `assembleRelease`, exige el almacén de claves del autor; sin él sale sin firmar y
   Android no la instala.)

### Pasar el instalable al celular e instalarlo

1. Envía el `.apk` al celular (WhatsApp a ti mismo, Google Drive o cable USB).
2. Tócalo en el celular. La primera vez Android pide permiso para **instalar apps desconocidas** desde esa
   app (WhatsApp, Drive o Archivos): **Ajustes › Permitir desde esta fuente**, y vuelve.
3. Pulsa **Instalar**. Si ya tenías otra PharmaMobil firmada con otra clave, Android no deja instalar
   encima: desinstálala primero.

---

## 5. iOS

No se pudo instalar en un iPhone: no hay Mac ni cuenta de Apple. Lo que se entrega en su lugar:

- El archivo de Xcode generado en un Mac de GitHub Actions (workflow **S14 iOS**, Release, «Any iOS
  Device», versión 1.0.0 (1), **sin firma**), con el registro en la rama `ci-evidencias-ios-s14`.
- La app de iOS corrió en el simulador de iPhone en las sesiones 10 y 11 (ramas `ci-evidencias-ios-s10` y
  `ci-evidencias-ios-s11`).

Con un Mac y una cuenta gratuita de Apple: abrir `iosApp/iosApp.xcodeproj` en Xcode, elegir el equipo en
**Signing & Capabilities**, conectar el iPhone, elegirlo como destino y pulsar **Run**. Para un iPhone real,
compilar con la IP de la PC: `PHARMASOFT_URL=http://192.168.1.40:8080/api/v1/` en
`iosApp/Configuration/Config.xcconfig`.

---

## 6. Recorrido guiado

| N.º | Qué haces | Qué debes ver |
|---|---|---|
| 1 | Abres PharmaMobil | Pantalla **Iniciar sesión** con el logo |
| 2 | Escribes un usuario inventado y una contraseña cualquiera | Aviso rojo **«Usuario o contraseña incorrectos.»** |
| 3 | Entras con `admin` | Inicio: **«Hola, Carlos Ramirez Torres»**, etiqueta **Administrador** y la tarjeta **Avisos de sincronización** |
| 4 | Pulsas **Activar avisos** | Diálogo de Android **«¿Permitir que PharmaMobil te envíe notificaciones?»** |
| 5 | Entras a **Productos** | 9 activos y 1 de baja, barra **«Inventario al día»** |
| 6 | **Nuevo producto** con nombre `Ab` | Debajo del campo Nombre: **«El nombre debe tener entre 3 y 150 caracteres»** |
| 7 | **Nuevo producto** con un nombre válido, precio y stock | **«Producto "…" registrado correctamente»** y aparece en la lista |
| 8 | Abres un producto › **Eliminar** › **Sí, eliminar** | **«Producto eliminado»**; pasa a la pestaña *De baja* |
| 9 | Activas el **modo avión** y creas otro producto | Barra **«Sin conexión: mostrando datos guardados»** y el producto con el chip **Pendiente** |
| 10 | Quitas el modo avión y pulsas **Sincronizar** | El chip desaparece y la barra vuelve a **«Inventario al día»** |
| 11 | Menú ☰ › **Cerrar sesión** | **«Cerraste sesión.»** con los campos vacíos |
| 12 | Entras con `vendedor` y abres un producto | Solo el botón **Compartir**: el vendedor no puede editar ni eliminar |

---

## 7. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `docker compose up` responde *open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified* | Docker Desktop no está abierto | Abrirlo, esperar **Engine running** y repetir |
| `docker compose up` responde *Falta DB_PASSWORD en el archivo .env* | No se creó `.env` | Paso 2.3 |
| El servicio no arranca: *password authentication failed for user "pharma"* | El volumen se creó con otra contraseña | Usar la misma en `.env` o borrar los datos con `docker compose down -v` |
| La app dice **«No se pudo conectar con el servidor»** | IP equivocada, firewall cerrado o el celular en otra red (o la red de la universidad aísla equipos) | Abrir `http://IP:8080/api/health` en el navegador del celular; revisar el paso 2.7 y usar un hotspot |
| Al instalar: *¿Quieres actualizar esta app?* y luego *App no instalada* | Ya hay una PharmaMobil firmada con otra clave | Desinstalar la anterior e instalar de nuevo |
| `gradlew` o `mvnw` dicen *JAVA_HOME is not set* | No hay JDK en la ruta | `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` |
| Tras quitar el modo avión, **Sincronizar** no envía lo pendiente (defecto D-01) | La app conserva la conexión de cuando no había red | Cerrar la app desde recientes y abrirla: se envía sola |

---

## 8. Registro de la prueba de esta guía

| Quién la siguió | Equipo / curso | Fecha | Hasta dónde llegó sin preguntar | Pasos que hubo que corregir |
|---|---|---|---|---|
| (pendiente) | | | | |
