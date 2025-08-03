plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.epdev.topotrackapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.epdev.topotrackapp"
        minSdk = 30
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.ktor.client.core)//
    implementation(libs.ktor.client.android)//
    implementation(libs.androidx.activity)
    implementation(libs.ktor.client.content.negotiation)//
    implementation(libs.ktor.serialization.kotlinx.json)//
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.play.services.location)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.osmdroid.android)
    implementation(libs.firebase.firestore.ktx)
    implementation("com.google.android.material:material:1.12.0")
    // Supabase (versión actualizada a la más reciente estable)
    implementation("io.github.jan-tennert.supabase:realtime-kt:1.4.1")
    implementation("io.github.jan-tennert.supabase:postgrest-kt:1.4.1")
    implementation("io.github.jan-tennert.supabase:storage-kt:1.4.1")

    // SplashScreen (versión estable más reciente)
    implementation("androidx.core:core-splashscreen:1.0.1")

// Coroutines (versión estable más reciente)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}