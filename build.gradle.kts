import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.0"
    `maven-publish`
}

group = "io.github.omkar-tenkale"
version = "0.2.0"

repositories {
    mavenCentral()
}

dependencies {
    val ktor_version = "3.0.1"
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-webjars:$ktor_version")
    implementation("io.ktor:ktor-server-auth:$ktor_version")
    testImplementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    testImplementation("io.ktor:ktor-server-auth:$ktor_version")
    testImplementation("io.ktor:ktor-server-call-logging:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

tasks.test {
    useJUnitPlatform()
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "ktor-role-based-auth"
            from(components["java"])
            pom {
                name.set("Ktor Role Based Auth")
                description.set("Ktor plugin to easily handle role based authorization ")
                url.set("https://github.com/omkar-tenkale/ktor-role-based-auth")
            }
        }
    }
}