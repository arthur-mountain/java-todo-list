// https://docs.gradle.org/8.10.2/userguide/multi_project_builds.html

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "todo-list"
include("app")
