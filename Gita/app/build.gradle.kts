import org.gradle.kotlin.dsl.implementation
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.aipoweredgita.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aipoweredgita.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "2.0.3"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COIN_API_BASE_URL", "\"https://prime-gorilla-49.sravanku018.deno.net/\"")
        buildConfigField("String", "VOICE_PROXY_URL", "\"https://noisy-sheep-76.sravanku018.deno.net/\"")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val localP12 = File("/home/sravan/Downloads/16-10-2025/upload-key.p12")
            val rootP12 = rootProject.file("upload-key.p12")
            val releaseKeystoreFile = rootProject.file("release.keystore")

            if (keystorePropertiesFile.exists()) {
                val properties = Properties()
                properties.load(FileInputStream(keystorePropertiesFile))
                val keyStorePath = properties.getProperty("storeFile") ?: "upload-key.p12"
                val resolvedStoreFile = rootProject.file(keyStorePath)
                if (resolvedStoreFile.exists()) {
                    storeFile = resolvedStoreFile
                    storePassword = properties.getProperty("storePassword") ?: "password123"
                    keyAlias = properties.getProperty("keyAlias") ?: "upload"
                    keyPassword = properties.getProperty("keyPassword") ?: "password123"
                }
            } else if (System.getenv("KEYSTORE_FILE") != null && File(System.getenv("KEYSTORE_FILE")).exists()) {
                storeFile = File(System.getenv("KEYSTORE_FILE"))
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "password123"
            } else if (rootP12.exists()) {
                storeFile = rootP12
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "password123"
            } else if (localP12.exists()) {
                storeFile = localP12
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "password123"
            } else if (releaseKeystoreFile.exists()) {
                storeFile = releaseKeystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password123"
                keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "password123"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true) {
                signingConfig = releaseSigning
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
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
        buildConfig = true
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

    // Google ML Kit Translation for On-Device local translation
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Fix for InvalidFragmentVersionForActivityResult lint error
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    // EncryptedSharedPreferences for secure credential storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Kotlin Serialization for type-safe navigation
    implementation(libs.kotlinx.serialization.json)

}
