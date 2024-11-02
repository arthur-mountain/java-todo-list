// https://docs.gradle.org/8.10.2/userguide/building_java_projects.html
plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // This dependency is used by the application.
    implementation(libs.guava)

    // Serialize json
    implementation(libs.gson)

    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    // Define the main class for the application.
    mainClass = "todolist.TodoListHttpServer"
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.register<Jar>("fatJar") {
    // 設定 JAR 檔案的基本名稱
    archiveBaseName.set("${project.name}-all")

    // 設定如何處理重複的檔案
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // 將主源集的輸出添加到 JAR 中
    from(sourceSets.main.get().output)

    manifest { // 設定清單（Manifest）內容
        attributes["Main-Class"] = "todolist.TodoListHttpServer" // 設定 Main class
    }

    // 依賴於運行時類路徑
    dependsOn(configurations.runtimeClasspath)

    from({
        // 將所有依賴加入到 JAR 中
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
}
