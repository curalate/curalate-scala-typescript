import BuildConfig.Dependencies
import sbt._

lazy val commonSettings = BuildConfig.commonSettings(currentVersion = "1.0")

commonSettings

name := "scala-typescript"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  "org.reflections" % "reflections" % "0.9.11",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
) ++ Dependencies.testDeps

lazy val showVersion = taskKey[Unit]("Show version")

showVersion := {
  println(version.value)
}

// custom alias to hook in any other custom commands
addCommandAlias("build", "; compile")
