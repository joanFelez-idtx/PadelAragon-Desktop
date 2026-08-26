val versionMajor = 2
val versionMinor = 1
val versionPatch = 0

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    id("org.jetbrains.compose") version "1.12.0-rc01"
    id("com.google.devtools.ksp") version "2.3.6"
}

group = "com.padelaragon.desktop"
version = "$versionMajor.$versionMinor.$versionPatch"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    implementation("org.jsoup:jsoup:1.22.1")
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")

    // Room (desktop/JVM target, BundledSQLiteDriver)
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.sqlite:sqlite-bundled:2.6.1")
    ksp("androidx.room:room-compiler:2.8.4")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.13.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.jsoup:jsoup:1.22.1")
}

compose.desktop {
    application {
        mainClass = "com.padelaragon.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Rpm
            )
            packageName = "PadelAragon"
            packageVersion = "$versionMajor.$versionMinor.$versionPatch"
            // jpackage/jlink trims the bundled runtime image to only the JDK modules
            // jdeps detects as statically referenced. TLS handshakes with Let's
            // Encrypt's ISRG Root X1 chain need EC crypto support (jdk.crypto.ec),
            // which OkHttp/the JDK only pull in reflectively via the JCA provider
            // lookup - jdeps never sees it, so it silently gets stripped from the
            // packaged .exe/.msi. That causes "PKIX path building failed" only in
            // the packaged app, never in `./gradlew run` (which uses the full JDK).
            // Including all modules avoids this class of runtime-image bug.
            includeAllModules = true
            windows {
                menuGroup = "PadelAragon"
                perUserInstall = true
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
