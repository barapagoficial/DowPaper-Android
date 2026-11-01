// Herramienta de desarrollo: renderiza escenas y pantallas de DowPaper a PNG en la JVM
// usando Robolectric con gráficos nativos. Requiere haber ejecutado ../../build.sh antes.
plugins { kotlin("jvm") version "2.0.21" }

val androidJar = providers.environmentVariable("ANDROID_JAR").orElse("/opt/android-sdk/platforms/android-35/android.jar").get()
val appRoot = rootDir.resolve("../..").canonicalFile

dependencies {
    testCompileOnly(files(androidJar))
    testRuntimeOnly(files(androidJar)) // como hace AGP: solo para la reflexión de JUnit; Robolectric usa android-all
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
        // APK de recursos con targetSdk 33 (el SDK más reciente que Robolectric puede usar aquí sin red)
        val resApk = out.get().file("preview-res.apk").asFile
        val resJar = providers.environmentVariable("RES_JAR").orElse("/opt/android-sdk/platforms/android-34/android.jar").get()
        val proc = ProcessBuilder("aapt2", "link", "-I", resJar, "--manifest", appRoot.resolve("build/AndroidManifest.xml").absolutePath,
            "--min-sdk-version", "26", "--target-sdk-version", "33", "-o", resApk.absolutePath,
            appRoot.resolve("build/res/res.zip").absolutePath, "--auto-add-overlay").inheritIO().start()
        check(proc.waitFor() == 0) { "aapt2 link falló" }
        f.writeText(
            "android_resource_apk=${resApk.absolutePath}\n" +
            "android_merged_manifest=${appRoot.resolve("app/src/main/AndroidManifest.xml").absolutePath}\n" +
            "android_custom_package=com.dowpaper.android\n"
        )
    }
}
sourceSets["test"].resources.srcDir(genTestConfig)

tasks.test {
    systemProperty("preview.out", appRoot.resolve("docs/screenshots").absolutePath)
    systemProperty("robolectric.logging", "stdout")
    systemProperty("preview.skipScenes", providers.gradleProperty("skipScenes").orElse("false").get())
    // Si existe una carpeta con los jars android-all descargados, usarla en modo offline.
    val depDir = providers.environmentVariable("ROBOLECTRIC_DEPS").orElse("/opt/robolectric-deps").get()
    if (file(depDir).isDirectory) {
        systemProperty("robolectric.offline", "true")
        systemProperty("robolectric.dependency.dir", depDir)
    }
    maxHeapSize = "3g"
    testLogging { showStandardStreams = true; exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
    outputs.upToDateWhen { false }
}

tasks.register<Copy>("dumpDeps") { from(configurations["testRuntimeClasspath"]); into(layout.buildDirectory.dir("deps")) }
