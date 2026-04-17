import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.aipoweredgita.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aipoweredgita.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.2.0"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    // kotlinOptions and compilerOptions moved to tasks.withType below
    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

tasks.withType<Test>().configureEach {
    systemProperty("robolectric.sqliteMode", "LEGACY") // Revert to LEGACY since NATIVE failed
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")

    // Moshi for JSON parsing
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // Gson as alternative (more forgiving with JSON)
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room Database
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.material3:material3:1.2.0")


    // Material3 Window Size Class for responsive layouts
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1") {
        exclude(group = "androidx.annotation", module = "annotation-experimental")
    }

    // Removed third-party calendar; using Google Compose components only

    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.work.runtime.ktx)
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Test dependencies
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")


    // OkHttp for resume-capable model downloads
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // TensorFlow Lite runtime for on-device ML inference
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    // LiteRT-LM for chat-style Gemma inference (replaces MediaPipe tasks-genai)
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.10.0")
    // Compose LiveData runtime for observeAsState on LiveData
    implementation("androidx.compose.runtime:runtime-livedata:1.7.5")

    // Fix for InvalidFragmentVersionForActivityResult lint error
    implementation("androidx.fragment:fragment-ktx:1.8.5")



}
