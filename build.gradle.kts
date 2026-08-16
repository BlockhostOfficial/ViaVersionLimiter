plugins {
    java
    id("com.gradleup.shadow")
}

group = "net.blockhost"
version = providers.gradleProperty("mavenVersion").get()
description = "Enforce Minecraft protocol version policy on Velocity and BungeeCord."

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
    }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "SpigotMC"
    }
    maven("https://jitpack.io") {
        name = "JitPack"
    }
    maven("https://eldonexus.de/repository/maven-public/") {
        name = "EldoNexus"
    }
}

val velocityVersion: String by project
val bungeeCordVersion: String by project
val commonsVersion: String by project
val strokkurCommandsVersion: String by project
val junitVersion: String by project

dependencies {
    compileOnly("com.velocitypowered:velocity-api:$velocityVersion")
    compileOnly("net.md-5:bungeecord-api:$bungeeCordVersion")

    implementation("com.github.6b6t.6b6t-commons:commons-config:$commonsVersion")
    implementation("com.github.6b6t.6b6t-commons:commons-commands-core:$commonsVersion")
    implementation("org.yaml:snakeyaml:2.4")

    compileOnly("net.strokkur.commands:annotations-velocity:$strokkurCommandsVersion")
    compileOnly("net.strokkur.commands:annotations-common-permission:$strokkurCommandsVersion")
    annotationProcessor("net.strokkur.commands:processor-velocity:$strokkurCommandsVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:deprecation"))
}

val pluginVersion = project.version.toString()
val pluginDescription = requireNotNull(project.description)

tasks.processResources {
    inputs.property("version", pluginVersion)
    inputs.property("description", pluginDescription)
    filesMatching(listOf("bungee.yml", "velocity-plugin.json")) {
        expand(
            mapOf(
                "version" to pluginVersion,
                "description" to pluginDescription,
            )
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveBaseName = "ViaVersionLimiter"
    archiveClassifier = "unshaded"
}

tasks.shadowJar {
    archiveBaseName = "ViaVersionLimiter"
    archiveClassifier = ""

    dependencies {
        include(dependency("com.github.6b6t.6b6t-commons:commons-config:.*"))
        include(dependency("com.github.6b6t.6b6t-commons:commons-core:.*"))
        include(dependency("com.github.6b6t.6b6t-commons:commons-commands-core:.*"))
        include(dependency("com.github.6b6t.6b6t-commons:commons-message:.*"))
        include(dependency("com.github.6b6t.ConfigLib:configlib-core:.*"))
        include(dependency("com.github.6b6t.ConfigLib:configlib-yaml:.*"))
        include(dependency("org.yaml:snakeyaml:.*"))
        include(dependency("org.snakeyaml:snakeyaml-engine:.*"))
        include(dependency("net.strokkur.commands:annotations-common:.*"))
    }

    relocate("net.blockhost.commons", "com.enderdash.agent.viaversionlimiter.libs.commons")
    relocate("de.exlll.configlib", "com.enderdash.agent.viaversionlimiter.libs.configlib")
    relocate("org.yaml.snakeyaml", "com.enderdash.agent.viaversionlimiter.libs.snakeyaml")
    relocate("org.snakeyaml.engine", "com.enderdash.agent.viaversionlimiter.libs.snakeyamlengine")

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    exclude("module-info.class", "META-INF/versions/*/module-info.class")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
