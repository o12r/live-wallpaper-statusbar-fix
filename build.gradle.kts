plugins {
    id("com.android.application") version "9.5.1" apply false
    // AGP 9 brings its built-in Kotlin support; pin the Kotlin Gradle plugin it resolves.
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
}
