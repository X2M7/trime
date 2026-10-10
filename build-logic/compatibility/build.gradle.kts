// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "com.osfans.trime.build_logic"

dependencies {
    implementation("org.ow2.asm:asm-tree:9.9")
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        register("gradleCompatibility") {
            id = "com.osfans.trime.gradle-compatibility"
            implementationClass = "GradleCompatibilityPlugin"
        }
    }
}
