plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // NEU: Für Room Annotation Processor
}

android {
    namespace = "dev.flondy.mynotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.flondy.mynotes"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.2"

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
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("com.google.android.gms:play-services-ads:23.1.0")
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8")
    implementation("androidx.datastore:datastore-preferences:1.1.1") // Oder die neueste Version
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2") // Für lifecycleScope
    implementation("androidx.compose.animation:animation")

    // Google Play Billing (für In-App Käufe)
    implementation("com.android.billingclient:billing-ktx:6.1.0")

    // Google Mobile Ads (für die Werbung)
    implementation("com.google.android.gms:play-services-ads:23.0.0")

    // Room Dependencies (NEU)
    val room_version = "2.6.1" // Überprüfe die neueste stabile Version
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version") // Annotation processor
    implementation("androidx.room:room-ktx:$room_version") // Kotlin Extensions and Coroutines support for Room
}