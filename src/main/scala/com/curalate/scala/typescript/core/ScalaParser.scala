package com.curalate.scala.typescript.core

import com.curalate.scala.typescript.configuration.Config
import scala.reflect.runtime.universe._

class ScalaParser(config: Config = Config()) {

  import ScalaModel._

  def parseCaseClasses(caseClassTypes: List[Type]): List[CaseClass] = {
    caseClassTypes.flatMap(getInvolvedTypes(Set.empty))
      .filter(isCaseClass)
      .distinct
      .map(parseCaseClass)
      .distinct
  }

  private def parseCaseClass(caseClassType: Type) = {
    val relevantMemberSymbols = caseClassType.members.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }
    val typeParams = Reflection.etaExpand(caseClassType.typeConstructor) match {
      case polyType: PolyTypeApi => polyType.typeParams.map(_.name.decodedName.toString)
      case _ => List.empty[String]
    }
    val members = relevantMemberSymbols map { member =>
      val memberName = member.name.toString
      CaseClassMember(memberName, getTypeRef(member.returnType.map(Reflection.dealias), typeParams.toSet))
    }

    CaseClass(
      ClassName(caseClassType.typeSymbol.fullName),
      members.toList,
      typeParams
    )
  }

  private def getInvolvedTypes(alreadyExamined: Set[Type])(scalaType: Type): List[Type] = {
    if (!alreadyExamined.contains(scalaType) && !scalaType.typeSymbol.isParameter) {
      val relevantMemberSymbols = scalaType.members.collect {
        case m: MethodSymbol if m.isCaseAccessor => m
      }
      val memberTypes = relevantMemberSymbols.map(_.typeSignature.map(Reflection.dealias) match {
        case NullaryMethodType(resultType) => resultType
        case t => t.map(Reflection.dealias)
      }).flatMap(getInvolvedTypes(alreadyExamined + scalaType))
      val typeArgs = scalaType match {
        case t: scala.reflect.runtime.universe.TypeRef => t.args.flatMap(getInvolvedTypes(alreadyExamined + scalaType))
        case _ => List.empty
      }
      (scalaType.typeConstructor :: typeArgs ::: memberTypes.toList).filter(!_.typeSymbol.isParameter).distinct
    } else {
      List.empty
    }
  }

  private def getTypeRef(scalaType: Type, typeParams: Set[String]): TypeRef = {
    val typeName = scalaType.typeSymbol.name.toString

    if(config.typeResolver.isDefinedAt(scalaType)) {
      return config.typeResolver.apply(scalaType)
    }

    typeName match {
      case "Int" | "Byte" =>
        IntRef
      case "Long" =>
        LongRef
      case "Double" =>
        DoubleRef
      case "Boolean" =>
        BooleanRef
      case "String" =>
        StringRef
      case "List" | "Seq" | "Set" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        SeqRef(getTypeRef(innerType, typeParams))
      case "Option" =>
        val innerType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        OptionRef(getTypeRef(innerType, typeParams))
      case "LocalDate" =>
        DateRef
      case "Instant" | "Timestamp" | "LocalDateTime" | "ZonedDateTime" | "DateTime" =>
        DateTimeRef
      case typeParam if typeParams.contains(typeParam) =>
        TypeParamRef(typeParam)
      case _ if isCaseClass(scalaType) =>
        val caseClassName = scalaType.typeSymbol.name.toString
        val typeArgs = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args
        val typeArgRefs = typeArgs.map(getTypeRef(_, typeParams))
        CaseClassRef(caseClassName, typeArgRefs)
      case "Either" =>
        val innerTypeL = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        val innerTypeR = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.last
        UnionRef(getTypeRef(innerTypeL, typeParams), getTypeRef(innerTypeR, typeParams))
      case "Map" =>
        val keyType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.head
        val valueType = scalaType.asInstanceOf[scala.reflect.runtime.universe.TypeRef].args.last
        MapRef(getTypeRef(keyType, typeParams), getTypeRef(valueType, typeParams))
      case _ =>
        //println(s"type ref $typeName umkown")
        UnknownTypeRef(typeName)
    }
  }

  private def isCaseClass(scalaType: Type) =
    scalaType.members.collect({ case m: MethodSymbol if m.isCaseAccessor => m }).nonEmpty

}
