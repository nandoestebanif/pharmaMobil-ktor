This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
---

## Conectividad REST (Sesión 7 · rama `feature/ktor-client`)

El inventario de Productos ya no viene del repositorio en memoria: se obtiene del backend **PharmaSoft** con Ktor Client.

### Conexión

| Dato | Valor |
|---|---|
| Backend | [dreyna/pharmaSoft](https://github.com/dreyna/pharmaSoft) (Spring Boot + Oracle, puerto 8080) |
| URL base Android (emulador) | `http://10.0.2.2:8080/api/v1/` |
| URL base iOS (simulador) | `http://localhost:8080/api/v1/` |
| Endpoint consumido | `GET productos?pagina=0&tamanio=20` |
| Prueba de vida | `GET http://localhost:8080/api/health` → 200 |
| Swagger | `http://localhost:8080/swagger-ui.html` |

La URL base la aporta `platformModule` de cada plataforma (`ConfiguracionApi`). En un celular físico se reemplaza por la IP local de la PC.

### Dependencias y permisos

- Ktor `3.6.0`: `core`, `content-negotiation`, `serialization-kotlinx-json` y `logging` en `commonMain`; `okhttp` en `androidMain` y `darwin` en `iosMain`.
- Plugin `kotlin("plugin.serialization")` con **la misma versión de Kotlin del proyecto** (`version.ref = "kotlin"`).
- OkHttp fijado en `5.4.0`: la versión 5.5.0 que trae Ktor 3.6.0 exige `compileSdk 37` y el proyecto compila con 36.
- Android: permiso `INTERNET` y `network_security_config.xml`, que permite HTTP en claro **solo** hacia `10.0.2.2`, `localhost` y `127.0.0.1`.
- iOS: `NSAllowsLocalNetworking` en `Info.plist`. ATS sigue exigiendo HTTPS para todo lo demás.

### Diccionario de DTO

El backend responde con un envoltorio paginado (`PaginaResponseDTO`). Los productos vienen dentro de `contenido`.

| Campo JSON (`ProductoResponseDTO`) | Tipo DTO | Obligatorio | Dominio (`Producto`) |
|---|---|---|---|
| `id` | `Long` | Sí | `id` |
| `nombre` | `String` | Sí | `nombre` (sin espacios extremos) |
| `precio` (BigDecimal) | `Double` | Sí | `precio` |
| `stock` | `Int` | No (0) | `stock` |
| `estado` | `Boolean` | No (true) | Filtro: los `false` (baja lógica) no se muestran |
| `categoriaId`, `categoriaNombre`, `fechaCreacion`, `fechaModificacion` | — | — | Ignorados (`ignoreUnknownKeys`) |

### Flujo

`ProductoScreen → ProductoViewModel → ListarProductosUseCase → ProductoRepositoryImpl → ProductoApi → HttpClient (OkHttp/Darwin)`

- `data/remote/HttpClientFactory.kt`: ContentNegotiation, Logging (`HEADERS`, prefijo `KtorHttp`), HttpTimeout (15 s / 10 s), DefaultRequest y `expectSuccess`.
- `data/remote/ErroresRed.kt`: traduce las excepciones de Ktor (4xx, 5xx, timeout, sin red, JSON inválido) a un mensaje para el usuario. La UI no conoce Ktor.
- `registrar()` remoto (POST) queda para la sesión 8.

### Pruebas (`ProductoRepositoryImplTest`, con `MockEngine`)

1. Éxito 200: deserializa la página y mapea al dominio.
2. Error 404: devuelve `Result.failure` con un mensaje legible.
3. Sin conexión: la app no se cae.
4. Timeout: devuelve un mensaje de tiempo agotado.
5. JSON con campos nuevos: se ignoran.

Se ejecutan con `./gradlew :shared:testAndroidHostTest`.

En el emulador, cada escenario se provoca cambiando `escenario` en `PlatformModule.android.kt`:

| Escenario | Valor | Lo que ve el usuario |
|---|---|---|
| Tiempo agotado | `TIEMPO_AGOTADO` | "El servidor tardó demasiado en responder." |
| Campo desconocido | `JSON_ESTRICTO` | "La respuesta del servidor no tiene el formato esperado." |
| Sin conexión | modo avión | "No se pudo conectar con el servidor. Revisa tu conexión." |

### Evidencias

En Logcat, filtra por `KtorHttp` para ver `REQUEST: http://10.0.2.2:8080/api/v1/productos...` y `RESPONSE: 200`.

---

## CRUD REST completo (Sesión 8 · rama `feature/crud-productos-istana`)

La pantalla de Productos ya hace las cuatro operaciones contra PharmaSoft: listar, registrar, editar y eliminar.

### Endpoints consumidos

| Operación en la app | Método y ruta | Éxito | Errores que se manejan |
|---|---|---|---|
| Cargar inventario | `GET /api/v1/productos?pagina=0&tamanio=20` | 200 (página) | sin conexión, timeout, 5xx |
| Abrir un producto para editar | `GET /api/v1/productos/{id}` | 200 | 404 |
| Registrar | `POST /api/v1/productos` | 201 | 400, 409 |
| Guardar cambios | `PUT /api/v1/productos/{id}` | 200 | 400, 404, 409 |
| Eliminar | `DELETE /api/v1/productos/{id}` | 204 sin cuerpo | 404, 409 |

El `DELETE` de PharmaSoft es una baja lógica (`estado = false`): el producto sale del inventario activo, pero su nombre sigue ocupado y registrar otro igual devuelve 409.

### Productos dados de baja

El listado del backend incluye los productos con `estado = false`. La app los separa del inventario activo:

- El filtro **Activos / De baja** del encabezado alterna entre las dos listas.
- En "De baja", el botón **Reactivar** trae el producto (`GET /productos/{id}`) y lo actualiza con `estado = true` (`PUT /productos/{id}`). Conserva su nombre, precio, stock y categoría.
- Si se intenta registrar un producto con el nombre de uno dado de baja, el 409 se muestra como "Ya existe un producto dado de baja con ese nombre. Reactívalo desde "De baja"."

`ReactivarProductoUseCase` reutiliza las operaciones `obtener` y `actualizar` del repositorio: el contrato sigue teniendo cinco operaciones.

### Capas

- `data/remote/dto`: `ProductoRequestDto`, `ProductoResponseDto`, `PaginaResponseDto<T>` y `ErrorResponseDto`.
- `data/remote/ProductoApi.kt`: `listar`, `obtener`, `crear`, `actualizar` y `eliminar` (este último no llama a `body()`).
- `data/remote/EjecutarLlamada.kt`: **único punto** donde las excepciones de Ktor se traducen a `ErrorApi`.
- `domain/error/ErrorApi.kt`: `Validacion`, `NoEncontrado`, `Conflicto`, `NoAutorizado`, `Servidor`, `SinConexion`, `TiempoAgotado` y `RespuestaInesperada`.
- `data/repository/ProductoRepositorioRest.kt`: implementa las cinco operaciones de `ProductoRepository`. El repositorio en memoria se conserva para pruebas.
- `domain/usecase`: `Listar`, `Obtener`, `Registrar`, `Actualizar`, `Eliminar` y `ReactivarProductoUseCase`; todos devuelven `Result`.
- `presentation`: ninguna clase importa `io.ktor`. `mensajeDe(ErrorApi)` convierte el error en el texto que ve el usuario.

### Estados de la interfaz

`ProductoUiState` separa dos cosas:

- `fase` (`Cargando`, `SinProductos`, `ConProductos`, `Error`): el estado de la pantalla completa.
- `operacion` (`Inactiva`, `EnCurso(tipo, productoId)`, `Fallida(mensaje)`): el estado de la acción del usuario.

Al guardar o eliminar, la lista sigue visible: solo se deshabilitan los botones y la fila afectada muestra progreso. Tras cada cambio se vuelve a pedir el listado, sin pasar por `Cargando`.

### Manejo de errores

Las excepciones de Ktor se traducen en un único punto (`ejecutarLlamada`) a un `ErrorApi` del dominio. La presentación no conoce códigos HTTP: recibe el `ErrorApi` y decide dónde mostrarlo.

| Respuesta | `ErrorApi` | Qué ve el usuario | Dónde |
|---|---|---|---|
| 400 con `validationErrors` | `Validacion(porCampo)` | El mensaje del servidor | Debajo del campo (nombre, precio o stock) |
| 404 | `NoEncontrado` | "El producto ya no existe en el servidor." | Aviso sobre la lista, que se refresca |
| 409 | `Conflicto(mensaje)` | El mensaje del servidor (nombre duplicado, producto ya inactivo) | Aviso sobre la lista, que se refresca |
| 401 / 403 | `NoAutorizado` | "No tienes permiso para realizar esta operación." | Aviso |
| 5xx | `Servidor` | "El servidor tuvo un problema. Intenta de nuevo en un momento." | Aviso |
| Sin red o servidor apagado | `SinConexion` | "No se pudo conectar con el servidor. Revisa tu conexión." | Aviso (o pantalla de error con Reintentar si falla la carga inicial) |
| Timeout | `TiempoAgotado` | "El servidor tardó demasiado en responder." | Igual que el anterior |
| JSON inesperado | `RespuestaInesperada` | "La respuesta del servidor no tiene el formato esperado." | Igual que el anterior |

Reglas que sigue la pantalla:

- Un error de una **operación** (crear, actualizar, eliminar, reactivar) va a `operacion = Fallida(mensaje)`: la lista sigue visible. Solo el fallo de la **carga inicial** cambia `fase` a `Error`.
- La validación local (nombre vacío, precio ≤ 0, stock negativo) se resuelve en el caso de uso y no llega a hacer la petición.
- En 404 y 409 la lista que tenía la app estaba desactualizada, así que se vuelve a pedir el listado.
- `CancellationException` nunca se convierte en `ErrorApi`: `ejecutarLlamada` la relanza y no se muestra ningún mensaje.

Comportamientos de PharmaSoft verificados contra el backend (bitácora de la actividad autónoma 08):

- Repetir un `DELETE` sobre el mismo producto no devuelve 404 sino **409** ("El producto X ya se encuentra inactivo"), porque la baja es lógica y el registro sigue existiendo.
- Un precio `0` o negativo lo detiene la validación local. Para ver el 400 del servidor bajo el campo precio hay que enviar un valor positivo menor que `0.01` (p. ej. `0.001`).
- El `ViewModel` está ligado a la `Activity`, no a la pantalla: salir de Productos a Inicio **no cancela** una operación en curso; termina y la lista queda actualizada al volver. La cancelación solo ocurre cuando se destruye la `Activity`.

Escenarios que no se pueden provocar desde la interfaz: se activan cambiando una línea (`escenario = EscenarioPrueba.…`) en `shared/src/androidMain/.../di/PlatformModule.android.kt`.

| `EscenarioPrueba` | Efecto |
|---|---|
| `NINGUNO` | Uso normal |
| `TIEMPO_AGOTADO` | Timeout de 1 ms: toda petición termina en `TiempoAgotado` |
| `JSON_ESTRICTO` | `ignoreUnknownKeys = false`: la respuesta termina en `RespuestaInesperada` |
| `RESPUESTA_LENTA` | Crear, actualizar y eliminar tardan 8 s: da tiempo a salir de la pantalla |

### Categoría

El backend exige `categoriaId` y el formulario aún no lo pide. Al crear se envía `CATEGORIA_POR_DEFECTO` (id 1, en `di/AppModule.kt`); al editar se conserva la categoría que ya tenía el producto.

### Pruebas

`./gradlew :shared:testAndroidHostTest`

- `EjecutarLlamadaTest`: traducción de 400, 401/403, 404, 409, 500 y cancelación.
- `ProductoRepositorioRestTest`: los cinco verbos con `MockEngine` (método, URL, cuerpo enviado y 204 sin cuerpo).
- `CrudProductoUseCasesTest`: obtener, actualizar y eliminar.
- `ProductoViewModelTest`: transiciones de `fase` (`Cargando` → `ConProductos` / `SinProductos` / `Error`) y de `operacion` (en curso, fallida, validación bajo el campo sin pasar a `Error`, 404 y 409 con refresco, cancelación sin mensaje).

---

## Capacidades nativas (Sesión 9 · rama `feature/expect-actual-istana`)

Capacidades que dependen del sistema operativo, resueltas con los dos mecanismos que ofrece Kotlin Multiplatform. La interfaz Compose y el dominio siguen en `commonMain`.

| Capacidad | Mecanismo | Código común | Android | iOS |
|---|---|---|---|---|
| Formato de moneda | `expect` / `actual` | `platform/Formato.kt` | `platform/Formato.android.kt` (`java.text.NumberFormat`, es-PE) | `platform/Formato.ios.kt` (`NSNumberFormatter`, es_PE) |
| Compartir | Interfaz + inyección | `domain/platform/Compartidor.kt` | `platform/CompartidorAndroid.kt` (`Intent.ACTION_SEND`) | `platform/CompartidorIos.kt` (`UIActivityViewController`) |
| Botón Atrás del sistema | `expect` / `actual` | `platform/BotonAtras.kt` | `platform/BotonAtras.android.kt` (`BackHandler`) | `platform/BotonAtras.ios.kt` (sin implementación: iOS no tiene ese botón) |

Las rutas son relativas a `shared/src/<sourceSet>/kotlin/pe/edu/upeu/pharmamobil/`.

### Por qué un mecanismo distinto para cada una

- **`formatearSoles` es un `expect`**: es una función pura, sin estado ni dependencias. El compilador exige el `actual` en cada plataforma; si falta uno, el proyecto no compila.
- **`AlPulsarAtras` también es un `expect`**: es un composable sin dependencias, y sirve de ejemplo de un `actual` vacío a propósito. El botón Atrás solo existe en Android; en iOS se vuelve con la flecha de la barra superior.
- **`Compartidor` es una interfaz**: necesita algo que `commonMain` no conoce (un `Context` en Android, un controlador de vista en iOS). Recibir dependencias por constructor y sustituirla en una prueba es sencillo con una interfaz y complicado con un `expect`.
- Las dos implementaciones de `Compartidor` se registran dentro del `expect val platformModule`, así que el compilador sigue exigiendo que cada plataforma aporte la suya.

### Dónde se usa

- `presentation/producto/ProductoUi.kt`: `Producto.aUi()` llama a `formatearSoles`. El dominio conserva el precio como número y el composable solo pinta el texto.
- `domain/usecase/TextoParaCompartir.kt`: `Producto.comoTextoParaCompartir()` arma el texto en código común (`Naproxeno 550mg — S/ 7.80 · Stock: 6`).
- `presentation/detalle/`: al tocar una fila del inventario se abre la pantalla de detalle del producto (`GET /productos/{id}`) con el botón **Compartir**. `DetalleProductoViewModel` recibe el `Compartidor` por constructor.
- Ninguna clase de `presentation` ni de `domain` importa `android.*`, `java.*` ni `platform.UIKit`.

### Navegación y pantallas

La app usa una pila de pantallas propia (`navigation/PilaDeNavegacion.kt`), sin librería de navegación. La barra superior muestra el menú en Inicio y la flecha de volver en el resto; el botón Atrás de Android hace lo mismo que la flecha.

| Pantalla | Se llega desde | Qué ofrece |
|---|---|---|
| Inicio | Arranque | Acceso a los módulos |
| Productos | Inicio o menú lateral | Inventario, filtro Activos / De baja y botón **Nuevo producto** |
| Detalle del producto | Tocar una fila | Datos del producto, **Compartir**, **Editar** y **Eliminar** |
| Nuevo / Editar producto | Botón Nuevo producto o Editar | Formulario; al guardar regresa al inventario ya actualizado |

- Las tres pantallas de productos comparten `ProductoViewModel`, así que el estado de la operación (`operacion`) y sus avisos se ven igual desde cualquiera.
- La pila se guarda con `rememberSaveable`: sobrevive a la rotación de la pantalla.
- `presentation/components/Tarjeta.kt` reúne la tarjeta, el icono en recuadro y la etiqueta que usan todas las pantallas.

### Registro en Koin

```kotlin
// androidMain/di/PlatformModule.android.kt
single<Compartidor> { CompartidorAndroid(androidContext()) }

// iosMain/di/PlatformModule.ios.kt
single<Compartidor> { CompartidorIos() }
```

En Android el `Context` es el de la aplicación (lo entrega `MainApplication` con `androidContext(...)`), por eso el selector se abre con `FLAG_ACTIVITY_NEW_TASK`.

### Qué cambia entre plataformas

El mismo `Double` no se ve igual: el símbolo, el espacio que lo separa del monto y los separadores los decide cada sistema. Por eso las pruebas de `FormatoTest` no comparan con un texto fijo: verifican el símbolo, los dos decimales y las cifras.

### Interoperabilidad Kotlin-Swift

| En Swift (`iosApp/iosApp/`) | Por qué se llama así |
|---|---|
| `import Shared` | El módulo `shared` se compila como framework con `baseName = "Shared"` |
| `KoinIosKt.doInitKoinIos()` | Las funciones de nivel superior de `KoinIos.kt` quedan en la clase `KoinIosKt`; Swift reserva los nombres que empiezan con `init`, así que Kotlin antepone `do` |
| `MainViewControllerKt.MainViewController()` | Mismo criterio: función de nivel superior de `MainViewController.kt` |

### iOS sin Mac: simulador e iPhone físico

El workflow `.github/workflows/ios-evidencias.yml` corre en un runner macOS de GitHub Actions con cada push de la rama.

- **Simulador** (job `ios-simulador`): levanta PharmaSoft con el perfil h2, compila la app, la abre en un simulador de iPhone y recorre listado, detalle y Compartir con Maestro. Las capturas y los registros quedan en la rama `ci-evidencias-ios`.
- **iPhone físico** (job `ios-dispositivo`): genera `PharmaMobil.ipa` sin firma en la rama `ci-ios-instalador`. Se instala desde Windows con Sideloadly y un Apple ID. Solo corre si `iosApp/Configuration/servidor-dispositivo.txt` contiene la IP de la PC en la red local.

En el simulador la app usa `http://localhost:8080/api/v1/`. En un iPhone físico `localhost` es el propio teléfono, así que la URL se escribe al compilar en la clave `PharmaSoftUrl` del `Info.plist` y `PlatformModule.ios.kt` la lee. El teléfono y la PC deben estar en la misma red, y el firewall de Windows debe permitir el puerto 8080.

`gradlew` está en el repositorio con permiso de ejecución (`git update-index --chmod=+x gradlew`): sin él, el paso de Xcode que lo invoca falla en macOS.

### Punto de control 1: el error que exige los `actual`

El primer commit de la rama declara solo el `expect`. En ese estado el proyecto no compila:

```
git log --oneline --grep="declara el expect"      # ubica el commit
git checkout <hash>
.\gradlew :shared:compileAndroidMain
# e: .../platform/Formato.kt:11:1 Expected formatearSoles has no actual declaration in module <commonMain> for JVM
git checkout feature/expect-actual-istana
```

### Pruebas

`./gradlew :shared:testAndroidHostTest`

- `FormatoTest`: símbolo, dos decimales, redondeo y cifras del monto formateado.
- `TextoParaCompartirTest`: el texto incluye nombre, precio formateado y stock.
- `DetalleProductoViewModelTest`: carga del detalle, errores y que `compartir()` entrega al `Compartidor` el texto armado en común. Usa `CompartidorFalso`.
- `PilaDeNavegacionTest`: avanzar, volver, regresar al inventario tras guardar, cambio de módulo desde el menú y guardado de la pila como texto.
- `AppModuleTest`: Koin resuelve `DetalleProductoViewModel`; el `Compartidor` y el motor HTTP se sustituyen por dobles, de modo que la prueba ya no sale a la red.

---

## Código específico de plataforma (Actividad autónoma 9)

Inventario de todo lo que el proyecto resuelve por plataforma. El resto (dominio, casos de uso, Ktor, ViewModels y pantallas Compose) vive una sola vez en `commonMain`.

Rutas relativas a `shared/src/<sourceSet>/kotlin/pe/edu/upeu/pharmamobil/`.

| Capacidad | Declaración en `commonMain` | `androidMain` (API nativa) | `iosMain` (API nativa) |
|---|---|---|---|
| Formato de moneda | `platform/Formato.kt` · `expect fun formatearSoles(valor: Double): String` | `platform/Formato.android.kt` · `java.text.NumberFormat` con `Locale.forLanguageTag("es-PE")` | `platform/Formato.ios.kt` · `NSNumberFormatter` con `NSLocale("es_PE")` |
| Compartir producto | `domain/platform/Compartidor.kt` · `interface Compartidor` (se inyecta con Koin) | `platform/CompartidorAndroid.kt` · `Intent.ACTION_SEND` + `Intent.createChooser` | `platform/CompartidorIos.kt` · `UIActivityViewController` |
| Módulo de inyección | `di/AppModule.kt` · `expect val platformModule: Module` | `di/PlatformModule.android.kt` · `module` con `androidContext()`, motor `OkHttp` | `di/PlatformModule.ios.kt` · `module` sin contexto, motor `Darwin` |
| Información del dispositivo (tercera capacidad) | `platform/InfoDispositivo.kt` · `expect class InfoDispositivo()` con `sistema`, `version` y `modelo` | `platform/InfoDispositivo.android.kt` · `android.os.Build` (`VERSION.RELEASE`, `VERSION.SDK_INT`, `MANUFACTURER`, `MODEL`) | `platform/InfoDispositivo.ios.kt` · `UIDevice.currentDevice` (`systemName`, `systemVersion`, `model`) |
| Botón Atrás del sistema | `platform/BotonAtras.kt` · `@Composable expect fun AlPulsarAtras(habilitado: Boolean, alVolver: () -> Unit)` | `platform/BotonAtras.android.kt` · `androidx.activity.compose.BackHandler` | `platform/BotonAtras.ios.kt` · vacío a propósito (iOS no tiene ese botón) |

Fuera de `shared`, cada app tiene su punto de entrada: `androidApp/.../MainActivity.kt` y `iosApp/iosApp/*.swift` (interoperabilidad descrita en la sección anterior).

### Tercera capacidad: pantalla «Acerca de»

Se abre desde el menú lateral. Muestra lo que entrega `InfoDispositivo` (sistema, versión y modelo) y cómo formatea la moneda la plataforma: el monto 24.5 pasado por `formatearSoles` y el carácter exacto que queda entre `S/` y el monto (`describirSeparador`, en `presentation/acerca/AcercaDeUi.kt`). Así la diferencia entre plataformas se ve en la propia app.

- `InfoDispositivo` es una clase `expect` porque no necesita dependencias: Android lee campos estáticos de `Build` y iOS pregunta a `UIDevice`. En el simulador de iOS el modelo sale de la variable `SIMULATOR_DEVICE_NAME`.
- Las clases `expect`/`actual` están en Beta: `shared/build.gradle.kts` activa `-Xexpect-actual-classes` para que el compilador no lo advierta en cada compilación.
- La pantalla se puede abrir al arrancar con `-pantalla acerca` en iOS y con el extra `pantalla=acerca` del `Intent` en Android (lo usa el workflow de evidencias).

### Cómo se verifica que nada se filtró a `commonMain`

```
grep -rnE "^import (android\.|platform\.)" shared/src/commonMain   # 0 resultados
```

### Qué pasa si falta un `actual`

Cada plataforma solo comprueba su propio `actual`. Con el de iOS comentado, `./gradlew :shared:compileAndroidMain` sigue compilando y `./gradlew :shared:compileKotlinIosSimulatorArm64` falla con:

```
e: .../platform/Formato.kt:11:1 Expected formatearSoles has no actual declaration in module <commonMain> for Native
```

En Windows no se compila iOS, así que un `actual` de iOS faltante no se nota en la PC: lo detecta el workflow de GitHub Actions al compilar en macOS.

### Pruebas

- `InfoDispositivoTest`: el `actual` de la plataforma de la prueba entrega sistema, versión y modelo.
- `AcercaDeUiTest`: `describirSeparador` reconoce el espacio normal (U+0020), el de no separación (U+00A0) y el símbolo pegado al monto.
- `PilaDeNavegacionTest`: la pantalla de arranque por nombre (`acerca`, `productos`) y el guardado de «Acerca de» en la pila.

---

## Autenticación (Sesión 10 · rama `feature/auth-jwt-istana`)

La app inicia sesión contra PharmaSoft con JWT, guarda el token cifrado en cada plataforma y lo adjunta a cada petición con el plugin `Auth` de Ktor. Ninguna llamada escribe la cabecera `Authorization` a mano.

### Backend

PharmaSoft (rama `master` del profesor) trae Spring Security + JWT y migraciones Flyway escritas para Oracle. Para trabajar sin Oracle, la rama local `feature/perfil-h2-seguridad` de PharmaSoft agrega el perfil `h2`:

- `src/main/resources/db/migration-h2/`: las migraciones V1–V3 traducidas a H2 (mismas tablas y usuarios) y `V4__datos_de_ejemplo.sql` (categorías, 10 productos, 2 clientes).
- `application-h2.yaml`: Flyway aplica esas migraciones al arrancar y Hibernate solo valida (`ddl-auto: validate`), igual que el perfil `dev`.

```
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

La colección de Postman del curso («PharmaBackend - Pruebas de seguridad») pasa completa contra este perfil: 63 peticiones, 142 aserciones.

### Usuarios de prueba

| Usuario | Contraseña | Rol | Qué ve en la app |
|---|---|---|---|
| `admin` | `Admin123` | ADMIN | Todo: productos (registrar, editar, eliminar), clientes y pedidos |
| `vendedor` | `Vendedor123` | VENDEDOR | Productos solo lectura (sin «Nuevo producto», sin Editar ni Eliminar), clientes y pedidos |
| `almacen` | `Almacen123` | ALMACENERO | Productos con escritura; no se le ofrecen clientes ni pedidos |

### Piezas

| Capa | Archivo | Qué hace |
|---|---|---|
| Dominio | `domain/model/Sesion.kt` | `Credenciales`, `Sesion` (`puede(permiso)`) y los nombres de los permisos |
| Dominio | `domain/model/ErrorSesion.kt` | `CredencialesInvalidas`, `SesionExpirada`, `SesionInvalida`, `SinPermiso`, `SinConexion` |
| Dominio | `domain/repository/AuthRepository.kt` | `iniciarSesion`, `sesionGuardada`, `cerrarSesion` y el aviso `sesionTerminada` |
| Dominio | `domain/usecase/SesionUseCases.kt` | Casos de uso de la sesión |
| Datos | `data/local/AlmacenSeguro.kt` | Interfaz del almacén del token |
| Android | `androidMain/.../data/local/AlmacenSeguroAndroid.kt` | `EncryptedSharedPreferences` con una llave AES-256 del Android Keystore |
| iOS | `iosMain/.../data/local/AlmacenSeguroIos.kt` | Keychain (`SecItemAdd` / `SecItemCopyMatching` / `SecItemDelete`) |
| Datos | `data/remote/HttpClientFactory.kt` | Plugins `Auth` (bearer), `Logging` saneado y `SesionVencida` |
| Datos | `data/remote/ErroresDeSesion.kt` | `mapearErrorSesion`: único lugar que traduce 401/403 a `ErrorSesion` |
| Datos | `data/repository/AuthRepositorioRest.kt` | `POST auth/login`, `GET auth/me` y cierre de sesión |
| Presentación | `presentation/sesion/` | `SesionUiState`, `SesionViewModel` y `LoginScreen` |

`AlmacenSeguro` es una interfaz y no un `expect`: la versión de Android necesita un `Context` en el constructor y la de iOS no recibe nada. Se registra en el `platformModule` de cada plataforma, como el `Compartidor` de la sesión 9.

### Cómo viaja el token

```kotlin
install(Auth) {
    bearer {
        loadTokens { almacen.leerToken()?.let { BearerTokens(it, null) } }
        sendWithoutRequest { peticion -> !peticion.url.encodedPath.endsWith("auth/login") }
    }
}
```

- `loadTokens` se lee una vez y queda en caché: después del login y al cerrar sesión se llama a `authProvider<BearerAuthProvider>()?.clearToken()`.
- `sendWithoutRequest` hace que el token viaje desde la primera petición. Sin él, Ktor envía sin token, recibe 401 y repite: cada llamada se duplica.
- `Logging` registra cabeceras solo en una compilación de depuración (`registroHttp`, que decide cada plataforma) y muestra `Authorization: ***`.

### 401 y 403

- **401 en cualquier petición** (token vencido o inválido): el plugin `SesionVencida` borra el token y avisa; `SesionViewModel` vuelve al login con «Tu sesión expiró. Vuelve a iniciar sesión.». PharmaSoft distingue el motivo por el mensaje («El token ha expirado…» o «El token no es válido»).
- **403** (el rol no tiene el permiso): la sesión sigue y la pantalla muestra «No tienes permiso para realizar esta operación.».
- La interfaz oculta lo que el rol no puede hacer (`sesion.puede("CATALOGO_ESCRIBIR")`, `CLIENTES_LEER`, `VENTAS_LEER`). Quien autoriza es el servidor.
- Para provocar el 403 a propósito (evidencia): con el extra `acciones=todas` en Android o el argumento `-acciones todas` en iOS, solo en depuración, la app muestra las acciones aunque el rol no las tenga.

### La sesión al arrancar

Si hay un token guardado, la app consulta `GET auth/me`: si sigue vigente entra directo (la sesión sobrevive al cierre de la app) y si venció vuelve al login con el aviso. Sin conexión, el token no se descarta.

### Pruebas

- `AuthRepositorioRestTest` (MockEngine): el login guarda el token y no envía `Authorization`; la primera petición después del login ya lleva `Bearer`; un segundo login usa el token nuevo y no el de la caché; un 401 borra el token y avisa; un 403 no; cerrar sesión deja de enviar el token.
- `MapearErrorSesionTest`: los mensajes reales de PharmaSoft.
- `SesionViewModelTest`: arranque con y sin token, validación de campos, credenciales incorrectas, 401 en otra pantalla y cerrar sesión.

### Evidencias sin Mac

El workflow `.github/workflows/ios-evidencias.yml` corre con cada push de la rama: levanta PharmaSoft con el perfil `h2`, repite el paso 1 de la guía en Swagger UI con un navegador real y recorre la app en un emulador de Android y en un simulador de iPhone con Maestro (`.github/ci/maestro/s10-*.yaml`). Para la expiración, reinicia el backend con tokens de 1 minuto y espera a que venzan. Las capturas quedan en las ramas `ci-evidencias-swagger-s10`, `ci-evidencias-android-s10` y `ci-evidencias-ios-s10`.

## Persistencia offline-first (Sesión 11 · rama `feature/offline-first-istana`)

PharmaMobil ya no le pide la lista a la red. Una base local con **SQLDelight** es la única fuente de verdad de la interfaz: la pantalla observa esa base con un `Flow` y la red pasa a ser un proceso aparte cuya única misión es mantenerla al día. Con PharmaSoft apagado o en modo avión la app abre, muestra el inventario guardado y permite registrar productos, que quedan **pendientes** y se envían al reconectar.

### Esquema

`shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/` (el plugin genera `PharmaDb` en `pe.edu.upeu.pharmamobil.db`):

| Archivo | Tabla | Para qué |
|---|---|---|
| `Producto.sq` | `producto (id, nombre, precio, stock, categoria_id, activo, pendiente)` | Copia local del inventario. `pendiente = 1`: creado en el dispositivo y aún no está en el servidor |
| `Perfil.sq` | `perfil (usuario, nombre_completo, permisos, roles)` | Datos de la última sesión validada, para entrar sin servidor (el token sigue en el almacén cifrado) |

Además de las columnas de la guía, `producto` guarda `categoria_id` (PharmaSoft la exige al editar) y `activo` (la lista «De baja» de la sesión 8). Un producto creado sin red recibe un **id local negativo** (`siguienteIdLocal`): el backend nunca usa esos números, así que no puede pisar a uno real.

### Piezas

| Capa | Archivo | Qué hace |
|---|---|---|
| Datos | `data/local/CreadorDeDriver.kt` | Interfaz que abre la base; cada plataforma registra la suya en el `platformModule` |
| Android | `androidMain/.../data/local/CreadorDeDriverAndroid.kt` | `AndroidSqliteDriver` → `databases/pharma.db` |
| iOS | `iosMain/.../data/local/CreadorDeDriverIos.kt` | `NativeSqliteDriver` (`OTHER_LDFLAGS = -lsqlite3` en `Config.xcconfig`, el framework es estático) |
| Pruebas | `commonTest/.../data/local/BaseDePrueba.kt` | `expect fun crearDriverDePrueba()`: JDBC en memoria en la JVM, `inMemoryDriver` en iOS. Una base nueva por prueba |
| Datos | `data/local/ProductoLocal.kt` | Consultas y traducción fila ↔ dominio. No importa nada de Ktor ni de los DTO |
| Datos | `data/local/PerfilLocal.kt` | Guarda y lee el perfil de la última sesión |
| Datos | `data/remote/ProductoRemoto.kt` | Lo que antes era `ProductoRepositorioRest`: ahora solo lo usa la sincronización |
| Datos | `data/repository/ProductoRepositorioOffline.kt` | Implementa el contrato sobre local + remoto |
| Dominio | `domain/repository/ProductoRepository.kt` | `observar(): Flow`, `sincronizar()`, `obtener`, `registrar`, `actualizar`, `eliminar` |
| Dominio | `domain/usecase/ProductosOfflineUseCases.kt` | `ObservarProductosUseCase` y `SincronizarProductosUseCase` (reemplazan a `ListarProductosUseCase`) |

### Sincronizar: primero subir, después bajar

```kotlin
override suspend fun sincronizar() {
    val rechazo = subirPendientes()                  // POST de cada pendiente
    local.reemplazarSincronizados(remoto.listar())   // GET y reemplazo de lo sincronizado
    rechazo?.let { throw it }
}
```

- Si se bajara primero, la lista del servidor (que todavía no tiene el producto creado sin red) lo borraría de la tabla y ya no habría nada que subir.
- `reemplazarSincronizados` borra solo `pendiente = 0` y va dentro de una transacción: el `Flow` emite una vez y la lista no parpadea.
- Cada pendiente se confirma al subirlo (`confirmarSubida`: fila con id negativo → fila con el id real). Si la red se corta a la mitad, lo ya subido no se vuelve a enviar.
- Un pendiente que el servidor rechaza (por ejemplo, nombre duplicado) se queda en el dispositivo, marcado, y no bloquea a los demás ni la bajada.
- Registrar guarda primero en local (aparece de inmediato con la etiqueta **Pendiente**) y luego intenta subir; sin red (`ErrorApi.SinConexion` o `TiempoAgotado`) no falla.
- Editar, dar de baja y reactivar siguen necesitando al servidor (es quien valida duplicados y permisos); su resultado se escribe en la base y el `Flow` actualiza la pantalla.

### La pantalla

`ProductoViewModel` se suscribe una vez a `observar()` en el `init` y llama a `sincronizar()`. Ya no queda ninguna «recarga de la lista» después de registrar, editar, eliminar o reactivar. La barra de sincronización muestra «Inventario al día», «Sincronizando con PharmaSoft…» o «Sin conexión: mostrando datos guardados», cuántos productos esperan para subir y el botón **Sincronizar**.

Al abrir sin servidor, `AuthRepositorioRest.sesionGuardada()` no puede consultar `GET auth/me`: entra con el perfil guardado y, si el token ya no vale, el primer 401 al volver la red cierra la sesión como en la sesión 10.

### Pruebas (commonTest, base en memoria)

- `ProductoRepositorioOfflineTest` (MockEngine + SQLite en memoria): sin red el producto queda pendiente y la pantalla lo ve; con red se sube con el id del servidor; `sincronizar` hace el POST del pendiente **antes** del GET de la lista y no pierde nada; un corte a la mitad no duplica; un 409 no bloquea la bajada; sin red el detalle sale de la copia local.
- `ProductoLocalTest`: ids locales negativos, la lista del servidor no borra lo pendiente, confirmar la subida cambia el id.
- `ProductoViewModelTest`: sin conexión muestra la lista guardada con el aviso; registrar no vuelve a pedir la lista.
- `AuthRepositorioRestTest`: sin servidor se entra con el perfil de la última sesión; cerrar sesión lo borra.
- `AppModuleTest`: la base abre con el driver registrado.

```
./gradlew :shared:testAndroidHostTest
```

### Evidencias

El workflow `.github/workflows/s11-offline.yml` corre con cada push de la rama: levanta PharmaSoft, ejecuta las pruebas y recorre el ciclo offline en un emulador de Android (modo avión con `adb`) y en un simulador de iPhone (PharmaSoft apagado). Copia la base `pharma.db` del emulador en cada paso para mostrar la marca `pendiente` y comprueba en Swagger UI que el producto creado sin red llegó al servidor con su id real. Las evidencias quedan en las ramas `ci-evidencias-android-s11` y `ci-evidencias-ios-s11`.
