package api

case class ApiRequest(data: Int, override val f: String) extends Data

case class ApiResponse(data: String, foo: Option[Int])

trait Data {
  val f: String
}