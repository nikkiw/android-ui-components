plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = com.config.Config.NAMESPACE
    compileSdk = com.config.Config.COMPILE_SDK

    defaultConfig {
        minSdk = com.config.Config.MIN_SDK

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    testOptions {
        targetSdk = com.config.Config.TARGET_SDK
        unitTests {
            isIncludeAndroidResources = true
        }
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
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)



    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
//    androidTestImplementation(libs.kaspresso)
}


publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.nikkiw"
            artifactId = "android-ui-components"
            version = "0.0.1"

            // Ensure the component is evaluated before publishing
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
