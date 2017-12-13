logLevel := Level.Warn

resolvers ++= Seq(
  "Curalate Ivy" at "https://maven.curalate.com/content/groups/ivy",
  "Curalate Maven" at "https://maven.curalate.com/content/groups/omnibus"
)

credentials += Credentials(Path.userHome / ".sbt" / "credentials")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
