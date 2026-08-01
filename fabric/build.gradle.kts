import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import xyz.jpenilla.resourcefactory.fabric.Environment

plugins {
  id("carbon.shadow-platform")
  id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
  alias(libs.plugins.resource.factory.fabric.convention)
}

repositories {
  maven("https://repo.jpenilla.xyz/snapshots/") {
    mavenContent {
      snapshotsOnly()
      includeModuleByRegex("de\\.hexaoxi", "messenger-.*")
      includeModule("com.seiama", "registry")
    }
  }
}


java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(25)
  }
}

tasks.withType<JavaCompile>().configureEach {
  options.release = 25
}


val shade: Configuration by configurations.creating

configurations.implementation {
  extendsFrom(shade)
}

dependencies {
  minecraft(libs.fabricMinecraft)
  implementation(libs.fabricLoader)
  implementation(libs.fabricApi)
  runtimeOnly(libs.fabricApiDeprecated) // LuckPerms needs to work at dev time

  shade(projects.carbonchatCommon) {
    exclude("net.kyori", "adventure-api")
    exclude("net.kyori", "adventure-text-serializer-gson")
    exclude("net.kyori", "adventure-text-serializer-plain")
    exclude("org.incendo", "cloud-core")
    exclude("org.incendo", "cloud-services")
    exclude("org.incendo", "cloud-brigadier")
    exclude("org.incendo", "cloud-minecraft-signed-arguments")
    exclude("io.leangen.geantyref")
  }

  implementation(libs.cloudFabric) {
    exclude("net.fabricmc.fabric-api")
  }
  include(libs.cloudFabric)
  shade(libs.cloudSigned) {
    exclude("org.incendo", "cloud-core")
    exclude("org.incendo", "cloud-services")
  }
  implementation(libs.fabricPermissionsApi)
  include(libs.fabricPermissionsApi)

  implementation(libs.adventurePlatformFabric)

  implementation(libs.miniplaceholders)

  runtimeDownload(libs.mysql)
  include(libs.jarRelocator)
  shade(libs.jarRelocator) {
    exclude(group = "org.ow2.asm")
  }
  runtimeDownload(libs.checkerQual)
}

fabricModJson {
  id = rootProject.name.lowercase()
  name = rootProject.name
  version = project.version.toString()
  description = project.description
  author("Draycia")
  author("jmp")
  contact {
    homepage = GITHUB_REPO_URL
    sources = GITHUB_REPO_URL
    issues = "$GITHUB_REPO_URL/issues"
  }
  license("GPLv3")
  environment = Environment.ANY
  mainEntrypoint("net.draycia.carbon.fabric.CarbonFabricBootstrap")
  mixin("carbonchat.mixins.json")
  depends("fabricloader", ">=" + libs.versions.fabricLoader.get())
  depends("fabric-api", "*")
  depends("cloud", "*")
  depends("adventure-platform-fabric", "*")
  depends("minecraft", ">=${libs.versions.minecraft.get()}")
  depends("luckperms", ">=5.0.0")
  suggests("miniplaceholders", "*")
}



tasks {
  shadowJar {
    configurations = listOf(shade)
    relocateDependency("org.incendo.cloud.minecraft.extras")
    standardRuntimeRelocations()
    relocateGuice()
    relocateDependency("org.checkerframework")
  }
  writeDependencies {
    standardRuntimeRelocations()
    relocateGuice()
    relocateDependency("org.checkerframework")
  }

  runServer {
    dependsOn(shadowJar)

    doFirst {
      val jar = shadowJar.get().archiveFile.get().asFile
      val mods = file("run/mods")
      mods.mkdirs()
      jar.copyTo(mods.resolve("carbonchat-dev.jar"), overwrite = true)
    }
  }
}

publishMods.modrinth {
  environment = ModrinthEnvironment.SERVER_ONLY
  minecraftVersions.set(listOf(libs.versions.minecraft.get()))
  modLoaders.addAll("fabric")
  requires("fabric-api")
  requires("adventure-platform-mod")
}
