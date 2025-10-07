plugins {
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
    java
    `maven-publish`
}

group = "com.asyncsite"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starter - Auto-configuration support
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Micrometer Tracing - Core distributed tracing
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")

    // Spring Boot Auto-configuration annotation processor
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Logging
    implementation("org.slf4j:slf4j-api")

    // Jackson for JSON logging (already included via spring-boot-starter-web, but explicit for clarity)
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // Optional: Hibernate5Module for JPA entity logging
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-hibernate5")

    // Optional: Kafka header propagation support
    compileOnly("org.springframework.kafka:spring-kafka")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.micrometer:micrometer-tracing-test")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

// This is a library, not a bootable application
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Sources JAR 생성
val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Javadoc JAR 생성
val javadocJar by tasks.creating(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// Suppress Gradle metadata validation for dependencies without versions
tasks.withType<org.gradle.api.publish.tasks.GenerateModuleMetadata> {
    suppressedValidationErrors.add("enforced-platform")
    suppressedValidationErrors.add("dependencies-without-versions")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.asyncsite"
            artifactId = "common-tracer"
            version = "0.1.0-SNAPSHOT"

            from(components["java"])

            // Sources and Javadoc JARs
            artifact(sourcesJar)
            artifact(javadocJar)

            pom {
                name = "Common Tracer"
                description = "Distributed tracing with Micrometer + LogTracer integration for AsyncSite microservices"
                url = "https://github.com/AsyncSite/common-tracer"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "asyncsite"
                        name = "AsyncSite Team"
                        email = "team@asyncsite.com"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/AsyncSite/common-tracer.git"
                    developerConnection = "scm:git:ssh://github.com/AsyncSite/common-tracer.git"
                    url = "https://github.com/AsyncSite/common-tracer"
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/AsyncSite/common-tracer")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}
