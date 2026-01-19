import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.denyshorman"
version = "0.1.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 21
    }

    withType<Javadoc>().configureEach {
        options {
            this as StandardJavadocDocletOptions
            addBooleanOption("Xdoclint:none", true)
            encoding = "UTF-8"
            charSet = "UTF-8"
        }
    }

    test {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "Nanoflow"
                description = "A tiny, simple flow implementation for Java."
                url = "https://github.com/denyshorman/nanoflow"
                inceptionYear = "2026"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "denyshorman"
                        name = "Denys Horman"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/denyshorman/nanoflow.git"
                    developerConnection = "scm:git:ssh://github.com/denyshorman/nanoflow.git"
                    url = "https://github.com/denyshorman/nanoflow"
                }
                issueManagement {
                    system = "GitHub"
                    url = "https://github.com/denyshorman/nanoflow/issues"
                }
            }
        }
    }
    repositories {
        maven {
            val isSnapshot = version.toString().endsWith("SNAPSHOT")

            url = if (isSnapshot) {
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                uri(layout.buildDirectory.dir("release-bundle"))
            }

            if (isSnapshot) {
                credentials {
                    username = project.findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME")
                    password = project.findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["maven"])
}
