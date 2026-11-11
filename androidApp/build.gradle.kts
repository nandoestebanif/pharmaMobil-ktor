import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Sesion 13: Firebase lee su configuracion de google-services.json (en esta carpeta). El archivo
// no se sube al repositorio (tiene la clave del proyecto): sin el, la app compila igual y la
// telemetria escribe en el registro del dispositivo.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// Sesion 14: un celular sin cable (sin adb reverse) llega a PharmaSoft por la IP de la PC en la
// red local. Se escribe en local.properties, que tampoco va a Git:
//   pharmasoft.url=http://192.168.1.40:8080/api/v1/
// Vacia = automatico (10.0.2.2 en el emulador, 127.0.0.1 con adb reverse).
val locales = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val urlServidor = (providers.gradleProperty("pharmasoft.url").orNull ?: locales.getProperty("pharmasoft.url") ?: "").trim()
val hostServidor = urlServidor.takeIf { it.isNotEmpty() }?.let { URI(it).host }

// Android bloquea HTTP en claro: la configuracion de red se genera para permitirlo solo hacia los
// hosts de desarrollo y, si se configuro, hacia la IP de la PC. El resto sigue exigiendo HTTPS.
// Se escribe al configurar el proyecto, asi cambiar la IP en local.properties basta.
val redGenerada = layout.projectDirectory.dir("generado/res").asFile
run {
    val hosts = listOfNotNull("10.0.2.2", "localhost", "127.0.0.1", hostServidor).distinct()
    val xml = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("<!-- Generado por androidApp/build.gradle.kts (sesion 14). No editar. -->")
        appendLine("<network-security-config>")
        appendLine("""    <domain-config cleartextTrafficPermitted="true">""")
        hosts.forEach { appendLine("""        <domain includeSubdomains="false">$it</domain>""") }
        appendLine("    </domain-config>")
        appendLine("</network-security-config>")
    }
    val archivo = File(redGenerada, "xml/network_security_config.xml")
    if (!archivo.exists() || archivo.readText() != xml) {
        archivo.parentFile.mkdirs()
        archivo.writeText(xml)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "pe.edu.upeu.pharmamobil"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.edu.upeu.pharmamobil"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        resValue("string", "servidor_url", urlServidor)
    }
    sourceSets["main"].res.srcDir(redGenerada)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}