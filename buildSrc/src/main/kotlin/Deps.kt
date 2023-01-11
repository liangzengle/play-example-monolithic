/**
 *
 * @author LiangZengle
 */
object Deps {

  object Akka {
    private const val Version = "2.6.20"
    const val Actor = "com.typesafe.akka:akka-actor-typed_2.13:$Version"
    const val Cluster = "com.typesafe.akka:akka-cluster-typed_2.13:$Version"

    object Serialization {
      const val Jackson = "com.typesafe.akka:akka-serialization-jackson_2.13:$Version"
      const val Kryo = "io.altoo:akka-kryo-serialization_2.13:2.4.3"
    }
  }

  object Kotlin {
    const val Version = "1.8.0"
    const val Compiler = "org.jetbrains.kotlin:kotlin-compiler-embeddable:$Version"
    const val Jvm = "org.jetbrains.kotlin:kotlin-stdlib:$Version"
    const val Reflect = "org.jetbrains.kotlin:kotlin-reflect:$Version"
    const val Gradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:$Version"
    const val GradleApi = "org.jetbrains.kotlin:kotlin-gradle-plugin-api:$Version"
    const val AllOpen = "org.jetbrains.kotlin:kotlin-allopen:$Version"
  }

  public object KotlinPoet {
    private const val Version = "1.12.0"
    const val Poet = "com.squareup:kotlinpoet:$Version"
    const val Metadata = "com.squareup:kotlinpoet-metadata:$Version"
    const val Ksp = "com.squareup:kotlinpoet-ksp:$Version"
  }

  object Ksp {
    const val Version = "${Kotlin.Version}-1.0.8"
  }

  const val AutoServiceKsp = "dev.zacsweers.autoservice:auto-service-ksp:1.0.0"

  object Play {
    const val Version = "1.0-SNAPSHOT"
    const val Akka = "me.play:play-akka:$Version"
    const val Dokka = "me.play:play-dokka:$Version"
    const val Entity = "me.play:play-entity:$Version"
    const val HttpClientKtor = "me.play:play-httpclient-ktro:$Version"
    const val MongoDB = "me.play:play-mongodb:$Version"
    const val Mvc = "me.play:play-mvc:$Version"
    const val Net = "me.play:play-net:$Version"
    const val Res = "me.play:play-res:$Version"
    const val Spring = "me.play:play-spring:$Version"

    object RSocket {
      const val Common = "me.play:play-rsocket-common:$Version"
      const val Core = "me.play:play-rsocket-core:$Version"
      const val ClientSpring = "me.play:play-rsocket-client-spring:$Version"
    }

    object CodeGen {
      const val Annotations = "me.play:play-codegen-annotations:$Version"
      const val Controller = "me.play:play-codegen-controller:$Version"
      const val Entity = "me.play:play-codegen-entity:$Version"
      const val Resource = "me.play:play-codegen-resource:$Version"
      const val Enumeration = "me.play:play-codegen-enumeration:$Version"
      const val Rpc = "me.play:play-codegen-rpc:$Version"
    }
  }

  object SpringBoot {
    const val Version = "3.0.1"
    const val Boot = "org.springframework.boot:spring-boot:${Version}"
    const val Starter = "org.springframework.boot:spring-boot-starter:${Version}"
    const val StarterLog4j2 = "org.springframework.boot:spring-boot-starter-log4j2:${Version}"
    const val StarterWebflux = "org.springframework.boot:spring-boot-starter-webflux:${Version}"
    const val StarterActuator = "org.springframework.boot:spring-boot-starter-actuator:${Version}"
    const val StarterRSocket = "org.springframework.boot:spring-boot-starter-rsocket:${Version}"
  }

  object Wire {
    const val Version = "4.4.3"
    const val Runtime = "com.squareup.wire:wire-runtime-jvm:$Version"
    const val Schema = "com.squareup.wire:wire-schema-jvm:$Version"
  }

  const val KotlinPoet4Groovy = Deps.KotlinPoet.Poet

}
