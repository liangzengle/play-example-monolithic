//import com.google.protobuf.gradle.*

plugins {
  id("com.squareup.wire") version Deps.Wire.Version
}

dependencies {
  compileOnly(Deps.Wire.Schema)
}

sourceSets.main {
  kotlin.srcDir("build/generated/source/wire")
}

tasks.getByName("compileJava").enabled = false

wire {
  sourcePath {
    srcDir("src/main/protobuf")
  }
  kotlin {}
}
