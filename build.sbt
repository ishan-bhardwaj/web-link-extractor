ThisBuild / version := Versions.project

ThisBuild / scalaVersion := Versions.scala

lazy val root = (project in file("."))
  .settings(
    name := "web-link-extractor",
    libraryDependencies ++= Dependencies.all
  )
