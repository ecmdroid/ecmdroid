import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val signingEnabled = keystorePropertiesFile.exists()

android {
    if (signingEnabled) {
        val keystoreProperties = Properties()
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
        signingConfigs {
            create("config") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }
    defaultConfig {
        compileSdk = 34
        applicationId = "org.ecmdroid"
        versionCode = 28
        versionName = "0.99.8"
        minSdk = 26
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingEnabled) {
                signingConfig = signingConfigs.getByName("config")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    namespace = "org.ecmdroid"
    buildFeatures {
        buildConfig = true
    }
    compileSdk = 34
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.drawerlayout)
    implementation(libs.usbserial)
    implementation(libs.documentfile)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
