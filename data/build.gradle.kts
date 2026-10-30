import java.net.URI

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val releaseBaseUrl =
    providers
        .gradleProperty("HEARTGUARD_RELEASE_BASE_URL")
        .orElse("https://d2f845gr2v9cs2.cloudfront.net/")
        .get()
val debugBaseUrl =
    providers
        .gradleProperty("HEARTGUARD_DEBUG_BASE_URL")
        .orElse("https://d2f845gr2v9cs2.cloudfront.net/")
        .get()

fun String.asBuildConfigString(): String =
    "\"${
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }\""

val validateReleaseServerConfiguration =
    tasks.register("validateReleaseServerConfiguration") {
        group = "verification"
        description = "Checks that release builds target a configured HTTPS HeartGuard server."
        doLast {
            val parsedBaseUrl = runCatching { URI(releaseBaseUrl) }.getOrNull()
            val host = parsedBaseUrl?.host.orEmpty()
            val normalizedHost = host.trimEnd('.').lowercase()
            val isPlaceholderHost =
                normalizedHost in
                    setOf(
                        "example.com",
                        "example.net",
                        "example.org",
                        "localhost",
                    ) ||
                    listOf(
                        ".example.com",
                        ".example.net",
                        ".example.org",
                        ".example",
                        ".invalid",
                        ".test",
                        ".localhost",
                    ).any { suffix -> normalizedHost.endsWith(suffix) }
            val isValidReleaseBaseUrl =
                parsedBaseUrl != null &&
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
                "Release build requires a valid HTTPS server URL ending in /."
            }
        }
    }

android {
    namespace = "com.nativelap.heartguard.data"
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
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", debugBaseUrl.asBuildConfigString())
        }
        release {
            buildConfigField("String", "BASE_URL", releaseBaseUrl.asBuildConfigString())
        }
    }
}

tasks
    .matching { task ->
        task.name == "preReleaseBuild" ||
            task.name == "assembleRelease" ||
            task.name == "bundleRelease"
    }.configureEach {
        dependsOn(validateReleaseServerConfiguration)
    }

dependencies {
    implementation(libs.hilt.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(project(":core"))
    implementation(project(":domain"))
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    testImplementation(libs.kotlin.test.junit)
}
