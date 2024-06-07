plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.passwordmanagersql"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.passwordmanagersql"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation (libs.recyclerview)
    implementation (libs.material)
//    implementation (libs.lifecycle.extensions)
//    implementation (libs.lifecycle.viewmodel.ktx)
    implementation (libs.android.database.sqlcipher)
    implementation (libs.room.runtime)
    annotationProcessor (libs.androidx.room.compiler)
    implementation (libs.androidx.biometric)
}