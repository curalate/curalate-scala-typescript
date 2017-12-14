import api.Data
import com.curalate.scala.typescript.configuration.Config
import com.curalate.scala.typescript.core.ScalaModel._
import com.curalate.scala.typescript.core._
import org.scalatest._
import scala.reflect.runtime.universe._

class GeneratorSpec extends FlatSpec with Matchers {
  "Generator" should "generate namespaces" in {
    TypeScriptGenerator.generateFromClassNames(classNames = Nil, namespaces = List("api"))
  }
}

class ScalaParserSpec extends FlatSpec with Matchers {
  "Scala parser" should "parse case class with one primitive member" in {
    val parsed = new ScalaParser().parseCaseClasses(List(TestTypes.TestClass1Type))
    val expected = CaseClass("TestClass1", List(CaseClassMember("name", StringRef)), List.empty)
    parsed should contain(expected)
  }

  it should "parse generic case class with one member" in {
    val parsed = new ScalaParser().parseCaseClasses(List(TestTypes.TestClass2Type))
    val expected = CaseClass("TestClass2", List(CaseClassMember("name", TypeParamRef("T"))), List("T"))
    parsed should contain(expected)
  }

  it should "parse generic case class with one member list of type parameter" in {
    val parsed = new ScalaParser().parseCaseClasses(List(TestTypes.TestClass3Type))
    val expected = CaseClass(
      "TestClass3",
      List(CaseClassMember("name", SeqRef(TypeParamRef("T")))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "parse generic case class with one optional member" in {
    val parsed = new ScalaParser().parseCaseClasses(List(TestTypes.TestClass5Type))
    val expected = CaseClass(
      "TestClass5",
      List(CaseClassMember("name", OptionRef(TypeParamRef("T")))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "correctly detect involved types" in {
    val parsed = new ScalaParser().parseCaseClasses(List(TestTypes.TestClass6Type))
    parsed should have length 6
  }

  it should "correctly hadle either types" in {
    val parsed = new ScalaParser().parseCaseClasses(List(TestTypes.TestClass7Type))
    val expected = CaseClass(
      "TestClass7",
      List(CaseClassMember("name", UnionRef(CaseClassRef("TestClass1", List()), CaseClassRef("TestClass1B", List())))),
      List("T")
    )
    parsed should contain(expected)
  }

  it should "handle custom type resolution" in {
    val c = Config().copy(typeResolver = {
      case x if x <:< typeOf[Data] => {
        ScalaModel.StringRef
      }
    })

    val parsed = new ScalaParser(c).parseCaseClasses(List(TestTypes.ValueTypeType))

    parsed should contain(CaseClass("WrapperType", List(CaseClassMember("data", StringRef)), Nil))
  }
}

object TestTypes {

  implicit val mirror = runtimeMirror(getClass.getClassLoader)
  val TestClass1Type = typeFromName("TestTypes.TestClass1")
  val TestClass2Type = typeFromName("TestTypes.TestClass2")
  val TestClass3Type = typeFromName("TestTypes.TestClass3")
  val TestClass4Type = typeFromName("TestTypes.TestClass4")
  val TestClass5Type = typeFromName("TestTypes.TestClass5")
  val TestClass6Type = typeFromName("TestTypes.TestClass6")
  val TestClass7Type = typeFromName("TestTypes.TestClass7")
  val ValueTypeType = typeFromName("TestTypes.WrapperType")

  private def typeFromName(name: String) = mirror.staticClass(name).toType

  case class TestClass1(name: String)

  case class TestClass1B(foo: String)

  case class TestClass2[T](name: T)

  case class TestClass3[T](name: List[T])

  case class TestClass4[T](name: TestClass3[T])

  case class TestClass5[T](name: Option[T])

  case class TestClass6[T](name: Option[TestClass5[List[Option[TestClass4[String]]]]], age: TestClass3[TestClass2[TestClass1]])

  case class TestClass7[T](name: Either[TestClass1, TestClass1B])

  case class ValueType(override val f: String) extends Data

  case class WrapperType(data: ValueType)
}
