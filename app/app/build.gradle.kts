plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.iptvpro.tv"
    compileSdk = 34  // Always compile against latest SDK

    defaultConfig {
        applicationId = "com.iptvpro.tv"
        minSdk = 19
        targetSdk = 28  // Target TV-compatible API
        versionCode = 1
        versionName = "1.0.0"

        // Inject BASE_URL from build environment (env var or local.properties)
        val baseUrl = System.getenv("IPTV_BASE_URL") 
            ?: project.findProperty("IPTV_BASE_URL") as String?
            ?: "http://192.168.9.158:5950"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug signing for CI if no keystore provided
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true   // Enable BuildConfig generation
        viewBinding = false
        compose = false
    }
}

// 零第三方依赖 - 纯系统API
dependencies {
    // Empty - using Android system APIs only
}
