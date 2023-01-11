import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://plugins.gradle.org/m2/")
  }

  dependencies {
    classpath(Deps.Kotlin.Gradle)
  }
}

plugins {
  `java-library`
  id("com.google.devtools.ksp") version Deps.Ksp.Version apply false
}

subprojects {
  repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
  }
  apply(plugin = "kotlin")
  apply(plugin = "com.google.devtools.ksp")

  group = "me.play"
  version = "1.0-SNAPSHOT"

  repositories {
    mavenCentral()
  }

  val api by configurations
  val implementation by configurations
  val testImplementation by configurations

  dependencies {
    implementation(Deps.Kotlin.Jvm)
    testImplementation(kotlin("test"))
  }

  tasks {
    "test"(Test::class) {
      useJUnitPlatform()
    }
  }

  val javaVersion = JavaVersion.VERSION_17
  java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
  }

  val kotlinCompilerArgs = listOf(
    "-Xallow-result-return-type",
    "-XXLanguage:+InlineClasses",
    "-opt-in=kotlin.RequiresOptIn",
    "-opt-in=kotlin.ExperimentalUnsignedTypes",
    "-opt-in=kotlin.time.ExperimentalTime",
    "-opt-in=kotlin.contracts.ExperimentalContracts",
    "-opt-in=kotlin.experimental.ExperimentalTypeInference",
    "-opt-in=kotlin.io.path.ExperimentalPathApi",
    "-opt-in=kotlin.ExperimentalStdlibApi",
    "-opt-in=kotlinx.serialization.InternalSerializationApi",
    "-Xjvm-default=all",
    "-Xstring-concat=indy-with-constants",
    "-Xcontext-receivers"
  )

  tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersion.toString()
    kotlinOptions.javaParameters = true
    kotlinOptions.freeCompilerArgs = kotlinCompilerArgs
  }
}
