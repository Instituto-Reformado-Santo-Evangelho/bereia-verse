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
            // Include natives for all platforms to allow cross-platform uberJar
            implementation(compose.desktop.linux_x64)
            implementation(compose.desktop.windows_x64)
            implementation(compose.desktop.macos_x64)
            implementation(compose.desktop.macos_arm64)
            
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
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
    }

    buildToolsVersion = "36.1.0"
}

dependencies {
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
            }
            
            windows {
                shortcut = true
                menu = true
                upgradeUuid = "550e8400-e29b-41d4-a716-446655440000" 
            }
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

    // CORREÇÃO: Usa provider para evitar resolução prematura das dependências (Config Cache safe)
    from(provider {
        project.configurations.getByName("jvmRuntimeClasspath").map { 
            if (it.isDirectory) it else zipTree(it) 
        }
    })
    
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// Tarefa Customizada para criar o EXE (Tipo JavaExec para evitar warnings e erros de escopo)
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

    // Configuração da Execução do Launch4j
    this.classpath = project.files(File(launch4jDir, "launch4j.jar"))
    this.mainClass.set("net.sf.launch4j.Main")
    this.args(configFile.absolutePath)
    this.workingDir = launch4jDir
    
    // Definição de Inputs/Outputs para Cache
    this.inputs.file(jarFile)
    this.inputs.property("jreUrl", jreUrl)
    this.outputs.file(outputExe)
    this.outputs.dir(File(outputDir, "jre"))

    doFirst {
        // 1. Baixar Launch4j (usando curl para evitar dependência do Ant/Gradle)
        if (!launch4jDir.exists()) {
            println("Baixando Launch4j...")
            if (!launch4jTgz.exists()) {
                val process = ProcessBuilder("curl", "-L", "-o", launch4jTgz.absolutePath, launch4jUrl).start()
                process.waitFor()
                if (process.exitValue() != 0) throw GradleException("Falha ao baixar Launch4j")
            }
            
            // Extrair
            println("Extraindo Launch4j...")
            val process = ProcessBuilder("tar", "-xzf", launch4jTgz.absolutePath, "-C", buildDirFile.absolutePath).start()
            process.waitFor()
            
            val extracted = File(buildDirFile, "launch4j")
            if (extracted.exists()) extracted.renameTo(launch4jDir)
        }
        
        File(launch4jDir, "bin/windres").setExecutable(true)
        File(launch4jDir, "bin/ld").setExecutable(true)

        // 2. Baixar e Extrair JRE
        if (!jreZip.exists()) {
            println("Baixando JRE 17 Windows...")
            val process = ProcessBuilder("curl", "-L", "-o", jreZip.absolutePath, jreUrl).start()
            process.waitFor()
            if (process.exitValue() != 0) throw GradleException("Falha ao baixar JRE")
        }
        
        val jreTargetDir = File(outputDir, "jre")
        if (!jreTargetDir.exists() || jreTargetDir.list()?.isEmpty() == true) {
            println("Extraindo JRE...")
            if (jreTargetDir.exists()) jreTargetDir.deleteRecursively()
            
            val process = ProcessBuilder("unzip", "-q", jreZip.absolutePath, "-d", outputDir.absolutePath).start()
            process.waitFor()
            
            outputDir.listFiles()?.find { it.isDirectory && it.name.startsWith("jdk") }?.renameTo(jreTargetDir)
        }

        if (!outputDir.exists()) outputDir.mkdirs()

        // 3. Copiar JAR Universal
        jarFile.copyTo(File(outputDir, jarFile.name), overwrite = true)

        // 4. Gerar XML de Configuração
        val configXml = """
            <launch4jConfig>
              <dontWrapJar>false</dontWrapJar>
              <headerType>gui</headerType>
              <jar>${jarFile.name}</jar>
              <outfile>${outputExe.absolutePath}</outfile>
              <errTitle>IRSE | Bereia Verse Error</errTitle>
              <cmdLine></cmdLine>
              <chdir>.</chdir>
              <priority>normal</priority>
              <downloadUrl>https://java.com/download</downloadUrl>
              <supportUrl></supportUrl>
              <stayAlive>false</stayAlive>
              <restartOnCrash>false</restartOnCrash>
              <manifest></manifest>
              <icon></icon>
              <jre>
                <path>jre</path>
                <bundledJre64Bit>true</bundledJre64Bit>
                <bundledJreAsFallback>false</bundledJreAsFallback>
                <minVersion>17.0.0</minVersion>
                <maxVersion></maxVersion>
                <jdkPreference>preferJre</jdkPreference>
                <runtimeBits>64</runtimeBits>
                <!-- Opções gráficas removidas para permitir aceleração de hardware no Windows -->
              </jre>
            </launch4jConfig>
        """.trimIndent()
        configFile.writeText(configXml)
        
        println("Launch4j configurado. Iniciando geração do EXE...")
    }
    
    // Resolve caminho fora do bloco de execução
    val distDir = project.rootProject.file("dist/windows")
    
    doLast {
        if (!distDir.exists()) distDir.mkdirs()
        
        // CORREÇÃO: Usar copyRecursively do Kotlin Stdlib para evitar acessar 'project' em tempo de execução
        // outputDir já foi resolvido na configuração
        outputDir.copyRecursively(distDir, overwrite = true)
        
        println("Arquivos copiados para: ${distDir.absolutePath}")
    }
}

