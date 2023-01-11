import org.gradle.api.internal.plugins.UnixStartScriptGenerator
import org.gradle.api.internal.plugins.WindowsStartScriptGenerator

plugins {
  id("play-modular-code") version "0.1"
  application
  java
  idea
}

tasks.jar {
  val resourceMain = "$buildDir/resources/main"
  val metaInf = "$buildDir/resources/main/META-INF"
  exclude { file ->
    val path = file.file.toPath()
    path.startsWith(resourceMain) && !path.startsWith(metaInf)
  }
}

application {
  mainClass.set("play.example.game.ContainerApp")
  applicationDistribution.into("conf") {
    from("src/main/conf")
  }
  applicationDefaultJvmArgs = listOf(
    "--add-opens",
    "java.base/java.lang=ALL-UNNAMED",
    "-Djdk.attach.allowAttachSelf=true"
  )
}

tasks.startScripts {
  unixStartScriptGenerator = plugin.PlayUnixStartScriptGenerator()
  windowsStartScriptGenerator = plugin.PlayWindowsStartScriptGenerator()
}

task("createStartScripts", CreateStartScripts::class) {
  applicationName = "game"
  val generatorUnix = UnixStartScriptGenerator()
  val generatorWin = WindowsStartScriptGenerator()
  generatorUnix.template = resources.text.fromFile("unixStartScript.txt")
  generatorWin.template = resources.text.fromFile("windowsStartScript.txt")
  unixStartScriptGenerator = generatorUnix
  windowsStartScriptGenerator = generatorWin
}

dependencies {
  api(project(":common"))
  api(project(":protos"))
  api(Deps.Play.Net)
  api(Deps.Play.MongoDB)
  api(Deps.Play.Mvc)

  ksp(Deps.AutoServiceKsp)
  compileOnly(Deps.Play.CodeGen.Annotations)
  compileOnly(Deps.Play.CodeGen.Controller)
  compileOnly(Deps.Play.CodeGen.Entity)
  compileOnly(Deps.Play.CodeGen.Enumeration)
  compileOnly(Deps.Play.CodeGen.Resource)
  compileOnly(Deps.Play.CodeGen.Rpc)
  ksp(Deps.Play.CodeGen.Controller)
  ksp(Deps.Play.CodeGen.Entity)
  ksp(Deps.Play.CodeGen.Enumeration)
  ksp(Deps.Play.CodeGen.Resource)
  ksp(Deps.Play.CodeGen.Rpc)


//  compileOnly(Deps.Hibernate.ValidatorApt)
//  kapt(Deps.Hibernate.ValidatorApt)

  testImplementation(Deps.KotlinPoet.Poet)
  testImplementation(Deps.KotlinPoet.Metadata)
  testImplementation("com.tngtech.archunit:archunit:1.0.0-rc1")
}

modularCode {
  enabled = true
  annotation = "play.example.game.app.module.ModularCode"
}

kotlin {
  sourceSets.main {
    kotlin.srcDir("build/generated/ksp/main/kotlin")
  }
  sourceSets.test {
    kotlin.srcDir("build/generated/ksp/test/kotlin")
  }
}

sourceSets.main {
  resources {
    srcDir("src/main/conf")
  }
}
