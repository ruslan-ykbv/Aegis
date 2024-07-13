plugins {
    alias(libs.plugins.android.application)
}


android {
    namespace = "com.example.passwordmanagersql"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.passwordmanagersql"
        minSdk = 31
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

    implementation (libs.appcompat)
    implementation (libs.material)
    implementation (libs.constraintlayout)
    implementation (libs.lifecycle.extensions)
    implementation (libs.room.runtime)

    implementation(libs.androidx.biometric)
    implementation(libs.androidx.monitor)
    implementation(libs.ext.junit)
    implementation(libs.androidx.work.runtime)
    annotationProcessor (libs.androidx.room.compiler)
    implementation (libs.recyclerview)

    implementation (libs.bcprov.jdk15on)
}