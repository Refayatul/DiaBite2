import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.google.services)
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.example.diabite"
    compileSdk = 36

    signingConfigs {
        getByName("debug") {
            // Optional: Configure debug signing if needed
        }
    }

    defaultConfig {

        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY")
        if (geminiApiKey.isNullOrBlank()) {
            throw GradleException("GEMINI_API_KEY not found or is empty in local.properties.")
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true // Fixes native library stripping warnings
        }
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // Suppress warnings about specific native libraries
        resources.excludes += "**/libandroidx.graphics.path.so"
        resources.excludes += "**/libdatastore_shared_counter.so"
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.graphics)
    implementation(libs.compose.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.material.icons.extended)

    // Compose dependencies
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)

    // Coil for Compose
    implementation(libs.coil.compose)

    // Gson
    implementation(libs.gson)

    // Gemini AI - FIXED: Use version catalog instead of hardcoded dependency
    implementation(libs.gemini.ai)

    // Hilt
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Timber
    implementation(libs.timber)

    // Firebase - FIXED: Use correct dependencies with BoM
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics) // Added missing analytics
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)

    // Google Play Services
    implementation(libs.play.services.auth)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
}
