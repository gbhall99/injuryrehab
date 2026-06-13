import org.gradle.api.tasks.Exec

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    coreLibrariesVersion = "1.7.21"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        // ART has no LambdaMetafactory: stdlib must stay 1.7.x (last indy-free
        // release) and the compiler must not emit 1.8+ stdlib intrinsics.
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_7)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_7)
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
    implementation(project(":draw"))

    // JVM-side integration tests: Robolectric boots the real Activity,
    // receivers and SQLite store without needing an emulator.
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:3.8")
    // On the runtime classpath too: JUnit must be able to resolve android.app.Application
    // when parsing @Config; the sandbox classloader still serves instrumented classes.
    // API 26 jar (Java-8 era bytecode) so it loads on the Java 11 test JVM, and it
    // doubles as a canary that the app sticks to APIs available at minSdk 26.
    testImplementation("org.robolectric:android-all:8.0.0_r4-robolectric-r1")
}

// Native sqlite for Robolectric's SQLite shadow (real database in JVM tests).
val sqliteNatives: Configuration by configurations.creating
dependencies {
    sqliteNatives("com.almworks.sqlite4java:libsqlite4java-linux-amd64:0.282@so")
}
val prepareSqliteNative by tasks.registering(Copy::class) {
    from(sqliteNatives)
    into(layout.buildDirectory.dir("sqlite4java"))
}

// Robolectric 3.8 self-downloads its SDK jar over plain HTTP, which breaks
// behind https-only networks - feed it the Gradle-resolved jar instead.
val robolectricSdkDir = layout.buildDirectory.dir("robolectric-sdk")
val prepareRobolectricSdk by tasks.registering(Copy::class) {
    from(configurations.named("testRuntimeClasspath").get().filter { it.name.startsWith("android-all-") })
    into(robolectricSdkDir)
}

tasks.test {
    dependsOn(prepareRobolectricSdk, prepareSqliteNative)
    systemProperty(
        "sqlite4java.library.path",
        layout.buildDirectory.dir("sqlite4java").get().asFile.absolutePath
    )
    // Robolectric 3.8 relies on the Field.modifiers reflection hack, which was
    // removed in newer JVMs - so the tests (only) execute on a Java 8 JVM.
    javaLauncher.set(project.extensions.getByType<JavaToolchainService>().launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", robolectricSdkDir.get().asFile.absolutePath)
    maxHeapSize = "2g"
    setForkEvery(1)
}

// ---------------------------------------------------------------------------
// Manual APK pipeline: kotlinc -> dx -> aapt -> zipalign -> apksigner.
// Uses Debian-packaged AOSP tools (aapt, zipalign, apksigner, dalvik-exchange)
// because Google's SDK download hosts are unavailable in this environment.
// ---------------------------------------------------------------------------

val platformJar = "/usr/lib/android-sdk/platforms/android-23/android.jar"
val apkDir = layout.buildDirectory.dir("apk")
val dexInputDir = layout.buildDirectory.dir("dexinput")

val collectDexInput by tasks.registering(Sync::class) {
    dependsOn(tasks.named("jar"), project(":core").tasks.named("jar"))
    into(dexInputDir)
    from(zipTree(tasks.named<Jar>("jar").flatMap { it.archiveFile }))
    from(zipTree(project(":core").tasks.named<Jar>("jar").get().archiveFile))
    configurations.runtimeClasspath.get()
        .filter { it.name.endsWith(".jar") && !it.name.contains("annotations") }
        .forEach { from(zipTree(it)) }
    // kotlin-stdlib's java.util.stream interop uses invokedynamic and is
    // unreachable in this app - keep it away from the dexer (guard enforces)
    exclude("kotlin/streams/**")
    exclude("META-INF/**", "module-info.class", "**/*.kotlin_metadata", "**/*.kotlin_builtins")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// ART does not provide LambdaMetafactory/StringConcatFactory: any class that
// references them would dex into invoke-custom and crash on devices. Fail the
// build instead - JVM tests cannot catch this (real Java has those methods).
val checkNoInvokeDynamic by tasks.registering {
    dependsOn(collectDexInput)
    inputs.dir(dexInputDir)
    doLast {
        val offenders = dexInputDir.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "class" }
            .filter { f ->
                val bytes = f.readBytes().toString(Charsets.ISO_8859_1)
                bytes.contains("LambdaMetafactory") || bytes.contains("StringConcatFactory")
            }
            .map { it.relativeTo(dexInputDir.get().asFile).path }
            .toList()
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "invokedynamic bootstrap references found (would crash on device):\n  " +
                    offenders.joinToString("\n  ")
            )
        }
        val n = dexInputDir.get().asFile.walkTopDown().count { it.extension == "class" }
        println("invoke-dynamic guard: " + n + " classes clean")
    }
}

val dex by tasks.registering(Exec::class) {
    dependsOn(collectDexInput, checkNoInvokeDynamic)
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
    outputs.file(apkDir.map { it.file("recoverwell-base.apk") })
    commandLine(
        "aapt", "package", "-f",
        "-M", manifest.toString(),
        "-S", resDir.toString(),
        "-I", platformJar,
        "-F", "${apkDir.get().asFile}/recoverwell-base.apk",
        "--min-sdk-version", "26",
        "--target-sdk-version", "35",
        "--version-code", "18",
        "--version-name", "2.8"
    )
}

// aapt add mutates in place, so work on a copy to keep the pipeline idempotent
val addDex by tasks.registering(Exec::class) {
    dependsOn(aaptPackage)
    inputs.files(apkDir.map { it.file("recoverwell-base.apk") }, apkDir.map { it.file("classes.dex") })
    outputs.file(apkDir.map { it.file("recoverwell-unsigned.apk") })
    workingDir(apkDir)
    doFirst {
        apkDir.get().file("recoverwell-base.apk").asFile
            .copyTo(apkDir.get().file("recoverwell-unsigned.apk").asFile, overwrite = true)
    }
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
