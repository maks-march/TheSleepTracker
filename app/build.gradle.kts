plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.sleeptracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sleeptracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"

        // Ссылки на проект и прямую загрузку APK — используются в «Поделиться» и настройках
        buildConfigField(
            "String",
            "GITHUB_URL",
            "\"https://github.com/maks-march/TheSleepTracker\"",
        )
        buildConfigField(
            "String",
            "APK_URL",
            "\"https://github.com/maks-march/TheSleepTracker/raw/main/apk/TheSleepTracker.apk\"",
        )
        buildConfigField(
            "String",
            "VERSION_URL",
            "\"https://raw.githubusercontent.com/maks-march/TheSleepTracker/main/version.json\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // подписываем debug-ключом, чтобы APK из репозитория ставился без настройки keystore
            signingConfig = signingConfigs.getByName("debug")
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
        buildConfig = true
    }

    lint {
        // lintVital на слабых машинах съедает всю память и валит release-сборку
        checkReleaseBuilds = false
        abortOnError = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.vico.compose.m3)

    debugImplementation(libs.androidx.ui.tooling)
}
