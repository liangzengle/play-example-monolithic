dependencies {
  api(Deps.Play.Akka)
  api(Deps.Play.Res)
  api(Deps.Play.Spring)
  api(Deps.Play.RSocket.ClientSpring)

  api(Deps.Akka.Serialization.Jackson)
  api(Deps.Akka.Serialization.Kryo)

  api(Deps.SpringBoot.Starter) {
    exclude(group = "org.springframework.boot", module="spring-boot-starter-logging")
  }
  api(Deps.SpringBoot.StarterLog4j2)
}
