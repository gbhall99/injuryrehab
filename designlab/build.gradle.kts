plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
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
