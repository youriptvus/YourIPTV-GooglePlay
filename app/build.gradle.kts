import java.util.Properties
import java.io.File

plugins { id("com.android.application") }

android {
    namespace = "com.youriptv.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.youriptv.app"
        // Google Play automatic protection requires API 24 or newer.
        minSdk = 24
        targetSdk = 36
        // Source compatibility fix; published code51 remains unchanged until deliberate publication.
        versionCode = 52
        versionName = "1.1.1"
        testInstrumentationRunner = "com.youriptv.app.PlaySmokeInstrumentation"
    }

    // No implicit dependency on the customer project or locally embedded keys.
    val signingPath = providers.environmentVariable("YOURIPTV_UPLOAD_SIGNING_PROPERTIES").orNull
    val releaseSigning = signingPath?.let { path ->
        val config = file(path).canonicalFile
        require(!config.toPath().startsWith(rootDir.canonicalFile.toPath())) {
            "Signing properties must be outside the repository"
        }
        val props = Properties().apply { config.inputStream().use { load(it) } }
        signingConfigs.create("playUpload") {
            val keyPath = File(props.getProperty("storeFile"))
            storeFile = (if (keyPath.isAbsolute) keyPath else File(config.parentFile, keyPath.path)).canonicalFile
            require(!storeFile!!.toPath().startsWith(rootDir.canonicalFile.toPath())) {
                "Signing key must be outside the repository"
            }
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }
    testBuildType = if (providers.gradleProperty("isolatedDeviceTests").orNull == "true") "isolatedTest" else "release"
    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
        }
        // Device-test package only; there remains exactly one production application.
        create("isolatedTest") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".playtest"
            versionNameSuffix = "-isolated-test"
            matchingFallbacks += listOf("debug")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { buildConfig = true; viewBinding = false }
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    val media3 = "1.11.1"
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    androidTestImplementation("com.google.android.apps.common.testing.accessibility.framework:accessibility-test-framework:4.1.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
    testImplementation("net.sf.kxml:kxml2:2.2.2")
}
