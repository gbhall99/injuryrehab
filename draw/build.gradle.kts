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
        freeCompilerArgs.addAll("-Xlambdas=class", "-Xsam-conversions=class")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.withType<Test>().configureEach { useJUnit() }
