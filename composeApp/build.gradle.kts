import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.msix)
}

kotlin {
    jvmToolchain(21)
    
    jvm("desktop") {
        compilerOptions {
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.feather.icons)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.appdirs)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)

                implementation(libs.kotlinx.coroutinesSwing)
                implementation("org.xerial:sqlite-jdbc:3.51.1.0")
                implementation("com.google.api-client:google-api-client:2.2.0")
                implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
                implementation("com.google.apis:google-api-services-drive:v3-rev20230822-2.0.0")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "br.com.irse.verse.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            
            buildTypes.release.proguard {
                isEnabled.set(false)
            }
            
            // Usamos nome padronizado bereiaverse para todas as plataformas
            packageName = "bereiaverse"
            
            packageVersion = "1.1.0"
            description = "Bereia Versículos | IRSE - Leitor Bíblico Automático com anotações"
            vendor = "IRSE"
            copyright = "© 2026 Instituto Reformado Santo Evangelho - IRSE"
            
            modules(
                "java.sql", "java.naming", "java.desktop", "java.xml", 
                "java.management", "java.security.jgss", "java.instrument", 
                "jdk.crypto.ec", "jdk.unsupported"
            )
            
            linux {
                shortcut = true
                appCategory = "Education"
                menuGroup = "Utility"
                iconFile.set(project.file("src/desktopMain/resources/bereiaverse.png"))
                debMaintainer = "IRSE"
            }
            
            macOS {
                bundleID = "br.com.irse.verse"
                iconFile.set(project.file("src/desktopMain/resources/bereiaverse.icns"))
            }

            windows {
                shortcut = true
                menu = true
                menuGroup = "IRSE"
                upgradeUuid = "550e8400-e29b-41d4-a716-446655440000" 
                iconFile.set(project.file("src/desktopMain/resources/bereiaverse.ico"))
            }
        }
    }
}

// Configuração do Plugin MSIX
msix {
    manifest {
        displayName.set("Bereia Versículos") // DisplayName pode ter acento
        publisher.set("CN=B41FD2FB-AD80-4515-8823-5F91386585CC")
        publisherDisplayName.set("Organização IRSE") // Fornecido pela Microsoft
        identityName.set("OrganizaoIRSE.BereiaVersculos") // Normalizado pela Microsoft
        version.set("1.1.1.0")
        appId.set("BereiaVerse")
        processorArchitecture.set("x64")
        description.set("Bereia Versículos | IRSE - Leitor Bíblico Automático com anotações")
    }
}

afterEvaluate {
    val folderName = "bereiaverse" // Pasta sem acento
    val appDir = project.layout.buildDirectory.dir("compose/binaries/main-release/app/$folderName").get().asFile
    
    tasks.named("createAppxManifest", de.stefan_oltmann.msix.CreateAppxManifestTask::class) {
        appExecutable.set("$folderName.exe")
        templateFile.set(project.file("packaging/msix/AppxManifest.xml.template"))
        outputFile.set(appDir.resolve("AppxManifest.xml"))
    }

    val copyMsixResources = tasks.register<Copy>("copyMsixResources") {
        from(project.file("packaging/msix/resources")) {
            include("*.png")
        }
        into(appDir.resolve("resources"))
        mustRunAfter("createMsixIcons")
    }
    
    tasks.named("createMsix", de.stefan_oltmann.msix.CreateMsixTask::class) {
        dependsOn("createReleaseDistributable")
        dependsOn(copyMsixResources)
        appDirectory.set(appDir)
        msixOutputFile.set(project.layout.buildDirectory.file("outputs/msix/BereiaVerse.msix"))
        
        // Configuração de Assinatura
        val pfxPath = project.findProperty("msix.pfx.path")?.toString()
        val pfxPassword = project.findProperty("msix.pfx.password")?.toString()
        
        if (!pfxPath.isNullOrEmpty() && File(pfxPath).exists()) {
            signingPfxFile.set(File(pfxPath))
            signingPassword.set(pfxPassword ?: "")
        }
    }
}

tasks.matching { it.name == "packageDeb" }.configureEach {
    val buildDirProvider = project.layout.buildDirectory
    val resourcesDir = project.file("src/desktopMain/resources")
    
    doLast {
        val buildDir = buildDirProvider.get().asFile
        val debOutDir = File(buildDir, "compose/binaries/main/deb")
        if (resourcesDir.exists() && debOutDir.exists()) {
            resourcesDir.copyRecursively(File(debOutDir, "extra_resources"), overwrite = true)
        }
    }
}

tasks.withType<AbstractJPackageTask>().configureEach {
    if (name == "packageDeb") {
        freeArgs.add("--linux-package-deps")
        freeArgs.add("libasound2,libpng16-16,libgtk-3-0,libgl1,libx11-6,zlib1g")
    }
}
