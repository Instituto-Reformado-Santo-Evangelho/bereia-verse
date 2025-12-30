import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
}

// Configuração para ferramentas de build externas
val launch4jConfig: Configuration? by configurations.creating

dependencies {
    launch4jConfig?.invoke("net.sf.launch4j:launch4j:3.14")
    launch4jConfig?.invoke("com.thoughtworks.xstream:xstream:1.4.20")
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    
    jvm()
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.service)
            implementation(libs.androidx.savedstate)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.feather.icons)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            // Include natives for all platforms ONLY if explicitly requested (e.g. for Fat Jar)
            if (project.hasProperty("universal")) {
                implementation(compose.desktop.linux_x64)
                implementation(compose.desktop.windows_x64)
                implementation(compose.desktop.macos_x64)
                implementation(compose.desktop.macos_arm64)
            } else {
                // Default: Include only the native libraries for the current OS to reduce package size
                implementation(compose.desktop.currentOs)
            }
            
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("org.xerial:sqlite-jdbc:3.51.1.0")

            // Google Drive JVM
            implementation("com.google.api-client:google-api-client:2.2.0")
            implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
            implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
        }
    }
}

android {
    namespace = "br.com.irse.verse.ui"

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Corrigir conflitos de recursos do Google Client
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}

compose.desktop {
    application {
        mainClass = "br.com.irse.verse.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "bereia-verse"
            packageVersion = "1.0.0"
            
            // Força a inclusão do módulo SQL para o SQLite funcionar
            modules("java.sql")
            
            linux {
                shortcut = true
                appCategory = "Education"
                menuGroup = "Utility"
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
            
            macOS {
                bundleID = "br.com.irse.verse"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }

            windows {
                shortcut = true
                menu = true
                upgradeUuid = "550e8400-e29b-41d4-a716-446655440000" 
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
        }
    }
}

// Injeta arquivos customizados no pacote .deb (usa matching para ser resiliente à ordem de criação)
tasks.matching { it.name == "packageDeb" }.all {
    doLast {
        val buildDir = project.layout.buildDirectory.get().asFile
        val resourcesDir = project.file("src/jvmMain/resources")
        val debOutDir = File(buildDir, "compose/binaries/main/deb")
        if (resourcesDir.exists() && debOutDir.exists()) {
            resourcesDir.copyRecursively(File(debOutDir, "extra_resources"), overwrite = true)
        }
    }
}

// Tarefa Manual para criar JAR Universal (Fat Jar)
val packageUniversalJar by tasks.registering(Jar::class) {
    archiveBaseName.set("bereia-verse-universal")
    archiveVersion.set("1.0.0")
    
    manifest {
        attributes["Main-Class"] = "br.com.irse.verse.MainKt"
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    val mainCompilation = kotlin.jvm().compilations.getByName("main")
    from(mainCompilation.output)

    from(provider {
        project.configurations.getByName("jvmRuntimeClasspath").map { 
            if (it.isDirectory) it else zipTree(it) 
        }
    })
    
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// Tarefa Customizada para criar o EXE Windows localmente
tasks.register<JavaExec>("createExe") {
    group = "distribution"
    description = "Empacota o UberJar em um executável Windows (.exe) com JRE embutido"
    
    dependsOn(packageUniversalJar)
    
    val buildDirFile = project.layout.buildDirectory.get().asFile
    val outputDir = File(buildDirFile, "compose/binaries/main/exe")
    val outputExe = File(outputDir, "BereiaVerse.exe")
    val configFile = File(outputDir, "launch4j-config.xml")
    val jarFile = packageUniversalJar.get().archiveFile.get().asFile
    
    val launch4jDir = File(buildDirFile, "launch4j-tool")
    val launch4jTgz = File(buildDirFile, "launch4j.tgz")
    val jreZip = File(buildDirFile, "windows-jre-17.zip")
    val jreUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.13%2B11/OpenJDK17U-jre_x64_windows_hotspot_17.0.13_11.zip"
    val launch4jUrl = "https://sourceforge.net/projects/launch4j/files/launch4j-3/3.14/launch4j-3.14-linux-x64.tgz"

    this.classpath = project.files(File(launch4jDir, "launch4j.jar"))
    this.mainClass.set("net.sf.launch4j.Main")
    this.args(configFile.absolutePath)
    this.workingDir = launch4jDir
    
    this.inputs.file(jarFile)
    this.outputs.file(outputExe)

    doFirst {
        if (!launch4jDir.exists()) {
            val proc = ProcessBuilder("curl", "-L", "-o", launch4jTgz.absolutePath, launch4jUrl).start()
            proc.waitFor()
            ProcessBuilder("tar", "-xzf", launch4jTgz.absolutePath, "-C", buildDirFile.absolutePath).start().waitFor()
            val extracted = File(buildDirFile, "launch4j")
            if (extracted.exists()) extracted.renameTo(launch4jDir)
        }
        
        File(launch4jDir, "bin/windres").setExecutable(true)
        File(launch4jDir, "bin/ld").setExecutable(true)

        if (!jreZip.exists()) {
            ProcessBuilder("curl", "-L", "-o", jreZip.absolutePath, jreUrl).start().waitFor()
        }
        
        val jreTargetDir = File(outputDir, "jre")
        if (!jreTargetDir.exists()) {
            if (!outputDir.exists()) outputDir.mkdirs()
            ProcessBuilder("unzip", "-q", jreZip.absolutePath, "-d", outputDir.absolutePath).start().waitFor()
            outputDir.listFiles()?.find { it.isDirectory && it.name.startsWith("jdk") }?.renameTo(jreTargetDir)
        }

        jarFile.copyTo(File(outputDir, jarFile.name), overwrite = true)

        val configXml = """
            <launch4jConfig>
              <dontWrapJar>false</dontWrapJar>
              <headerType>gui</headerType>
              <jar>${jarFile.name}</jar>
              <outfile>${outputExe.absolutePath}</outfile>
              <errTitle>IRSE | Bereia Verse Error</errTitle>
              <chdir>.</chdir>
              <priority>normal</priority>
              <downloadUrl>https://java.com/download</downloadUrl>
              <stayAlive>false</stayAlive>
              <jre>
                <path>jre</path>
                <bundledJre64Bit>true</bundledJre64Bit>
                <minVersion>17.0.0</minVersion>
                <jdkPreference>preferJre</jdkPreference>
                <runtimeBits>64</runtimeBits>
              </jre>
            </launch4jConfig>
        """.trimIndent()
        configFile.writeText(configXml)
    }
    
    val distDir = project.rootProject.file("dist/windows")
    doLast {
        if (!distDir.exists()) distDir.mkdirs()
        outputDir.copyRecursively(distDir, overwrite = true)
    }
}

// Tarefa para copiar instaladores Linux (.deb) para dist/linux
val copyLinuxDistributables by tasks.registering(Copy::class) {
    val debSourceDir = project.layout.buildDirectory.dir("compose/binaries/main/deb")
    val distDir = project.rootProject.layout.projectDirectory.dir("dist/linux")
    from(debSourceDir)
    into(distDir)
}

tasks.matching { it.name == "packageDeb" }.all { finalizedBy(copyLinuxDistributables) }

// Tarefa para copiar instaladores macOS (.dmg) para dist/mac
val copyMacDistributables by tasks.registering(Copy::class) {
    val dmgSourceDir = project.layout.buildDirectory.dir("compose/binaries/main/dmg")
    val distDir = project.rootProject.layout.projectDirectory.dir("dist/mac")
    from(dmgSourceDir)
    into(distDir)
}

tasks.matching { it.name == "packageDmg" }.all { finalizedBy(copyMacDistributables) }
