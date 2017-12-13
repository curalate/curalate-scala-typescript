package com.curalate.scala.typescript.configuration

import com.curalate.scala.typescript.core.ScalaModel.TypeRef
import java.io.PrintStream
import scala.reflect.runtime.universe.{Type => ScalaType}

case class Config(
  emitInterfaces: Boolean = true,
  emitClasses: Boolean = false,
  optionToNullable: Boolean = true,
  optionToUndefined: Boolean = false,
  outputStream: Option[PrintStream] = None,
  typeResolver: PartialFunction[ScalaType, TypeRef] = PartialFunction.empty
)