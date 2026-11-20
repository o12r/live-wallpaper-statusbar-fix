import java.util.Properties

// LSPosed / Xposed module: makes the status bar (and ZTE's lock screen clock and widgets) follow
// a live wallpaper's own dark/light text hint.
plugins {
    id("com.android.application")
}

android {
    namespace = "com.weworkshop.statusbarfix"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "com.weworkshop.statusbarfix"
        minSdk = 31 // WallpaperColors.getColorHints()
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"
    }

    // Release signing comes from ../keystore.properties (git-ignored). Without it, release
    // builds fall back to the debug key so anyone can still build the project.
    val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { f ->
        Properties().apply { f.inputStream().use { load(it) } }
    }
    signingConfigs {
        if (keystoreProps != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // The hook class is loaded by name from assets/xposed_init; don't shrink or rename it.
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

base {
    archivesName = "LiveWallpaperStatusBarFix"
}

dependencies {
    compileOnly(project(":stubs"))
}
