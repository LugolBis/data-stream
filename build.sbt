val scala3Version = "3.8.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "data-stream",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,

    libraryDependencies += "org.typelevel" %% "fs2-kafka" % "4.0.0",

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
