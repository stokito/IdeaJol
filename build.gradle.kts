plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.1"
}

group = "com.github.stokito.IdeaJol"
version = "1.12.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjdk.jol:jol-core:0.17")
}

intellij {
    version.set("2022.3.1")
    type.set("IC")

    plugins.set(listOf("java"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("")
    }
//    runPluginVerifier {
//        ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
//    }
    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
