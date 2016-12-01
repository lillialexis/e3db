//
// build.sbt
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

name := "e3db-client-examples"
organization := "com.tozny.e3db"

autoScalaLibrary := false
crossPaths := false
mainClass in (Compile, run) := Some("com.tozny.e3db.examples.ListRecords")
resolvers += "tozny" at "https://maven.tozny.com/repo"

libraryDependencies ++= Seq(
  "com.tozny.e3db" % "e3db-client" % "0.7.0",
  "org.slf4j" % "slf4j-nop" % "1.7.21",
  "com.github.memoizr" % "retro-optional" % "0.2.0"
)
