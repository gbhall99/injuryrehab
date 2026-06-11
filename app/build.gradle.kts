import org.gradle.api.tasks.Exec

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        // dx (the only dexer available offline) cannot translate invokedynamic,
        // so force class-based lambdas / SAM conversions.
        freeCompilerArgs.addAll("-Xlambdas=class", "-Xsam-conversions=class")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    // Real framework classes (not stubs) used as a compile-time classpath only;
    // the device supplies the actual framework at runtime.
    compileOnly("org.robolectric:android-all:14-robolectric-10818077")
    implementation(project(":core"))
}

// ---------------------------------------------------------------------------
// Manual APK pipeline: kotlinc -> dx -> aapt -> zipalign -> apksigner.
// Uses Debian-packaged AOSP tools (aapt, zipalign, apksigner, dalvik-exchange)
// because Google's SDK download hosts are unavailable in this environment.
// ---------------------------------------------------------------------------

val platformJar = "/usr/lib/android-sdk/platforms/android-23/android.jar"
val apkDir = layout.buildDirectory.dir("apk")
val dexInputDir = layout.buildDirectory.dir("dexinput")

val collectDexInput by tasks.registering(Copy::class) {
    dependsOn(tasks.named("jar"), project(":core").tasks.named("jar"))
    into(dexInputDir)
    from(zipTree(tasks.named<Jar>("jar").flatMap { it.archiveFile }))
    from(zipTree(project(":core").tasks.named<Jar>("jar").get().archiveFile))
    configurations.runtimeClasspath.get()
        .filter { it.name.endsWith(".jar") && !it.name.contains("annotations") }
        .forEach { from(zipTree(it)) }
    exclude("META-INF/**", "module-info.class", "**/*.kotlin_metadata", "**/*.kotlin_builtins")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val dex by tasks.registering(Exec::class) {
    dependsOn(collectDexInput)
    inputs.dir(dexInputDir)
    outputs.file(apkDir.map { it.file("classes.dex") })
    doFirst { apkDir.get().asFile.mkdirs() }
    commandLine(
        "dalvik-exchange", "--dex", "--min-sdk-version=26",
        "--output=${apkDir.get().asFile}/classes.dex",
        dexInputDir.get().asFile.toString()
    )
}

val aaptPackage by tasks.registering(Exec::class) {
    dependsOn(dex)
    val manifest = file("src/main/AndroidManifest.xml")
    val resDir = file("src/main/res")
    inputs.file(manifest)
    inputs.dir(resDir)
    outputs.file(apkDir.map { it.file("recoverwell-unsigned.apk") })
    commandLine(
        "aapt", "package", "-f",
        "-M", manifest.toString(),
        "-S", resDir.toString(),
        "-I", platformJar,
        "-F", "${apkDir.get().asFile}/recoverwell-unsigned.apk",
        "--min-sdk-version", "26",
        "--target-sdk-version", "30",
        "--version-code", "1",
        "--version-name", "1.0"
    )
}

val addDex by tasks.registering(Exec::class) {
    dependsOn(aaptPackage)
    workingDir(apkDir)
    commandLine("aapt", "add", "recoverwell-unsigned.apk", "classes.dex")
}

val zipalignApk by tasks.registering(Exec::class) {
    dependsOn(addDex)
    workingDir(apkDir)
    commandLine("zipalign", "-f", "4", "recoverwell-unsigned.apk", "recoverwell-aligned.apk")
}

val generateKeystore by tasks.registering(Exec::class) {
    val ks = apkDir.map { it.file("debug.keystore") }
    outputs.file(ks)
    onlyIf { !ks.get().asFile.exists() }
    doFirst { apkDir.get().asFile.mkdirs() }
    commandLine(
        "keytool", "-genkeypair", "-keystore", ks.get().asFile.toString(),
        "-alias", "debug", "-storepass", "android", "-keypass", "android",
        "-dname", "CN=RecoverWell Debug", "-keyalg", "RSA", "-keysize", "2048",
        "-validity", "10000"
    )
}

val assembleApk by tasks.registering(Exec::class) {
    dependsOn(zipalignApk, generateKeystore)
    workingDir(apkDir)
    commandLine(
        "apksigner", "sign",
        "--ks", "debug.keystore", "--ks-pass", "pass:android", "--key-pass", "pass:android",
        "--out", "recoverwell-debug.apk", "recoverwell-aligned.apk"
    )
    doLast {
        exec { workingDir(apkDir); commandLine("apksigner", "verify", "recoverwell-debug.apk") }
        println("APK ready: ${apkDir.get().asFile}/recoverwell-debug.apk")
    }
}
