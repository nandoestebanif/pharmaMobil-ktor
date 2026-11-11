# Pruebas en dispositivos reales y registro de defectos (sesión 14, paso 7)

Versión probada: **PharmaMobil 1.0.0** (`versionCode 1`, `pe.edu.upeu.pharmamobil`), instalable de
entrega `IstanaFlores_ProyectoFinal_Android.apk`, firmado con el almacén propio
(SHA-256 del certificado `cf4441fc0591244b62424b63f0c209a75eb849ec958d14e47eb48345fe34892d`),
minificado con R8. Servicio: PharmaSoft con el perfil `postgres` (PostgreSQL 17 en Docker) en la PC,
alcanzado por la IP del hotspot (`http://10.115.77.218:8080`). Fecha: 11 de noviembre de 2026.

La instalación se hizo desde el archivo (enviado por WhatsApp), sin cable ni Android Studio conectado.

## Dispositivos

| Clave | Equipo | Sistema | Red |
|---|---|---|---|
| D1 | Samsung Galaxy S24 Ultra (SM-S928B) | Android 16 · One UI 8.5 | Comparte el hotspot de la PC |
| D2 | Samsung Galaxy S25 Ultra (SM-S938B) | Android 17 · One UI 9.0 | Conectado al hotspot de D1 |

## Matriz de pruebas

| N.º | Paso del recorrido | Qué se observa | D1 (Android 16) | D2 (Android 17) |
|---|---|---|---|---|
| 1 | Instalación desde el archivo firmado | Instala sin el entorno; nombre e icono definitivos | Cumple | Cumple |
| 2 | Inicio de sesión incorrecto y correcto | «Usuario o contraseña incorrectos.» distinto de la falta de conexión | Cumple | Cumple |
| 3 | CRUD con un rechazo de validación | El 400 llega al campo Nombre | Cumple (crear «fer», dar de baja Vitamina C, «Ab» rechazado) | Cumple (crear «adiel», dar de baja Paracetamol, «Ab» rechazado) |
| 4 | Operación sin privilegio (vendedor) | Rechazo informado y la sesión sigue abierta | Cumple: la versión de entrega no ofrece Editar ni Eliminar al vendedor; el servicio responde 403 si se intenta | Cumple (igual) |
| 5 | Modo avión: abrir, leer y crear | La pantalla muestra datos y lo creado queda pendiente | Cumple («jsjduss» Pendiente; base local vacía en ese momento: estado vacío informativo) | Cumple (lista de 11 y «ndfjkf» Pendiente) |
| 6 | Recuperar la conexión y sincronizar | Lo pendiente sube, nada se duplica, la pantalla se actualiza | Cumple | **Con defecto D-01**: subió tras reabrir la app |
| 7 | Permiso: conceder, denegar, denegar para siempre | Tres estados; el tercero ofrece Ajustes | Cumple denegar y denegar para siempre («Abrir Ajustes») | Cumple (igual) |
| 8 | Cerrar sesión y reabrir | Token borrado; pide login | Cumple | Cumple |

El estado «concedido» y el camino por Ajustes se documentaron en la sesión 13 en D1 (mismo código).
El CI «S14 Entrega» recorrió además la versión minificada completa en un emulador Android 15 sin
cierres de la app (serialización e inyección funcionan con R8).

## Registro de defectos

### D-01 · Sincronizar no envía lo pendiente justo después de recuperar la red

| Campo | Contenido |
|---|---|
| Dispositivo y versión | D2 · Samsung SM-S938B · Android 17 (One UI 9.0). No se reprodujo en D1. |
| Pasos para reproducir | 1. Abrir la app con sesión de admin. 2. Activar el modo avión. 3. Productos › Nuevo producto, registrar «ndfjkf» (queda Pendiente). 4. Quitar el modo avión y esperar a que el teléfono vuelva al hotspot. 5. Pulsar Sincronizar. |
| Resultado esperado | El producto sube, pierde el chip «Pendiente» y la barra pasa a «Inventario al día». |
| Resultado obtenido | El producto sigue pendiente. Al cerrar la app desde recientes y abrirla otra vez, se envía y la lista queda al día (11 activos, sin duplicado). |
| Severidad | Media: estorba el recorrido, no pierde datos y tiene solución sin código. |
| Estado | Pendiente (alcance congelado). Solución temporal: cerrar y reabrir la app. Causa probable: el cliente HTTP conserva la conexión creada mientras no había red; se corregiría reintentando la sincronización cuando Android avisa que la red volvió. |

### D-02 · La dirección del servicio queda fija en el instalable

| Campo | Contenido |
|---|---|
| Dispositivo y versión | D1 y D2 (todas las versiones). |
| Pasos para reproducir | 1. Compilar con `pharmasoft.url` = IP de la PC. 2. Cambiar la PC de red (otro WiFi u otro hotspot). 3. Abrir la app. |
| Resultado esperado | La app llega a PharmaSoft. |
| Resultado obtenido | Si la IP de la PC cambia, la app queda sin conexión con el servicio hasta recompilar con la nueva IP. |
| Severidad | Media. |
| Estado | Descartado como defecto de código, declarado como limitación del entorno de pruebas: PharmaSoft no está publicado en un servidor con dirección fija. En la sustentación se usa la misma red. |

### Observación (no es defecto)

Activar el modo avión en D1 apaga también el hotspot, así que la PC y D2 pierden la red a la vez.
Después de reactivarlo la PC conservó la IP `10.115.77.218` y D1 sincronizó sin problemas.
