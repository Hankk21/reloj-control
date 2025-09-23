plugins {
    alias(libs.plugins.android.application)
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
    // Volley para solicitudes HTTP
    implementation("com.android.volley:volley:1.2.1")

    // Glide (opcional, para cargar imágenes/archivos)
    implementation ("com.github.bumptech.glide:glide:5.0.5")
    annotationProcessor ("com.github.bumptech.glide:compiler:5.0.5")

    // Gson para parsear JSON
    implementation ("com.google.code.gson:gson:2.13.2")
}