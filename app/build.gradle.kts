plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.relojcontrol"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.relojcontrol"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase services (reemplaza servidor PHP)
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation ("com.google.firebase:firebase-auth")           // Para login
    implementation ("com.google.firebase:firebase-database")      // Para datos
    implementation ("com.google.firebase:firebase-storage")       // Para archivos

    // Glide (para cargar imágenes/archivos)
    implementation ("com.github.bumptech.glide:glide:5.0.5")
    annotationProcessor ("com.github.bumptech.glide:compiler:5.0.5")

    // MPAndroidChart para graficos
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Gson para parsear JSON
    implementation ("com.google.code.gson:gson:2.13.2")
}