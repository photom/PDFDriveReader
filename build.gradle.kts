// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        // Security: the Android Gradle Plugin pulls in vulnerable transitive
        // build-tool libraries. Raise them to patched versions; constraints and
        // platforms never add libraries to the classpath.
        classpath(platform(libs.netty.bom))
        constraints {
            classpath(libs.guava)
            classpath(libs.bouncycastle.bcprov)
            classpath(libs.bouncycastle.bcpkix)
            classpath(libs.bouncycastle.bcutil)
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    // AGP's built-in Kotlin support uses the Kotlin Gradle Plugin on the build
    // classpath; declaring it here pins it to a patched version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
}
