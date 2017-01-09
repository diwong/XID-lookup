package XID

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json.DefaultJsonProtocol

import scala.io.StdIn
//import java.util.UUID

import akka.http.scaladsl.marshalling.ToResponseMarshallable

//class EntryRecord(val tenant: String, val SSId:  String, val XId: UUID, val Id: UUID)

case class CreateRequest(tenant: String, SSId: String, Id: String)
case class FindIdRequest(tenant: String, XId: String)
case class FindXIdRequest(tenant: String, SSId: String)

trait Protocols extends DefaultJsonProtocol {
  //TODO - following results in compiling error of "could not find implicit value for parameter of UUID type]
  // need to provide conversion methods for UUID <-> String
  implicit val EntryRecordFormat = jsonFormat4(EntryRecord.apply)
  implicit val createRequestFormat = jsonFormat3(CreateRequest.apply)
  implicit val findIdRequestFormat = jsonFormat2(FindIdRequest.apply)
  implicit val findXIdRequestFormat = jsonFormat2(FindXIdRequest.apply)
}

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  def doCreate(createRequest: CreateRequest): Future[Either[String, EntryRecord]] = {
    println(s"---> doCreate")

    val tenant = createRequest.tenant
    val SSId = createRequest.SSId
    //val Id = UUID.fromString(createRequest.Id)
    val Id = createRequest.Id
    val entry = model.Add(tenant, SSId, Id)
    //TODO: add logic to handle result
    // how to convert from EntryRecord -> Future[Either[String, EntryRecord]]
    Unmarshal(entry).to[EntryRecord].map(Right(_))
  }

  def doFindId(findRequest: FindIdRequest): Future[Either[String, EntryRecord]] = {
    println(s"---> doFindId")

    val tenant = findRequest.tenant
    //val XId = UUID.fromString(findRequest.XId)
    val XId = findRequest.XId
    val entry = model.FindId(tenant, XId)
    Unmarshal(entry).to[EntryRecord].map(Right(_))
  }

  def doFindXId(findXIdRequest: FindXIdRequest): Future[Either[String, EntryRecord]] = {
    println(s"---> doFindXId")

    val tenant = findXIdRequest.tenant
    val SSId = findXIdRequest.SSId
    val entry = model.FindXId(tenant, SSId)
    Unmarshal(entry).to[EntryRecord].map(Right(_))
  }

  val routes = {
    logRequestResult("my-service") {
      path("health") {
        get {
          complete(OK, "Everything is great!")
        }
      } ~
      path("ID") {
        // find internal id given tenant and external id
        (get & entity(as[FindIdRequest])) { findIdRequest =>
          complete {
            doFindId(findIdRequest).map[ToResponseMarshallable] {
              case Right(entryRecord) => entryRecord
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      } ~
      pathPrefix("XID") {
        // find external id given tenant and source system id
        (get & entity(as[FindXIdRequest])) { findXIdRequest =>
          complete {
            doFindXId(findXIdRequest).map[ToResponseMarshallable] {
              case Right(entryRecord) => entryRecord
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        } ~
        // add new entry
        (post & entity(as[CreateRequest])) { createRequest =>
          complete {
            doCreate(createRequest).map[ToResponseMarshallable] {
              case Right(entryRecord) => entryRecord
              case Left(errorMessage) => BadRequest -> errorMessage
            }
          }
        }
      }
    }
  }
}

object MyService extends App with Service {
  override implicit val system = ActorSystem("rest-server")
  override implicit val executor = system.dispatcher //dw - what is purpose?
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  val host = config.getString("http.interface")
  val port = config.getInt("http.port")
  override val logger = Logging(system, getClass)

  //Startup and listen for requests
  val bindingFuture = Http().bindAndHandle(routes, host, port)
  println(s"Waiting for requests at http://$host:$port/...\nHit RETURN to terminate")
  StdIn.readLine()

  //Shutdown
  bindingFuture.flatMap(_.unbind())
  system.terminate()
}
