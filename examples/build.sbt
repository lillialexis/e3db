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

libraryDependencies ++= Seq(
  "com.tozny.e3db" % "e3db-client" % "0.5.0"
)