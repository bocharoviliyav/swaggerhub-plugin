plugins {
    `kotlin-dsl`
    `java-library`
    kotlin("jvm") version "1.7.10"
    id ("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "blog.bocharoviliyav"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    gradlePluginPortal()
}


dependencies {
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.10.0"))

    implementation("com.squareup.okhttp3:okhttp")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
    implementation(kotlin("script-runtime"))
}

gradlePlugin {
    plugins {
        register("swaggerhub-plugin") {
            id = "swaggerhub-plugin"
            displayName = "swaggerhub-plugin"
            implementationClass = "blog.bocharoviliyav.swaggerhub.SwaggerhubPlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}


tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "com.github.csolem.gradle.shadow.kotlin.example.App"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}