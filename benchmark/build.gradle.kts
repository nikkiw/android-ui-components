plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.benchmark)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ndev.android.ui.benchmark"
    compileSdk = com.config.Config.COMPILE_SDK

    defaultConfig {
        minSdk = com.config.Config.MIN_SDK

        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] =
            "EMULATOR,LOW-BATTERY"
    }

    testBuildType = "release"
    buildTypes {
        debug {
            // Since isDebuggable can"t be modified by gradle for library modules,
            // it must be done in a manifest - see src/androidTest/AndroidManifest.xml
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "benchmark-proguard-rules.pro"
            )
        }
        release {
            isDefault = true
        }
    }
    compileOptions {
        sourceCompatibility = com.config.Config.COMPILE_JAVA_VERSION
        targetCompatibility = com.config.Config.COMPILE_JAVA_VERSION
    }
    kotlinOptions {
        jvmTarget = com.config.Config.JVM_TARGET
    }
}

dependencies {
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.benchmark.junit4)
//    androidTestImplementation(libs.androidx.benchmark.perfetto)
    // Add your dependencies here. Note that you cannot benchmark code
    // in an app module this way - you will need to move any code you
    // want to benchmark to a library module:
    // https://developer.android.com/studio/projects/android-library#Convert
    androidTestImplementation(project(":library"))

}