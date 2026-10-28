import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.lumi.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.lumi.launcher"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // One shared key for every build, so updates always install over each other.
    // The key and its passwords live OUTSIDE GitHub (see keystore.properties.example).
    val keyProps = Properties()
    file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { keyProps.load(it) }
    val hasKey = keyProps.getProperty("storeFile") != null && file(keyProps.getProperty("storeFile")).exists()
    if (!hasKey) {
        logger.warn("WARNING: app/keystore.properties or the keystore is missing. This build won't install over Mom's copy of Lumi.")
    }

    signingConfigs {
        create("lumi") {
            if (hasKey) {
                storeFile = file(keyProps.getProperty("storeFile"))
                storePassword = keyProps.getProperty("storePassword")
                keyAlias = keyProps.getProperty("keyAlias")
                keyPassword = keyProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            if (hasKey) signingConfig = signingConfigs.getByName("lumi")
        }
        release {
            isMinifyEnabled = false
            if (hasKey) signingConfig = signingConfigs.getByName("lumi")
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
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Screenshot previews only render when asked (LUMI_SCREENSHOTS=1), never in normal checks.
            it.systemProperty("roborazzi.test.record", "true")
            it.systemProperty("roborazzi.output.dir", rootProject.file("screenshots").absolutePath)
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.32.2")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.32.2")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
