import Dependencies._

ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "pekko.bonz"
ThisBuild / organizationName := "pekko.bonz"
val PekkoVersion = "1.0.2"

lazy val root = (project in file("."))
  .settings(
    name := "pekko-bonz",
    libraryDependencies += munit % Test,
    libraryDependencies += "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
