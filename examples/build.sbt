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

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
javacOptions in (Compile, doc) := Seq("-source", "1.7")

resolvers += "tozny" at "https://maven.tozny.com/repo"

libraryDependencies ++= Seq(
  "com.tozny.e3db" % "e3db-client" % "0.6.0",
  "org.slf4j" % "slf4j-nop" % "1.7.21",
  "org.glassfish" % "javax.json" % "1.0.4"
)
