val implementation by configurations

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.6.0-1.0.1")
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":annotations"))
    implementation("com.electronwill.night-config:core:3.6.4")
    implementation("com.electronwill.night-config:json:3.6.5")
}
