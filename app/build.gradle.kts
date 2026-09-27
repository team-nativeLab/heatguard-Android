import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val releaseBaseUrl = providers.gradleProperty("HEARTGUARD_RELEASE_BASE_URL").orElse("").get()
val debugBaseUrl = providers.gradleProperty("HEARTGUARD_DEBUG_BASE_URL")
    .orElse("https://api.heartguard.example.com/")
    .get()

fun String.asBuildConfigString(): String = "\"${
    replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}\""

val validateReleaseServerConfiguration = tasks.register("validateReleaseServerConfiguration") {
    group = "verification"
    description = "Checks that release builds target a configured HTTPS HeartGuard server."
    doLast {
        val parsedBaseUrl = runCatching { URI(releaseBaseUrl) }.getOrNull()
        val host = parsedBaseUrl?.host.orEmpty()
        val normalizedHost = host.trimEnd('.').lowercase()
        val isPlaceholderHost = normalizedHost in setOf(
            "example.com",
            "example.net",
            "example.org",
            "localhost",
        ) || listOf(
            ".example.com",
            ".example.net",
            ".example.org",
            ".example",
            ".invalid",
            ".test",
            ".localhost",
        ).any { suffix -> normalizedHost.endsWith(suffix) }
        val isValidReleaseBaseUrl = parsedBaseUrl != null &&
            parsedBaseUrl.scheme == "https" &&
            normalizedHost.isNotBlank() &&
            !isPlaceholderHost &&
            parsedBaseUrl.rawUserInfo == null &&
            parsedBaseUrl.rawQuery == null &&
            parsedBaseUrl.rawFragment == null &&
            releaseBaseUrl.endsWith("/")
        require(
            isValidReleaseBaseUrl,
        ) {
            "Release build requires a real HTTPS server URL ending in /; set " +
                "HEARTGUARD_RELEASE_BASE_URL in your Gradle user properties."
        }
    }
}

android {
    namespace = "com.nativelap.heartguard"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nativelap.heartguard"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", debugBaseUrl.asBuildConfigString())
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", releaseBaseUrl.asBuildConfigString())
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.matching { task ->
    task.name == "preReleaseBuild" ||
        task.name == "assembleRelease" ||
        task.name == "bundleRelease"
}.configureEach {
    dependsOn(validateReleaseServerConfiguration)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
