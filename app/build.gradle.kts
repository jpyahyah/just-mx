plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.justmx.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.justmx.player"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "OPENSUBTITLES_API_KEY", "\"${project.findProperty("OPENSUBTITLES_API_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-common:1.8.0")
    implementation("androidx.media3:media3-datasource-okhttp:1.8.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.8.0-0.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.json:json:20231013")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
}