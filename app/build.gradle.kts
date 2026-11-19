plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// The one place the version lives: platform/release.sh and the release workflow read it from this line.
val appVersion = "0.1.0"

// Every release must be signed with this key or phones refuse the update. CI writes it from the
// CURLEW_KEYSTORE_B64 secret; platform/release.sh checks the signer against platform/signing-cert.sha256.
val releaseKeystore = file(System.getenv("CURLEW_KEYSTORE") ?: "${System.getProperty("user.home")}/.config/curlew/release.keystore")

android {
    namespace = "io.github.vamsikalagaturu.curlew"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.vamsikalagaturu.curlew"
        minSdk = 29
        targetSdk = 36
        val (major, minor, patch) = appVersion.split('.').map(String::toInt)
        versionCode = major * 10_000 + minor * 100 + patch
        versionName = appVersion
    }

    signingConfigs {
        create("release") {
            storeFile = releaseKeystore
            storePassword = "android"
            keyAlias = "curlew"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName(if (releaseKeystore.exists()) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    implementation("androidx.core:core-ktx:1.16.0")
}
