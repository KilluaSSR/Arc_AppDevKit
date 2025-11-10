plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

android {
    namespace = "killua.dev.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.9"
        apiVersion = "1.9"
    }
    buildFeatures {
        compose = true
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "killua.dev"
                artifactId = "arc_appdevkit"
                version = "1.1"

                from(components.getByName("release"))
            }
        }

        repositories{
            mavenLocal()
        }
    }
}

dependencies {
    api(libs.androidx.startup.runtime)
    api(libs.androidx.work.runtime.ktx)
    api(libs.androidx.work.multiprocess)
    api(libs.logcat)

    api(libs.hilt.android)
    api(libs.androidx.hilt.common)
    api(libs.androidx.hilt.navigation.compose)
    api(libs.androidx.hilt.work)
    
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.compiler)
    
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.activity.compose)
    
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    api(libs.androidx.navigation.compose)
    api(libs.androidx.material.icons.extended)
    api(libs.coil.compose)
    
    api(libs.ktor.client.core)
    api(libs.ktor.client.android)
    api(libs.ktor.client.logging)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.ktor.client.websockets)
    
    api(libs.gson)
    api(libs.kotlinx.serialization.json)
    api(libs.androidx.datastore.preferences)
    api(libs.kotlinx.coroutines.core)
    
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.ui.tooling.preview)
    api(libs.androidx.material3)
    api(libs.material)
    api(libs.androidx.material3.android)

}