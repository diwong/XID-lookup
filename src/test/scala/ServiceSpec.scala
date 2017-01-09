package XID

import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._


class ServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with Service {
  override def testConfigSource = "akka.loglevel = WARNING"
  override def config = testConfig
  override val logger = NoLogging

  val entry1 = EntryRecord("test", "S1", "X1", "I1")
  val entry2 = EntryRecord("test", "S2", "X2", "I2")
  val entry3 = EntryRecord("test", "S3", "X3", "I3")

  //TODO: create mock model

  "Service" should "respond to health check" in {
    Get(s"/health") ~> routes ~> check {
      status shouldBe OK
      responseAs[String] shouldBe "Everything is great!"
    }
  }

  it should "respond to Id query" in {
    Get(s"/ID", FindIdRequest(entry1.tenant, entry1.XId)) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[EntryRecord] shouldBe entry1
    }
  }

  it should "respond to XId query" in {
    Get(s"/XID", FindXIdRequest(entry2.tenant, entry2.SSId)) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[EntryRecord] shouldBe entry2
    }
  }

  //TODO: how to retrieve generated XId for validation
  it should "respond to create request" in {
    Post(s"/XID", CreateRequest(entry3.tenant, entry3.SSId, entry3.Id)) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[EntryRecord] shouldBe entry3
    }
  }
}
