package pe.edu.upeu.pharmamobil.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pharmamobil.shared.generated.resources.Res
import pharmamobil.shared.generated.resources.logo_pharmamobil

/**
 * Logo de PharmaMobil sobre un recuadro blanco: se lee igual sobre el
 * degradado verde, en el menu lateral y en el tema oscuro.
 */
@Composable
fun LogoPharmaMobil(
    modifier: Modifier = Modifier,
    lado: Dp = 48.dp,
    sombra: Dp = 0.dp
) {

    Surface(
        modifier = modifier.size(lado),
        shape = RoundedCornerShape(lado * 0.28f),
        color = Color.White,
        shadowElevation = sombra
    ) {

        Image(
            painter = painterResource(Res.drawable.logo_pharmamobil),
            contentDescription = "Logo de PharmaMobil",
            modifier = Modifier.padding(lado * 0.14f)
        )
    }
}
