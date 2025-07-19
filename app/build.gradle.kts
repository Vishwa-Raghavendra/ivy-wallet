import com.ivy.wallet.buildsrc.Firebase
import com.ivy.wallet.buildsrc.Google

import com.ivy.wallet.buildsrc.Project
import com.ivy.wallet.buildsrc.appModuleDependencies

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    compileSdk = Project.compileSdkVersion

    defaultConfig {
        applicationId = Project.applicationId
        minSdk = Project.minSdk
        targetSdk = Project.targetSdk
        versionCode = Project.versionCode
        versionName = Project.versionName

        //testInstrumentationRunner = "com.ivy.wallet.HiltTestRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../debug.jks")
            storePassword = "IVY7834!DEbug"
            keyAlias = "debug"
            keyPassword = "IVY7834!DEbug"
        }

        create("release") {
            storeFile = file("../sign.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isDefault = false

            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            resValue("string", "app_name", "Ivy Wallet")
        }

        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isDefault = true

            signingConfig = signingConfigs.getByName("debug")

            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "Ivy Wallet Debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    hilt {
        enableAggregatingTask = false
    }

    buildFeatures {
        compose = true
    }

//    java {
//        toolchain {
//            languageVersion = JavaLanguageVersion.of(8)
//        }
//    }

//    composeOptions {
//        kotlinCompilerExtensionVersion = com.ivy.wallet.buildsrc.GlobalVersions.compose
//    }

    lint {
//        isCheckReleaseBuilds = true
//        isAbortOnError = false
    }

    packaging {
        //Exclude this files so Jetpack Compose UI tests can build
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
        //-------------------------------------------------------
    }

    testOptions {
        unitTests.all {
            //Required by Kotest
            it.useJUnitPlatform()
        }
    }
    namespace = "com.ivy.wallet"
}

dependencies {
    val roomVersion = "2.7.1"
    val hiltVersion = "2.56.1"
    val hiltWorkVersion = "1.2.0"
    val accompanistVersion = "0.15.0"
    val lifecycleVersion = "2.8.7"
    val coroutinesVersion = "1.10.2"
    val retrofitVersion = "2.9.0"
    val workVersion = "2.10.1"
    appModuleDependencies()

    //Ivy
    implementation(project(":ivy-design"))
    implementation("com.github.ILIYANGERMANOV:ivy-frp:0.9.5")

    //HILT
    ksp("com.google.dagger:hilt-android-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-work:$hiltWorkVersion")
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("androidx.test:runner:1.6.2")


    //ROOM
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")


    //COMPOSE: https://developer.android.com/jetpack/androidx/releases/compose
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation ("androidx.compose.ui:ui-tooling-preview")

    //URL: https://developer.android.com/jetpack/androidx/releases/activity
    implementation("androidx.activity:activity-compose:1.10.1")

    //URL: https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Jetpack Glance (Compose Widgets)
    implementation ("androidx.glance:glance-appwidget:1.0.0-alpha03")

    //Accompanist https://github.com/google/accompanist
    implementation("com.google.accompanist:accompanist-coil:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-insets:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.24.4-alpha")
    implementation("com.google.accompanist:accompanist-flowlayout:0.24.4-alpha")


    //LifeCycle
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")


    //Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutinesVersion")


    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-gson:$retrofitVersion")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    //AndroidX
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.work:work-runtime-ktx:$workVersion")
    implementation("androidx.work:work-testing:$workVersion")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.webkit:webkit:1.13.0")

    //COIL
    implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")

    //DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    //Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")

    //Arrow
    implementation(platform("io.arrow-kt:arrow-stack:1.0.1"))
    implementation("io.arrow-kt:arrow-core")
    implementation("io.arrow-kt:arrow-fx-coroutines")
    implementation("io.arrow-kt:arrow-fx-stm")

    Google()
    Firebase()


    //Testing
    implementation("androidx.compose.ui:ui-test-junit4:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling:1.1.1")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("com.google.dagger:hilt-android-testing:$hiltVersion")
    androidTestImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:$1.1.1") // Needed for createComposeRule, but not createAndroidComposeRule:

    //KoTest
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2") //junit5 is required!
    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("io.kotest:kotest-assertions-core:5.1.0")
    testImplementation("io.kotest:kotest-property:5.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0") //otherwise Kotest doesn't work...
    testImplementation("io.kotest.extensions:kotest-property-arrow:1.0.1")


    /** Third party */
    //URL: https://github.com/airbnb/lottie-android
    implementation("com.airbnb.android:lottie:3.7.0")

    //URL: https://github.com/jeziellago/compose-markdown
    implementation("com.github.jeziellago:compose-markdown:0.2.6")

    //URL: https://github.com/JakeWharton/timber/releases
    implementation("com.jakewharton.timber:timber:4.7.1")

    //URL: https://github.com/greenrobot/EventBus/releases
    implementation("org.greenrobot:eventbus:3.2.0")

    //URL: https://github.com/notKamui/Keval - evaluate math expressions (calculator)
    implementation("com.notkamui.libs:keval:0.8.0")

    implementation("com.opencsv:opencsv:5.5")
    implementation("org.apache.commons:commons-lang3:3.12.0")
}
