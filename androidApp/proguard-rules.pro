# Sesion 14: reglas de R8 para la version de entrega (isMinifyEnabled = true).
#
# kotlinx.serialization, Ktor, Koin y SQLDelight traen sus propias reglas dentro de sus
# librerias (consumer rules) y la version minificada recorre el flujo completo sin cambios
# (workflow S14 Entrega). Aqui va solo lo que esas reglas no cubren.

# Crashlytics: numeros de linea legibles en los reportes; el nombre del archivo se oculta.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Excepciones propias que se registran en la telemetria con su nombre de clase.
-keepnames class pe.edu.upeu.pharmamobil.** extends java.lang.Throwable

# Modelos de transporte (DTO): los serializadores generados se conservan con la clase.
-keepclassmembers @kotlinx.serialization.Serializable class pe.edu.upeu.pharmamobil.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
