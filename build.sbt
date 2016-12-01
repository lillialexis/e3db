//
// build.sbt
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

organization := "com.tozny.e3db"
version := "0.6.0"

scalaVersion := "2.11.8"
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
scalacOptions ++= Seq("-feature", "-deprecation", "-Xfatal-warnings")

enablePlugins(JavaAppPackaging)

dockerBaseImage := "tozny/java"
executableScriptName := "e3db"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.jcenterRepo
resolvers += "tozny" at "https://maven.tozny.com/repo"

libraryDependencies ++= Seq(
  // Core Libraries
  "org.scalaz" %% "scalaz-core" % "7.2.6",
  "org.scalaz" %% "scalaz-concurrent" % "7.2.6",
  "io.argonaut" %% "argonaut" % "6.2-M3",   // TODO: update once released

  // Options, Config, Logging
  "net.bmjames" %% "scala-optparse-applicative" % "0.4",
  "com.typesafe" % "config" % "1.3.0",
  "org.log4s" %% "log4s" % "1.3.0",
  "org.slf4j" % "slf4j-api" % "1.7.21",
  "ch.qos.logback" % "logback-core" % "1.0.13",
  "ch.qos.logback" % "logback-classic" % "1.0.13",

  // Cryptography
  "org.bitbucket.b_c" % "jose4j" % "0.5.2",

  // Tozny Libraries
  "com.tozny.e3db" % "e3db-client" % "0.7.0"
)

val examples = project.in(file("examples"))