// Tarefa para criar Instalador Windows (Requer 'makensis' instalado no Linux)
tasks.register<Exec>("packageWindows") {
    group = "distribution"
    description = "Gera o instalador NSIS para Windows (Requer makensis instalado)"
    
    dependsOn("createExe")
    
    val nsiFile = project.rootProject.file("installers/windows/installer.nsi")
    val distDir = project.rootProject.file("dist/windows")
    
    // Passa o diretório de build como argumento para o script NSIS
    commandLine("makensis", "-DBUILD_DIR=${distDir.absolutePath}", nsiFile.absolutePath)
    
    doFirst {
        println("Gerando instalador Windows via NSIS...")
        // CORREÇÃO: Usar java.io.File direto para evitar acessar 'project.file'
        if (!File("/usr/bin/makensis").exists() && !File("/usr/local/bin/makensis").exists()) {
             throw GradleException("Ferramenta 'makensis' não encontrada. Instale o NSIS (ex: sudo pacman -S nsis).")
        }
    }
}

// Tarefa para copiar os instaladores Linux (.deb) para a pasta dist
val copyLinuxDistributables by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copia os pacotes Linux (.deb) gerados para a pasta dist/linux"

    val debSourceDir = project.layout.buildDirectory.dir("compose/binaries/main/deb")
    val distDir = project.rootProject.layout.projectDirectory.dir("dist/linux")

    from(debSourceDir)
    into(distDir)

    doLast {
        println("Pacote .deb copiado com sucesso para: ${distDir.asFile.absolutePath}")
    }
}

// Intercepta a tarefa padrão 'packageDeb' para rodar a cópia automaticamente ao final
tasks.matching { it.name == "packageDeb" }.all {
    finalizedBy(copyLinuxDistributables)
}

// --- MAC OS ---
val copyMacDistributables by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copia instaladores macOS (.dmg) para dist/mac"

    val dmgSourceDir = project.layout.buildDirectory.dir("compose/binaries/main/dmg")
    val distDir = project.rootProject.layout.projectDirectory.dir("dist/mac")

    from(dmgSourceDir)
    into(distDir)

    doLast {
        println("DMG copiado para: ${distDir.asFile.absolutePath}")
    }
}

tasks.matching { it.name == "packageDmg" }.all {
    finalizedBy(copyMacDistributables)
}

// --- ANDROID ---
val copyAndroidDistributables by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copia APKs gerados (Debug e Release) para dist/android"

    val androidBuildDir = project.rootProject.file("androidApp/build/outputs/apk")
    val distDir = project.rootProject.layout.projectDirectory.dir("dist/android")

    from(androidBuildDir)
    into(distDir)

    doLast {
        println("APKs copiados para: ${distDir.asFile.absolutePath}")
    }
}

// Automatiza a cópia do Android após o build
project.rootProject.allprojects {
    tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.all {
        if (project.name == "androidApp") {
            finalizedBy(copyAndroidDistributables)
        }
    }
}