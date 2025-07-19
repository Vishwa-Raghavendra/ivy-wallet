import com.ivy.wallet.buildsrc.Project
import com.ivy.wallet.buildsrc.ivyDesignModuleDependencies

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-android")
//    id("kotlin-kapt")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ivy.design"
    compileSdk = Project.compileSdkVersion

    defaultConfig {
        minSdk = Project.minSdk
        targetSdk = Project.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true

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
    }

    buildFeatures {
        compose = true
    }

//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.15"
//    }

    packagingOptions {
        //Exclude this files so Jetpack Compose UI tests can build
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
        //-------------------------------------------------------
    }

}

dependencies {
    val vico = "2.1.3"
    ivyDesignModuleDependencies()

    //Charts
    implementation("com.patrykandpatrick.vico:compose:$vico")
}