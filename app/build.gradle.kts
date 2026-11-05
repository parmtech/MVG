plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.parmtech.mvg"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.parmtech.mvg"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    // Optional stable user-owned debug signing for reliable Google OAuth SHA-1 across builds.
    // CI secrets are injected as environment variables; never store a keystore in this repo.
    // Without secrets the standard Android debug key is used, and SHA-1 may change per runner.
    val stableKeystore = System.getenv("MVG_SIGNING_KEYSTORE")
    if (!stableKeystore.isNullOrBlank()) {
        require(file(stableKeystore).isFile) { "Stable signing keystore is missing" }
        fun secret(name: String): String = System.getenv(name)
            ?.takeIf { it.isNotBlank() }
            ?: throw org.gradle.api.GradleException("Stable signing requires GitHub Secret " + name)
        val storePasswordValue = secret("MVG_KEYSTORE_PASSWORD")
        val aliasValue = secret("MVG_KEY_ALIAS")
        val keyPasswordValue = secret("MVG_KEY_PASSWORD")
        signingConfigs {
            create("ciStable") {
                storeFile = file(stableKeystore)
                storePassword = storePasswordValue
                keyAlias = aliasValue
                keyPassword = keyPasswordValue
            }
        }
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("ciStable")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    // Google Identity Services authorization (no embedded service-account keys)
    implementation("com.google.android.gms:play-services-auth:22.0.0")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
