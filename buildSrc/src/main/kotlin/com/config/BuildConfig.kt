package com.config

import org.gradle.api.JavaVersion


object Config {
    // SDK
    const val COMPILE_SDK = 35
    const val MIN_SDK = 23
    const val TARGET_SDK = 35
    const val APPLICATION_ID = "com.ndev.android.ui.components"
    const val NAMESPACE = "com.ndev.android.ui.components"

    val COMPILE_JAVA_VERSION = JavaVersion.VERSION_17
    const val JVM_TARGET = "17"
}

