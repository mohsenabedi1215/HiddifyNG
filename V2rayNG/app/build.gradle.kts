plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.v2ray.ang"
    compileSdk = 34

    defaultConfig {
            applicationId "ang.hiddify.com"//hiddify
        minSdk = 21
        targetSdk = 34
        versionCode = 550800
        versionName = "8.0.0"
        multiDexEnabled = true
    }
signingConfigs {
        release {
            def properties = new Properties()
            properties.load(new FileInputStream(rootProject.file('release.properties')))

            keyAlias properties['key.alias']
            keyPassword properties['key.password']
            storeFile file(properties['keystore.path'])
            storePassword properties['keystore.password']
//            storeFile file("signing.keystore")
//            def properties = new Properties()
//            properties.load(new FileInputStream(rootProject.file('signing.properties')))
//            storePassword System.getenv("SIGNING_STORE_PASSWORD")
//            keyAlias System.getenv("SIGNING_KEY_ALIAS")
//            keyPassword System.getenv("SIGNING_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled false
            zipAlignEnabled false
            shrinkResources false
            ndk.abiFilters 'x86', 'x86_64', 'armeabi-v7a', 'arm64-v8a'
            signingConfig signingConfigs.release
        }
        debug {
            isMinifyEnabled false
            zipAlignEnabled false
            shrinkResources false
            ndk.abiFilters 'x86', 'x86_64', 'armeabi-v7a', 'arm64-v8a'
//            applicationIdSuffix '.debug'
//            android.defaultConfig.applicationId "ang.hiddify.com.debug"
            versionNameSuffix 'd'
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
        }
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

     splits {
        language {
            enable = false
        }
        abi {
            enable true
            reset()
            include 'x86', 'x86_64', 'armeabi-v7a', 'arm64-v8a' //select ABIs to build APKs for
            universalApk true //generate an additional APK that contains all the ABIs
        }
    }
    bundle {
        language {
            enableSplit = false
        }
        abi {
            enableSplit = true
        }
    }

    applicationVariants.all {
        val variant = this
        val versionCodes =
            mapOf("armeabi-v7a" to 1, "arm64-v8a" to 2, "x86" to 3, "x86_64" to 4)

        variant.outputs
            .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
            .forEach { output ->
                val abi = if (output.getFilter("ABI") != null)
                    output.getFilter("ABI")
                else
                    "all"

                if(output.getFilter(com.android.build.OutputFile.ABI)==null)
                    output.outputFileName = "HiddifyNG.apk"
                else
                    output.outputFileName = "HiddifyNG_" + output.getFilter(com.android.build.OutputFile.ABI) + ".apk"
        
            }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar","*.jar"))))
    testImplementation("junit:junit:4.13.2")
    implementation 'androidx.coordinatorlayout:coordinatorlayout:1.2.0'

    // Androidx
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta02")

    // Androidx ktx
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    //kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.23")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    implementation("com.tencent:mmkv-static:1.3.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.reactivex:rxjava:1.3.8")
    implementation("io.reactivex:rxandroid:1.2.1")
    implementation("com.tbruyelle.rxpermissions:rxpermissions:0.9.4@aar")
    implementation("com.github.jorgecastilloprz:fabprogresscircle:1.01@aar")
    implementation("me.drakeet.support:toastcompat:1.1.0")
    implementation("com.blacksquircle.ui:editorkit:2.9.0")
    implementation("com.blacksquircle.ui:language-base:2.9.0")
    implementation("com.blacksquircle.ui:language-json:2.9.0")
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.9.0")
    implementation("com.google.zxing:core:3.5.3")

    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.work:work-multiprocess:2.8.1")
    //Hiddify
    implementation fileTree(dir: 'jniLibs', include: ['*.so'], exclude: [ ])

    // Import the BoM for the Firebase platform
    implementation platform('com.google.firebase:firebase-bom:31.5.0')

    // Add the dependencies for the Crashlytics and Analytics libraries
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation 'com.google.firebase:firebase-crashlytics-ktx'
    implementation 'com.google.firebase:firebase-analytics-ktx'
    implementation 'com.google.firebase:firebase-inappmessaging-display-ktx'
    implementation 'com.google.firebase:firebase-messaging-ktx'
    implementation 'androidx.work:work-runtime:2.8.1'
    implementation 'com.google.android.play:core-ktx:1.8.1'
//    implementation 'com.google.android.play:app-update-ktx:2.0.1'
//    implementation 'fr.bmartel:jspeedtest:1.32.1'
//    implementation 'com.github.hiddify:Super-Gauge-View:14314adde9'
//     https://mvnrepository.com/artifact/org.achartengine/achartengine
//    implementation("org.achartengine:achartengine:1.2.0")
    implementation files('libs/achartengine-1.2.0.jar')

    implementation 'org.conscrypt:conscrypt-android:2.5.2'
//    implementation("com.squareup.okhttp3:okhttp:5.0.+")
//    implementation platform("org.http4k:http4k-bom:4.44.0.0"){
//        exclude(group: 'group.name', module: 'META-INF/DEPENDENCIES')
//
//    }
//
//
//    // Java (for development only):
//    implementation("org.http4k:http4k-core")
//
//    // Apache v5 (Sync):
//    implementation("org.http4k:http4k-client-apache")
//
//
////    // Apache v4 (Sync):
////    implementation("org.http4k:http4k-client-apache4"){
////        exclude group: 'mozilla', module: 'public-suffix-list'
////    }
//
//    // Apache v5 (Async):
////    implementation("org.http4k:http4k-client-apache-async")
//
//    // Apache v4 (Async):
////    implementation("org.http4k:http4k-client-apache4-async")
//
//    // Fuel (Sync + Async):
//    implementation("org.http4k:http4k-client-fuel")
//
//    // OkHttp (Sync + Async):
//    implementation("org.http4k:http4k-client-okhttp")

//    // Websocket:
//    implementation("org.http4k:http4k-client-websocket")






//    implementation("com.github.jkcclemens:khttp:0.1.0")

//    implementation("org.symphonyoss.symphony:jcurl:0.9.18")

    implementation 'com.github.tapadoo:alerter:7.2.4'
//    implementation 'com.github.Yalantis:Context-Menu.Android:1.1.4'
//    implementation 'com.github.dmytrodanylyk:circular-progress-button:1.3'
//    compile 'com.github.dmytrodanylyk:android-morphing-button:1.0'
    implementation 'com.github.anastr:speedviewlib:1.6.0'
    implementation("com.mikepenz:materialdrawer:9.0.1")
    implementation 'androidx.core:core-splashscreen:1.0.0+'





}
apply plugin: 'com.google.gms.google-services'


