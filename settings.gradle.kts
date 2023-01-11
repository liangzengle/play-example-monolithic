pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.aliyun.com/repository/public/")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://plugins.gradle.org/m2/")
  }
}

rootProject.name = "play-example-monolithic"


include("common")
include("game")
include("protos")
include("robot")
include("docgen")
