plugins {
    `kotlin-dsl`
    `java-library`
    kotlin("jvm") version "1.7.10"
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
