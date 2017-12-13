package com.curalate.scala.typescript.core

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

object Reflection {
  def etaExpand(typ: Type): universe.Type = {
    typ.etaExpand
  }

  def dealias(typ: Type) = typ.dealias
}
