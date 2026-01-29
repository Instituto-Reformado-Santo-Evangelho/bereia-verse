import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
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
    compileSdk = 36

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
            description = "IRSE | Bereia Verse - Leitor Bíblico Automático"
            vendor = "IRSE"
            copyright = "© 2026 Instituto Reformado Santo Evangelho - IRSE"
            
            // Força a inclusão do módulo SQL para o SQLite funcionar
            modules("java.sql")
            
            linux {
                shortcut = true
                appCategory = "Education"
                menuGroup = "Utility"
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
                
                // Instala o .desktop customizado e o ícone nos caminhos padrão do sistema
                debMaintainer = "IRSE"
            }
            
            macOS {
                bundleID = "br.com.irse.verse"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }

            windows {
                shortcut = true
                menu = true
                menuGroup = "IRSE"
                upgradeUuid = "550e8400-e29b-41d4-a716-446655440000" 
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
        }
    }
}

// Injeta arquivos customizados no pacote .deb (usa matching para ser resiliente à ordem de criação)
tasks.matching { it.name == "packageDeb" }.configureEach {
    val buildDirProvider = project.layout.buildDirectory
    val resourcesDir = project.file("src/jvmMain/resources")
    
    doLast {
        val buildDir = buildDirProvider.get().asFile
        val debOutDir = File(buildDir, "compose/binaries/main/deb")
        if (resourcesDir.exists() && debOutDir.exists()) {
            resourcesDir.copyRecursively(File(debOutDir, "extra_resources"), overwrite = true)
        }
    }
}

// Configuração manual de dependências para o pacote DEB
tasks.withType<AbstractJPackageTask>().configureEach {
    if (name == "packageDeb") {
        freeArgs.add("--linux-package-deps")
        freeArgs.add("libasound2,libpng16-16,libgtk-3-0,libgl1,libx11-6,zlib1g")
    }
}
