plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.revjet.sdk"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

dependencies {
    // `api` for what the public API exposes: StateFlow, and Insets in the mount presets
    api(libs.coroutines.android)
    api(libs.androidx.core.ktx)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.play.services.ads.identifier)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    // The android.jar stub returns defaults for org.json, so unit tests need a real implementation
    testImplementation(libs.json)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.coroutines.test)
}

android {
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.revjet"
                artifactId = "revjet-sdk"
                version = providers.gradleProperty("revjetVersion").get()

                pom {
                    name.set("RevJet SDK")
                    description.set("Renders RevJet native and JavaScript ad tags.")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                }
            }
        }
    }
}
