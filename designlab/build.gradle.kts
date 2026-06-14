plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    coreLibrariesVersion = "1.7.21"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_7)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_7)
    }
}

dependencies {
    implementation(project(":draw"))
}

// Renders every drawn surface to PNG for design review and regenerates the
// app's vector drawable resources from the single icon source of truth.
tasks.register<JavaExec>("render") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.recoverwell.designlab.MainKt")
    args(rootProject.projectDir.absolutePath)
}
