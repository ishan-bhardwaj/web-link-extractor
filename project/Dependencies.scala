import sbt._

object Dependencies {
  val runtime: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % Versions.typesafeConfig,
    "org.slf4j" % "slf4j-api" % Versions.slf4j,
    "org.slf4j" % "slf4j-simple" % Versions.slf4j
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  )

  val all: Seq[ModuleID] = runtime ++ test
}

