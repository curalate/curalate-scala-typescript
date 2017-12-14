import sbt._
import sbt.Keys._

object BuildConfig {
  object Dependencies {
    val testDeps = Seq(
      "org.scalatest" %% "scalatest" % versions.scalatest,
      "org.mockito" % "mockito-all" % versions.mockito
    ).map(_ % "test")
  }

  object Revision {
    lazy val revision = System.getProperty("revision", "SNAPSHOT")
  }

  object versions {
    val mockito = "1.10.19"
    val scalatest = "3.0.1"
  }

  def commonSettings(currentVersion: String) = {
    Seq(
      organization := "com.curalate",

      version := s"${currentVersion}-${BuildConfig.Revision.revision}",

      credentials += Credentials(Path.userHome / ".sbt" / "credentials"),

      scalaVersion := "2.12.4",

      crossScalaVersions := Seq("2.10.6", "2.11.8", scalaVersion.value),

      resolvers ++= Seq(
        "Curalate Ivy" at "https://maven.curalate.com/content/groups/ivy",
        "Curalate Maven" at "https://maven.curalate.com/content/groups/omnibus"
      ),

      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:experimental.macros",
        "-unchecked",
        "-Ywarn-nullary-unit",
        //"-Xfatal-warnings",
        "-Ywarn-dead-code",
        "-Xfuture"
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => Seq("-Xlint:-unused")
        case _ => Seq("-Xlint")
      }),

      scalacOptions in doc := scalacOptions.value.filterNot(_ == "-Xfatal-warnings"),   

      publishMavenStyle := true,

      publishTo in Global := {
        val nexus = "https://maven.curalate.com/"
        if (isSnapshot.value)
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "content/repositories/releases")
      }
    )
  }
}
