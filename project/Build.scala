import scala.language.implicitConversions
import java.nio.file._

import sbt._
import Keys._

object CliBuild extends Build {

  private implicit def seqToClasspathDepdenency[T <% ProjectReference](ps: Seq[T]): Seq[ClasspathDependency] = ps.map(elem => {
    val proj: ProjectReference = elem
    ClasspathDependency(proj, proj.configuration)
  })

  /**
    * Set up an external dependency on the SDK project if the
    * E3DB-CLIENT-SDK environment variable or system property is defined.
    */
  val sdkProject: Seq[ClasspathDependency] = Option(System.getenv("E3DB-CLIENT-SDK"))
    .orElse(Option(System.getProperty("E3DB-CLIENT-SDK")))
    .map(Paths.get(_).toFile)
    .filter(_.isDirectory)
    .map(proj => {
      println(s"Loading SDK project from ${proj}.")
      RootProject(base = proj)
    }).toSeq

  lazy val root: Project = Project(id = "e3db-cli",
    base = file(".")
  ).dependsOn(sdkProject :_*)
}