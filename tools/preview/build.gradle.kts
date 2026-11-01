// Herramienta de desarrollo: renderiza escenas y pantallas de DowPaper a PNG en la JVM
// usando Robolectric con gráficos nativos. Requiere haber ejecutado ../../build.sh antes.
plugins { kotlin("jvm") version "2.0.21" }

val androidJar = providers.environmentVariable("ANDROID_JAR").orElse("/opt/android-sdk/platforms/android-35/android.jar").get()
val appRoot = rootDir.resolve("../..").canonicalFile

dependencies {
    testCompileOnly(files(androidJar))
    testImplementation(files(appRoot.resolve("build/classes")))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1") { exclude(group = "androidx.test"); exclude(group = "androidx.test.espresso") }
}

kotlin { jvmToolchain(21) }

val genTestConfig by tasks.registering {
    val out = layout.buildDirectory.dir("generated/testconfig")
    outputs.dir(out)
    doLast {
        val f = out.get().file("com/android/tools/test_config.properties").asFile
        f.parentFile.mkdirs()
        f.writeText(
            "android_resource_apk=${appRoot.resolve("build/base.apk").absolutePath}\n" +
            "android_merged_manifest=${appRoot.resolve("app/src/main/AndroidManifest.xml").absolutePath}\n" +
            "android_custom_package=com.dowpaper.android\n"
        )
    }
}
sourceSets["test"].resources.srcDir(genTestConfig)

tasks.test {
    systemProperty("preview.out", appRoot.resolve("docs/screenshots").absolutePath)
    systemProperty("robolectric.logging", "stdout")
    maxHeapSize = "3g"
    testLogging { showStandardStreams = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    outputs.upToDateWhen { false }
}

tasks.register<Copy>("dumpDeps") { from(configurations["testRuntimeClasspath"]); into(layout.buildDirectory.dir("deps")) }
