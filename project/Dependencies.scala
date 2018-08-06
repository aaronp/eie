import sbt._

object Dependencies {

  //https://github.com/typesafehub/scala-logging
  val logging = "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

  val testDependencies = List(
    "org.scalactic" %% "scalactic" % "3.0.4" % "test",
    "org.scalatest" %% "scalatest" % "3.0.4" % "test",
    "org.pegdown" % "pegdown" % "1.6.0" % "test",
    "junit" % "junit" % "4.12" % "test"
  )

  val simulacrum = "com.github.mpilquist" %% "simulacrum" % "0.12.0"

  val IO = simulacrum :: logging :: testDependencies

}
