import java.util.Properties
import java.io.File

plugins { id("com.android.application") }

val sharedVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val distributionDefaults = Properties().apply {
    rootProject.file("distribution.properties").inputStream().use { load(it) }
}
val distribution = providers.gradleProperty("distribution")
    .orElse(distributionDefaults.getProperty("distribution", "play")).get()
require(distribution in setOf("play", "customer")) { "distribution must be play or customer" }
val isCustomerDistribution = distribution == "customer"
val sharedVersionName = requireNotNull(sharedVersion.getProperty("versionName"))
val sharedVersionCode = requireNotNull(sharedVersion.getProperty("versionCode").toIntOrNull())
require(sharedVersionName.matches(Regex("[0-9A-Za-z][0-9A-Za-z.+-]{0,31}")) && sharedVersionCode > 0) {
    "Invalid shared production version"
}
// Keep the older release outputs intact and prevent one distribution's task results replacing the other.
layout.buildDirectory.set(layout.projectDirectory.dir("build/$distribution"))

android {
    namespace = "com.youriptv.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.youriptv.app"
        // Google Play automatic protection requires API24; the customer package retains API23.
        minSdk = if (isCustomerDistribution) 23 else 24
        targetSdk = 36
        versionCode = if (isCustomerDistribution) {
            providers.gradleProperty("updateTestVersionCode").orNull?.toInt() ?: sharedVersionCode
        } else sharedVersionCode
        versionName = if (isCustomerDistribution) {
            providers.gradleProperty("updateTestVersionName").orNull ?: sharedVersionName
        } else sharedVersionName
        buildConfigField("boolean", "IS_CUSTOMER_DISTRIBUTION", isCustomerDistribution.toString())
        if (isCustomerDistribution) {
            val updateTestOrigin = providers.gradleProperty("updateTestOrigin").orNull.orEmpty()
            val updateTestBackupOrigin = providers.gradleProperty("updateTestBackupOrigin").orNull.orEmpty()
            val updateManifest = if (updateTestOrigin.isEmpty()) {
                "https://github.com/youriptvus/YourIPTV/releases/latest/download/update.json"
            } else "${updateTestOrigin}update.json"
            val backupManifest = when {
                updateTestBackupOrigin.isNotEmpty() -> "${updateTestBackupOrigin}update.json"
                updateTestOrigin.isNotEmpty() -> ""
                else -> "https://youriptv.us/updates/update.json"
            }
            buildConfigField("String", "UPDATE_MANIFEST_URL", "\"$updateManifest\"")
            buildConfigField("String", "UPDATE_BACKUP_MANIFEST_URL", "\"$backupManifest\"")
            buildConfigField("String", "UPDATE_TEST_ORIGIN", "\"$updateTestOrigin\"")
            buildConfigField("String", "UPDATE_TEST_BACKUP_ORIGIN", "\"$updateTestBackupOrigin\"")
        }
        testInstrumentationRunner = "com.youriptv.app.PlaySmokeInstrumentation"
    }

    // These are independently supplied identities, even when their current public certificates match.
    val signingVariable = if (isCustomerDistribution) {
        "YOURIPTV_CUSTOMER_SIGNING_PROPERTIES"
    } else "YOURIPTV_UPLOAD_SIGNING_PROPERTIES"
    val signingPath = providers.environmentVariable(signingVariable).orNull
    val releaseSigning = signingPath?.let { path ->
        val config = file(path).canonicalFile
        require(!config.toPath().startsWith(rootDir.canonicalFile.toPath())) {
            "Signing properties must be outside the repository"
        }
        val props = Properties().apply { config.inputStream().use { load(it) } }
        signingConfigs.create(if (isCustomerDistribution) "customerRelease" else "playUpload") {
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
            applicationIdSuffix = if (isCustomerDistribution) ".customertest" else ".playtest"
            versionNameSuffix = "-isolated-test"
            matchingFallbacks += listOf("debug")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    sourceSets {
        getByName("main").kotlin.directories.add("src/$distribution/java")
        getByName("test").kotlin.directories.add("src/${distribution}Test/java")
        // Build-type priority lets the small selected overlay replace shared resources without duplicating the player.
        listOf("debug", "release", "isolatedTest").forEach { name ->
            getByName(name) {
                val overlay = file("src/$distribution/AndroidManifest.xml")
                if (overlay.exists()) manifest.srcFile(overlay)
                res.directories.add("src/$distribution/res")
            }
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
