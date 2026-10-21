plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)         // TAMBAHKAN: membaca google-services.json
}

android {
    namespace = "com.example.catatanbelajar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.catatanbelajar"
        minSdk = 28
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

    buildFeatures {
        viewBinding = true                      // TAMBAHKAN: Binding class untuk semua layout
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.activity.ktx)          // Activity Result API (kamera/galeri)
    implementation(libs.androidx.lifecycle.runtime.ktx) // lifecycleScope
    implementation(libs.androidx.recyclerview)          // daftar catatan
    implementation(libs.androidx.exifinterface)         // memperbaiki rotasi foto kamera

    // --- Firebase: BoM dulu, lalu library TANPA versi ---
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)                  // login/register
    implementation(libs.firebase.firestore)             // database CRUD

    // --- Coroutines: Task.await() untuk Firebase ---
    implementation(libs.kotlinx.coroutines.play.services)
}