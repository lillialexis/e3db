//
// build.sbt
//
// Copyright (C) 2016, Tozny, LLC.
// All Rights Reserved.
//

name := "pds-client-examples"
organization := "com.tozny.pds"

autoScalaLibrary := false
crossPaths := false

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
javacOptions in (Compile, doc) := Seq("-source", "1.7")

libraryDependencies ++= Seq(
  "com.tozny.pds" % "pds-client" % "0.2.1-SNAPSHOT"
)
