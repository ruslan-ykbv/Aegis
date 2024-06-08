plugins {
    alias(libs.plugins.android.application)
}
//configurations.all{
//    resolutionStrategy {
//        force("com.android.support:support-v4:28.0.0")
//    }
//}

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

    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation ("androidx.room:room-runtime:2.6.1")
    implementation(libs.androidx.biometric)
    annotationProcessor ("androidx.room:room-compiler:2.6.1")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
}