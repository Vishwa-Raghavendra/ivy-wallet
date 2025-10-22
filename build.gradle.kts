import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //alias(libs.plugins.android.reporting) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false

    /**F
     * Run with:
     * 1) ./gradlew dependencyUpdates // Simple report in the console
     * 2) ./gradlew dependencyUpdates -DoutputFormatter=html,json,xml
     * (Report in console & generate files accordingly)
     */
    alias(libs.plugins.ben.manes.versions)
}

tasks {
    register("clean", Delete::class) {
        delete(rootProject.buildDir)
    }

    withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

// Any of parameter of this task can be passed on or changed when running the gradle task as parameter
tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {
    outputFormatter = "html"
    outputDir = "build/reports/dependencyUpdates"
    reportfileName = "report"
}

// https://github.com/ben-manes/gradle-versions-plugin#rejectversionsif-and-componentselection
// This has been tested thoroughly by community
fun isNonStable(version: String): Boolean {
    val stableKeyword =
        listOf("RELEASE", "FINAL", "GA", "RC").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}