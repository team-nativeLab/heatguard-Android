plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.nativelap.heartguard.domain"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 34
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(project(":core"))
}
