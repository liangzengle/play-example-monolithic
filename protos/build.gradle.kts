//import com.google.protobuf.gradle.*

buildscript {
  dependencies {
    classpath(Deps.Play.Wire)
  }
}

plugins {
  id("com.squareup.wire") version Deps.Wire.Version
}

dependencies {
  compileOnly(Deps.Wire.Schema)
  api(Deps.Play.Wire)
  protoPath(Deps.Play.Wire)
}

sourceSets.main {
  kotlin.srcDir("build/generated/source/wire")
}

tasks.getByName("compileJava").enabled = false

wire {
  sourcePath {
    srcDir("src/main/protobuf")
  }
  custom {
    schemaHandlerFactoryClass = "play.wire.PlaySchemaHandlerFactory"
  }
}
