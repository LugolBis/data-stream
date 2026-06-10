val scala3Version = "3.8.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "data-stream",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.3.2" % Test,
      "org.typelevel" %% "fs2-kafka" % "4.0.0",
      "org.http4s" %% "http4s-ember-client" % "0.23.34",
      "org.http4s" %% "http4s-circe" % "0.23.34",
      "org.http4s" %% "http4s-ember-server" % "0.23.34",
      "org.http4s" %% "http4s-dsl" % "0.23.34",
      "io.circe" %% "circe-generic" % "0.14.15",
      "io.circe" %% "circe-parser" % "0.14.15"
    ),

    assembly / assemblyJarName := "data-stream.jar",
    assembly / test := {},

    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF")  => MergeStrategy.discard
      case PathList("META-INF", "services", _*) => MergeStrategy.concat
      case PathList("META-INF", _*)             => MergeStrategy.discard
      case "reference.conf"                     => MergeStrategy.concat
      case "application.conf"                   => MergeStrategy.concat
      case x                                    =>
        val old = (assembly / assemblyMergeStrategy).value
        old(x)
    }
  )
