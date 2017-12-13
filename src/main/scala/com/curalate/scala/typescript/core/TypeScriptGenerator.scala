package com.curalate.scala.typescript.core

import com.curalate.scala.typescript.configuration.Config
import org.reflections.Reflections
import scala.collection.JavaConverters._
import scala.reflect.runtime.universe._

object TypeScriptGenerator {
  def generateFromClassNames(
    classNames: List[String],
    namespaces: List[String] = Nil,
    config: Config = Config(),
    classLoader: ClassLoader = getClass.getClassLoader
  ) = {
    implicit val mirror = runtimeMirror(classLoader)

    val allClasses = namespaces.flatMap(ns => new Reflections(ns).getAllTypes().asScala) ++ classNames

    val allTypes = allClasses map { className =>
      mirror.staticClass(className).toType
    }

    generate(allTypes)(config)
  }

  def generate(caseClasses: List[Type])(implicit config: Config) = {
    val outputStream = config.outputStream.getOrElse(Console.out)
    val scalaCaseClasses = new ScalaParser(config).parseCaseClasses(caseClasses)
    val typeScriptInterfaces = Compiler.compile(scalaCaseClasses)
    TypeScriptEmitter.emit(typeScriptInterfaces, outputStream)
  }
}
