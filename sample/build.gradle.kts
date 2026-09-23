plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.revjet.sdk.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.revjet.sdk.sample"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "2.1.0"
    }

    buildTypes {
        release {
            // R8 runs against the SDK's consumer rules, so the sample shows what publishers ship
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":sdk"))
    implementation(project(":sdk-compose"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)

    // Reports what the SDK holds on to after a screen goes away
    debugImplementation(libs.leakcanary)
}
