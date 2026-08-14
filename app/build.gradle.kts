import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

// PRO TIP: Load signing keys securely from a local properties file (add keystore.properties to .gitignore)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.vahak.mehrban"
    // Standardized AGP format
    compileSdk = 37

    defaultConfig {
        applicationId = "com.vahak.mehrban"
        minSdk = 26
        targetSdk = 37
        versionCode = 19
        versionName = "1.5.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Ensure vector drawables work perfectly on older devices
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // These will read from keystore.properties.
            // If the file doesn't exist (e.g., fresh clone), it falls back safely.
            storeFile = file(keystoreProperties["storeFile"] ?: "dummy.jks")
            storePassword = keystoreProperties["storePassword"] as String? ?: ""
            keyAlias = keystoreProperties["keyAlias"] as String? ?: ""
            keyPassword = keystoreProperties["keyPassword"] as String? ?: ""
        }
    }

    buildTypes {
        debug {
            // Allows installing Debug and Release apps simultaneously on the same device
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
//            buildConfigField("String", "BASE_URL", "\"http://192.168.0.88/\"")
            buildConfigField("String", "BASE_URL", "\"https://mehr-banan.ir/\"")

            // 🚀 Developers get analytics (so you can use DebugView)
            buildConfigField("Boolean", "ENABLE_ANALYTICS", "true")
        }

        release {
            // PRO SETUP: Shrink code and resources for smaller, secure APKs
            isMinifyEnabled = true
            isShrinkResources = true

            // Link to the signing config created above
            signingConfig = signingConfigs.getByName("release")

            buildConfigField("String", "BASE_URL", "\"https://mehr-banan.ir/\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 🚀 Production users get analytics
            buildConfigField("Boolean", "ENABLE_ANALYTICS", "true")
        }

        create("qa") {
            initWith(getByName("release"))
            versionNameSuffix = "-QA"

            // 🚀 QA testers DO NOT get analytics
            buildConfigField("Boolean", "ENABLE_ANALYTICS", "false")
        }
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("website") {
            dimension = "distribution"
            buildConfigField("String", "INSTALL_SOURCE", "\"website\"")
        }
        create("bazaar") {
            dimension = "distribution"
            buildConfigField("String", "INSTALL_SOURCE", "\"bazaar\"")
        }
        create("myket") {
            dimension = "distribution"
            buildConfigField("String", "INSTALL_SOURCE", "\"myket\"")
        }
        create("googleplay") {
            dimension = "distribution"
            buildConfigField("String", "INSTALL_SOURCE", "\"googleplay\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->

            // AGP 8+ exposes version info ONLY on the output object
            val versionName = output.versionName.get()
            val versionCode = output.versionCode.get()
            val flavorName = variant.productFlavors.joinToString("-") { it.second }
            val fileName = when (val buildType = variant.buildType!!) {
                "debug" -> "Mehrban-${flavorName}-Debug-v$versionName($versionCode).apk"
                "release" -> "Mehrban-${flavorName}-Release-v$versionName($versionCode).apk"
                else -> {
                    val cleanBuildType = buildType.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase() else it.toString()
                    }
                    "Mehrban-${flavorName}-${cleanBuildType}-v$versionName($versionCode).apk"
                }
            }

            @Suppress("UnstableApiUsage")
            output.outputFileName.set(fileName)
        }
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    // Retrofit & Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp3.logging.interceptor)

    implementation(libs.play.services.auth.api.phone)

    // --- 🚀 HILT CONFIGURATION ---
    implementation(libs.hilt.android)
    implementation(libs.hilt.work)

    implementation(libs.android.device.names)

    // Logging
    implementation(libs.timber)

    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Firebase products (versions automatically managed by BoM)
    implementation(libs.firebase.analytics)

    implementation(libs.bazaar.updater)


    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}