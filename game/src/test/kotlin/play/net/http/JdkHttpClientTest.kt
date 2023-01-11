package play.net.http

import org.junit.jupiter.api.Test
import play.util.concurrent.Future.Companion.toPlay
import play.util.http.JHttpClient
import play.util.http.JdkHttpClient
import play.util.time.Time
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.time.measureTime

internal class JdkHttpClientTest {

  private val jhttpClient = JHttpClient.newBuilder().version(java.net.http.HttpClient.Version.HTTP_1_1)
    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(2))
//      .authenticator(Authenticator.getDefault())
    .build()

  private val httpClient = JdkHttpClient(jhttpClient, Duration.ofSeconds(3))

  @Test
  fun get() {
    val n = 10000
    val startTime = Time.currentMillis()
    val cdl = CountDownLatch(n)
    for (i in 1..n) {
      httpClient.get("http://localhost:8080", mapOf()).toPlay().onComplete {
        cdl.countDown()
      }
    }
    cdl.await()
    val endTime = Time.currentMillis()
    println("耗时: ${endTime - startTime}ms")
    httpClient
  }

  @Test
  fun post() {
  }

  @Test
  fun testPost() {
  }
}
