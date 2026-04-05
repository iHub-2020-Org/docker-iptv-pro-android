plugins {
    id("com.android.application") version "8.1.4"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.iptvpro.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.iptvpro.tv"
        minSdk = 21          // AGP 8.x 最低支持 API 21 (Android 5.0)
        targetSdk = 28       // 保持 TV 兼容性
        versionCode = 1
        versionName = "1.0.0"

        // Inject BASE_URL at build time via env var, local.properties, or fallback
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
            // Auto debug-sign for CI; replace with real keystore for production
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
}

// Zero third-party dependencies - pure Android system APIs
dependencies {}
