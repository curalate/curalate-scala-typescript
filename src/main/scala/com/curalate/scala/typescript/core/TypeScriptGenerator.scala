package com.curalate.scala.typescript.core

import com.curalate.scala.typescript.configuration.Config
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

object ClassName {
  def apply(name: String): ClassName = {
    val split = name.split("\\.")

    val className = split.last

    val ns = split.take(split.size - 1).mkString(".")

    ClassName(ns, className)
  }
}

case class ClassName(ns: String, name: String) {
  def fullName = s"$ns.$name".stripPrefix(".")
}

case class ClassType(name: ClassName, typ: Type)

object TypeScriptGenerator {
  /**
   *
   * @param javaClassNames       Classnames to generate
   * @param javaNamespaces       Namespaces to reflect on to generate
   * @param javaRecurseNamespace Whether or not to find all sub namespaces in a namespace while reflecting or just use top level
   * @param tsNamespacePrefix    The typescript namespace to prefix all declrations with
   * @param tsNamespaceMap       The map of java namespace -> typescript namespace  if you want to map the names
   * @param config
   * @param classLoader
   */
  def generateFromClassNames(
    javaClassNames: List[String],
    javaNamespaces: List[String] = Nil,
    javaRecurseNamespace: Boolean = true,
    tsUseNamespace: Boolean = true,
    tsNamespacePrefix: Option[String] = None,
    tsNamespaceMap: Map[String, String] = Map.empty,
    config: Config = Config(),
    classLoader: ClassLoader = getClass.getClassLoader
  ) = {
    implicit val mirror = runtimeMirror(classLoader)

    val allClasses = javaNamespaces.flatMap(ns => {
      val types = new Reflections(ClasspathHelper.forPackage(ns, classLoader), new SubTypesScanner(false)).getAllTypes.asScala

      if (!javaRecurseNamespace) {
        types.filter(s => ClassName(s).ns == ns)
      } else {
        types
      }
    }) ++ javaClassNames

    val allTypes = allClasses map { className =>
      ClassType(ClassName(className), mirror.staticClass(className).toType)
    }

    generate(allTypes, tsUseNamespace, tsNamespacePrefix.getOrElse(""), tsNamespaceMap)(config)
  }

  def generate(types: List[ClassType], tsUseNamespace: Boolean, nsPrefix: String, namespace: Map[String, String])(implicit config: Config) = {
    val outputStream = config.outputStream.getOrElse(Console.out)

    val scalaCaseClasses = new ScalaParser(config).parseCaseClasses(types.map(_.typ))

    TypeScriptEmitter.emitLeader(outputStream)

    Compiler.compile(scalaCaseClasses).groupBy(_.namespace).foreach {
      case (detectedNs, declarations) =>
        // swap the detected namespace for the mapped namespace
        val mappedNamespace = namespace.keys.find(key => {
          detectedNs.startsWith(key)
        }).map(key => {
          detectedNs.replace(key, namespace(key)).stripPrefix(".").stripSuffix(".")
        }).getOrElse(detectedNs)

        val typescriptNamespace = s"$nsPrefix.$mappedNamespace".stripPrefix(".").stripSuffix(".")

        TypeScriptEmitter.emit(
          declarations,
          outputStream,
          if (tsUseNamespace && typescriptNamespace.nonEmpty) Some(typescriptNamespace) else None)
    }
  }
}
