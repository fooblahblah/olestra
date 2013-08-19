import sbt._
import Keys._

object OlestraBuild extends Build {

  lazy val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.fooblahblah",
    version      := "1.0.0",
    scalaVersion := "2.10.1",

    resolvers ++= Seq(
      "typesafe repo" at "http://repo.typesafe.com/typesafe/maven-releases",
      "spray repo"    at "http://repo.spray.io",
      "VictorOps Repository" at "https://raw.github.com/fooblahblah/maven-repo/master/releases"
    ),

    publishTo := Some(Resolver.file("file",  new File("../maven-repo/releases"))),

    libraryDependencies ++= Seq(
      "com.typesafe"                %  "config"                   % "1.0.0",
      "com.typesafe.akka"           %% "akka-actor"               % "2.1.4",
      "io.spray"                    %  "spray-client"             % "1.1-M6",
      "junit"                       %  "junit"                    % "4.11",
      "play"                        %% "play-json"                % "2.1.3",
      "org.apache.directory.studio" %  "org.apache.commons.codec" % "1.6",
      "org.specs2"                  %% "specs2"                   % "1.13"
    ),
    scalacOptions ++= Seq("-language:postfixOps", "-language:implicitConversions")
  )

  lazy val root = Project(id = "olestra", base = file("."), settings = buildSettings)
}
