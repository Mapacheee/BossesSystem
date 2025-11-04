plugins {
    java
    `java-library`
    id("com.gradleup.shadow") version "9.2.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "me.mapacheee"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
}

dependencies {
    api("com.thewinterframework:paper:1.0.4")
    annotationProcessor("com.thewinterframework:paper:1.0.4")
    api("com.thewinterframework:configuration:1.0.2")
    annotationProcessor("com.thewinterframework:configuration:1.0.2")
    api("com.thewinterframework:command:1.0.1")
    annotationProcessor("com.thewinterframework:command:1.0.1")

    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.lumine:Mythic-Dist:5.9.5")
}

tasks {
    processResources {
        filesMatching("paper-plugin.yml") {
            expand("version" to version)
        }
    }
    shadowJar {
        archiveFileName.set("BossesSystem-${project.version}.jar")
    }
}
