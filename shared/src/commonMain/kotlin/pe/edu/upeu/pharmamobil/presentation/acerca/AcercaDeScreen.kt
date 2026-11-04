package pe.edu.upeu.pharmamobil.presentation.acerca

import pe.edu.upeu.pharmamobil.presentation.components.LogoPharmaMobil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobil.platform.InfoDispositivo
import pe.edu.upeu.pharmamobil.presentation.components.IconoEnRecuadro
import pe.edu.upeu.pharmamobil.presentation.components.Tarjeta

/**
 * «Acerca de»: muestra en que sistema corre la app y como formatea la moneda.
 * Los datos se calculan una vez al abrir la pantalla; el composable solo
 * pinta el AcercaDeUi que recibe.
 */
@Composable
fun AcercaDeScreen(
    modifier: Modifier = Modifier,
    datos: AcercaDeUi = remember { InfoDispositivo().aUi() },
    // Sesion 13: envia un error de prueba a la telemetria (Firebase Crashlytics en Android).
    onProbarTelemetria: (() -> Unit)? = null
) {

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Tarjeta(modifier = Modifier.fillMaxWidth()) {

            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                LogoPharmaMobil(lado = 56.dp)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

                    Text(
                        text = "PharmaMobil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Una sola app para Android e iOS",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Seccion(titulo = "Dispositivo") {
            Dato(Icons.Default.Smartphone, "Sistema", datos.sistema)
            Separador()
            Dato(Icons.Default.Info, "Versión", datos.version)
            Separador()
            Dato(Icons.Default.Badge, "Modelo", datos.modelo)
        }

        Seccion(titulo = "Formato de moneda") {
            Dato(Icons.Default.Payments, "Monto de prueba (24.5)", datos.precioDeMuestra)
            Separador()
            Dato(Icons.Default.SpaceBar, "Entre S/ y el monto", datos.separador)
            Separador()
            Dato(Icons.Default.SettingsSuggest, "Configuración regional", "es-PE")
        }

        if (onProbarTelemetria != null) {
            var enviado by remember { mutableStateOf(false) }
            Seccion(titulo = "Diagnóstico") {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Los errores de la app se registran en Firebase Crashlytics, sin datos personales ni el token de sesión.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = { onProbarTelemetria(); enviado = true }) {
                        Text("Enviar error de prueba")
                    }
                    if (enviado) {
                        Text(
                            text = "Registrado. Cierra y vuelve a abrir la app para que llegue a la consola.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            text = "Cada plataforma entrega estos datos con su propia API: " +
                "android.os.Build y java.text en Android, UIDevice y Foundation en iOS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}


@Composable
private fun Seccion(
    titulo: String,
    contenido: @Composable () -> Unit
) {

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )

        Tarjeta(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                contenido()
            }
        }
    }
}


@Composable
private fun Separador() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}


@Composable
private fun Dato(
    icono: ImageVector,
    etiqueta: String,
    valor: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

            Text(
                text = etiqueta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = valor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
