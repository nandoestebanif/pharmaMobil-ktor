import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    // Sesion 11: base local con SQLDelight
    alias(libs.plugins.sqldelight)
}

kotlin {
    // Actividad autonoma 9: InfoDispositivo es una clase expect. Las clases
    // expect/actual estan en Beta y sin esta opcion el compilador lo advierte.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    android {
       namespace = "pe.edu.upeu.pharmamobil.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            // Sesion 9: BackHandler para el actual de AlPulsarAtras.
            implementation(libs.androidx.activity.compose)
            // Sesion 10: EncryptedSharedPreferences para el token (AlmacenSeguroAndroid)
            implementation(libs.androidx.security.crypto)
            // api: MainApplication (androidApp) usa androidContext() al arrancar Koin.
            api(libs.koin.android)
            // Sesion 7: motor de red de Android
            implementation(libs.ktor.client.okhttp)
            // Sesion 11: driver de SQLite de Android
            implementation(libs.sqldelight.android)
            // Sesion 13: Firebase Crashlytics con el SDK multiplataforma de GitLive. Va en androidMain
            // y no en commonMain: en iOS el SDK necesita las librerias nativas de Firebase enlazadas
            // desde Xcode (CocoaPods o SPM), y sin una Mac no se pueden agregar ni probar.
            implementation("dev.gitlive:firebase-crashlytics:2.7.0")
            // OkHttp 5.5.0 (transitiva de Ktor) pide compileSdk 37; se fija 5.4.0 (ver libs.versions.toml).
            implementation("com.squareup.okhttp3:okhttp") {
                version { strictly(libs.versions.okhttp.get()) }
            }
        }
        iosMain.dependencies {
            // Sesion 7: motor de red de iOS (NSURLSession)
            implementation(libs.ktor.client.darwin)
            // Sesion 11: driver nativo de SQLite
            implementation(libs.sqldelight.native)
        }
        commonMain.dependencies {
            // api: androidApp llama a initKoin(), cuya firma expone KoinAppDeclaration.
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Sesion 7: Ktor Client (nucleo + plugins) y kotlinx.serialization
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            // Sesion 10: plugin Auth (bearer) para adjuntar el JWT a cada peticion
            implementation(libs.ktor.client.auth)
            // Sesion 11: base local (runtime) y consultas como Flow
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            // Motor falso: prueba el cliente sin red (200, 404, timeout, sin conexion)
            implementation(libs.ktor.client.mock)
        }
        // Sesion 11: en las pruebas de la JVM la base vive en memoria con el driver JDBC.
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.jvm)
        }
    }
}

sqldelight {
    databases {
        create("PharmaDb") {
            packageName.set("pe.edu.upeu.pharmamobil.db")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
tasks.withType<Test>().configureEach {
    testLogging {
        showStandardStreams = true
    }
}