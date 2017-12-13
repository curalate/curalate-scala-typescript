import BuildConfig.Dependencies
import sbt._

lazy val commonSettings = BuildConfig.commonSettings(currentVersion = "1.0")

commonSettings

name := "scala-typescript"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.reflections" % "reflections" % "0.9.11",
  "com.google.code.findbugs" % "jsr305" % "1.3.+",
  "org.slf4j" % "slf4j-api" % "1.7.25" % "provided",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
) ++ Dependencies.testDeps

lazy val showVersion = taskKey[Unit]("Show version")

showVersion := {
  println(version.value)
}

// custom alias to hook in any other custom commands
addCommandAlias("build", "; compile")
