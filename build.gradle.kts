plugins {
    // DECISION: Apply plugins as 'apply false' at root level so subprojects inherit versions consistently.
    // SEE: ADR-002
    java
    id("org.springframework.boot") version "3.4.2" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.swiggy"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    // DECISION: Target Java 21 LTS across all subprojects for Virtual Threads and modern language features.
    // SEE: ADR-002
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // DECISION: Centralize Spring Boot dependency version alignment across all services and libraries.
    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.2")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // DECISION: Allow dynamic agent loading for ByteBuddy/Mockito on Java 21.
        jvmArgs("-XX:+EnableDynamicAgentLoading")
    }
}
