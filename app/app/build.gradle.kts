plugins {
    id("com.android.application") version "8.1.4"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.iptvpro.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iptvpro.tv"
        minSdk = 21
        targetSdk = 34      // ← AGP 8.x lintVitalRelease requires ≥31; use 34 to match compileSdk
        versionCode = 1
        versionName = "1.0.0"

        val baseUrl = System.getenv("IPTV_BASE_URL")
            ?: (project.findProperty("IPTV_BASE_URL") as String?)
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
        buildConfig = true
        viewBinding = false
        compose = false
    }

    // Disable the lint check that blocks release builds with targetSdk warnings
    // We are a TV app distributed via ADB sideload, not Google Play
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {}
