# Ficha de integración cerrada y alcance congelado (sesión 14)

Revisión de la ficha de la sesión 13 (actividad autónoma, producto D) al empezar la sesión 14.
Cada hueco queda en una de tres columnas: **cerrado**, **se cierra hoy** o **se declara** como
limitación. Lo declarado no se toca más y pasa al informe con su motivo.

## Huecos de la sesión 13

| N.º | Hueco | Gravedad | Decisión | Motivo / qué haría falta |
|---|---|---|---|---|
| 1 | Un 5xx al registrar con red borraba el producto del teléfono | Alta | Cerrado (12fe685) | — |
| 2 | Un producto rechazado (409) quedaba «Pendiente» para siempre | Alta | Cerrado (69d30a9, c3ef883, 14a04b2) | — |
| 3 | El 400 de un producto creado sin red no tenía dónde mostrarse | Media | Cerrado (fila fallida con el motivo) | — |
| 4 | Nada avisaba cuando un cambio sin red no llegaba al servidor | Media | Cerrado (9417826) | — |
| 5 | Los errores de la app no se registraban | Media | Cerrado (1bd9d46) | — |
| 6 | Editar con el token vencido pierde lo escrito al volver al login | Media | Se declara | Guardar un borrador exige otra tabla o campo en la base: es una migración del esquema y el alcance está congelado. Al crear no pasa (el producto se guarda pendiente antes de enviarlo). |
| 7 | Los pendientes no guardan quién los creó | Media | Se declara | Igual que el 6: necesita una columna nueva. Mitigación sin código: sincronizar antes de cerrar sesión (la barra muestra cuántos pendientes hay). |
| 8 | Modo avión y reinstalación probados solo en el emulador | Baja | Se cierra hoy | Paso 7: recorrido en dos celulares reales con la versión de entrega. |
| 9 | No había build firmado | Media | Se cierra hoy | Pasos 2 a 4. |
| 10 | iOS sin permiso de notificaciones real ni Firebase | Baja | Se declara | No hay Mac: Xcode y la firma de Apple solo funcionan en macOS. |
| 11 | El paso 8 del recorrido automático buscaba un producto fuera de pantalla | Baja | Cerrado (808dd5e, 999fa21) | — |

## Hueco nuevo encontrado al preparar la entrega

| N.º | Hueco | Gravedad | Decisión | Motivo |
|---|---|---|---|---|
| 12 | El celular solo llegaba a PharmaSoft con el cable (`adb reverse`): instalado desde el archivo, sin el entorno conectado, no había servicio | Alta | Se cierra hoy | La IP de la PC se configura al compilar (`pharmasoft.url` en `local.properties`) y la configuración de red permite HTTP solo hacia esa IP y los hosts de desarrollo. |

## Descartado (trabajo futuro, nunca estuvo en el alcance)

- Notificaciones remotas (push con Firebase Cloud Messaging): los avisos de la app son locales.
- Pedidos y ventas («Pronto» en la portada): el alcance comprometido es productos, clientes y sesión.

## Alcance congelado

Desde este punto no entra funcionalidad nueva, ni cambios de librerías, ni migraciones de la
base. El producto que se entrega es:

1. Inicio y cierre de sesión con JWT guardado cifrado (EncryptedSharedPreferences / Keychain) y
   roles que el servidor hace respetar (403 sin cerrar la sesión).
2. CRUD de productos y registro de clientes contra PharmaSoft, con validaciones del servidor en su campo.
3. Offline-first: la pantalla lee la base local (SQLDelight); sin red se crea en pendiente y al
   sincronizar sube antes de bajar, con reintentos y filas fallidas visibles.
4. Permiso de notificaciones con sus tres estados y avisos de envío fallido.
5. Firebase Crashlytics detrás de la interfaz `Telemetria`.
6. Android completo; iOS compila desde el mismo módulo, con las limitaciones de la fila 10.
