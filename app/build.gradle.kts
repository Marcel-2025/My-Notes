plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt") // Room Annotation Processor
}

android {
    namespace = "dev.flondy.mynotes"
    // Google Play verlangt für Updates seit 31.08.2026 targetSdk 36 (Android 16).
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.flondy.mynotes" // NIEMALS ändern – Play-Store-Identität
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Room exportiert jedes Datenbankschema nach app/schemas (Migrationshistorie, Migrationstests).
    kapt {
        arguments { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    buildTypes {
        debug {
            // Offizielle Google-Testanzeigen – während der Entwicklung niemals echte Anzeigen anklicken.
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BANNER_AD_UNIT_ID", "\"ca-app-pub-6130152704553606/6365675325\"")
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
    sourceSets {
        // Exportierte Schemas für MigrationTestHelper (Robolectric liest die Assets der Debug-Variante).
        // Nur Debug – im Release-Build sind die Schemas nicht enthalten.
        getByName("debug").assets.srcDir("$projectDir/schemas")
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
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
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Google Play Billing – Version 8 ist für App-Updates seit 31.08.2026 Pflicht.
    // Kern-Bibliothek ohne -ktx: billing-ktx 8.1+ erzwingt Kotlin-Stdlib 2.2 (inkompatibel mit Kotlin 2.0 + Room-kapt)
    implementation("com.android.billingclient:billing:8.3.0")

    // Google Mobile Ads + User Messaging Platform (DSGVO-Einwilligung)
    implementation("com.google.android.gms:play-services-ads:24.9.0")
    implementation("com.google.android.ump:user-messaging-platform:3.2.0")

    // Room
    val roomVersion = "2.7.2" // 2.6.1 kann die Kotlin-2.1-Metadaten neuer Google-SDKs nicht lesen
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Tests
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit-ktx:1.2.1")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
