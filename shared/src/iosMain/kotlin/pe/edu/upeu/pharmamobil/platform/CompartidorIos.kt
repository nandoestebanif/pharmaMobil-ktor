package pe.edu.upeu.pharmamobil.platform

import kotlinx.cinterop.ExperimentalForeignApi
import pe.edu.upeu.pharmamobil.domain.platform.Compartidor
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

/** iOS: presenta la hoja de compartir del sistema (UIActivityViewController). */
class CompartidorIos : Compartidor {

    @OptIn(ExperimentalForeignApi::class)
    override fun compartir(texto: String) {

        // Si todavia no hay una ventana activa no hay donde presentar la hoja.
        val origen = controladorVisible() ?: return

        val controlador = UIActivityViewController(
            activityItems = listOf(texto),
            applicationActivities = null
        )

        // En iPad la hoja es un popover y necesita un punto de anclaje.
        controlador.popoverPresentationController?.let { popover ->
            popover.sourceView = origen.view
            popover.sourceRect = origen.view.bounds
        }

        origen.presentViewController(controlador, animated = true, completion = null)
    }

    /**
     * Controlador desde el que se puede presentar: el de la ventana activa y,
     * si ya esta mostrando otro encima, el que queda arriba del todo.
     */
    private fun controladorVisible(): UIViewController? {

        val aplicacion = UIApplication.sharedApplication

        val ventana = aplicacion.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .flatMap { escena -> escena.windows.filterIsInstance<UIWindow>() }
            .firstOrNull { it.keyWindow }
            ?: aplicacion.keyWindow

        var controlador = ventana?.rootViewController
        while (controlador?.presentedViewController != null) {
            controlador = controlador.presentedViewController
        }
        return controlador
    }
}
