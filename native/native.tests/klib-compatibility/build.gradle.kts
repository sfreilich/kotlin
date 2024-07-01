plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(projectTests(":native:native.tests"))
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

testsJar {}

val latestReleasedCompiler = findProperty("kotlin.internal.native.test.latestReleasedCompilerVersion") as String
val releasedCompiler by nativePublishedDistribution(latestReleasedCompiler)

nativeTest(
    "test",
    null,
    releasedCompilerDist = releasedCompiler,
    maxMetaspaceSizeMb = 1024 // to handle two compilers in classloader
)