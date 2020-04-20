// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import com.softwaremill.PublishTravis.publishTravisSettings
import sbtcrossproject.crossProject

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .in(file("core"))
  .settings(buildSettings)
  .settings(publishSettings)
  .settings(scalaMacroDependencies)
  .settings(moduleName := "mercator")

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val tests = project
  .in(file("tests"))
  .settings(buildSettings)
  .settings(moduleName := "mercator-tests")
  .settings(initialCommands in console := """import mercator.tests._; import mercator._;""")
  .settings(publishArtifact := false)
  .dependsOn(coreJVM)

lazy val root = (project in file("."))
  .aggregate(coreJVM, coreJS, tests)
  .settings(buildSettings)
  .settings(publishSettings)
  .settings(publishTravisSettings)
  .settings(publishArtifact := false)

lazy val buildSettings = Seq(
  organization := "com.propensive",
  name := "mercator",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-Ywarn-value-discard",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
  ),
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq(
          "-Xexperimental",
          "-Xfuture",
          "-Ywarn-nullary-unit",
          "-Ywarn-inaccessible",
          "-Ywarn-adapted-args"
        )
      case _ =>
        Nil
    }
  },
  scmInfo := Some(
    ScmInfo(url("https://github.com/propensive/mercator"),
            "scm:git:git@github.com:propensive/mercator.git")
  ),
  crossScalaVersions := "2.12.8" :: "2.13.0" :: Nil,
  scalaVersion := crossScalaVersions.value.head
)

lazy val publishSettings = ossPublishSettings ++ Seq(
  homepage := Some(url("http://propensive.com/")),
  organizationHomepage := Some(url("http://propensive.com/")),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  developers := List(
    Developer(
      id = "propensive",
      name = "Jon Pretty",
      email = "",
      url = new URL("https://github.com/propensive/mercator/")
    )
  ),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/propensive/" + name.value),
      "scm:git:git@github.com/propensive/" + name.value + ".git"
    )
  ),
  sonatypeProfileName := "com.propensive",
)

lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided
)
