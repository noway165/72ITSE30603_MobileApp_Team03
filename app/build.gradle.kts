import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

// Machine-specific settings live in local.properties (not committed to Git):
//   mockapi.baseUrl=https://<your-project-id>.mockapi.io/api/v1/
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val mockApiBaseUrl: String = localProperties.getProperty("mockapi.baseUrl", "").trim()

android {
    namespace = "vn.edu.vlu.financetracker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "vn.edu.vlu.financetracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0-milestone1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "MOCKAPI_BASE_URL", "\"$mockApiBaseUrl\"")
        buildConfigField("String", "EXCHANGE_RATE_BASE_URL", "\"https://open.er-api.com/\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time (LocalDate, YearMonth) on API 24-25
        isCoreLibraryDesugaringEnabled = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Firebase Authentication is enabled once the team leader adds app/google-services.json.
// Without it the app falls back to a local (offline) demo account store.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation(libs.mpandroidchart)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
