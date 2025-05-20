plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = com.config.Config.NAMESPACE
    compileSdk = com.config.Config.COMPILE_SDK

    defaultConfig {
        minSdk = com.config.Config.MIN_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = com.config.Config.COMPILE_JAVA_VERSION
        targetCompatibility = com.config.Config.COMPILE_JAVA_VERSION
    }
    kotlinOptions {
        jvmTarget = com.config.Config.JVM_TARGET
    }

    sourceSets {
        // Get the androidTest section and supplement it with the list of resource folders
        getByName("androidTest") {
            // add the src/main/res folder to the existing test resources
            res.srcDirs(
                "src/androidTest/res",   // your test resources (if any)
                "src/main/res"           // add basic resources
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)


    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
}