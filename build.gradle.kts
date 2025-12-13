plugins {
    id("io.micronaut.application") version "4.6.0"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.freefair.lombok") version "8.14.3"
}

version = "0.1"
group = "lv.taukulis"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor("info.picocli:picocli-codegen")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    implementation("info.picocli:picocli")
    implementation("io.micronaut.picocli:micronaut-picocli")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    runtimeOnly("ch.qos.logback:logback-classic")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


application {
    mainClass = "lv.taukulis.UgitCommand"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}



micronaut {
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("lv.taukulis.*")
    }
}


tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}


