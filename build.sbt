// shadow sbt-scalajs' crossProject and CrossType until Scala.js 1.0.0 is released
import sbtcrossproject.{crossProject, CrossType}
import com.typesafe.sbt.pgp.PgpKeys.publishSigned

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("core"))
  .settings(buildSettings: _*)
  .settings(publishSettings: _*)
  .settings(scalaMacroDependencies: _*)
  .settings(moduleName := "mercator")
  .settings(
    scalaVersion := crossScalaVersions.value.head
  )
  .jvmSettings(
    crossScalaVersions := "2.12.4" :: "2.13.0-RC1" :: "2.11.12" :: Nil
  )
  .jsSettings(
    crossScalaVersions := "2.12.4" :: "2.13.0-RC1" :: "2.11.12" :: Nil
  )
  .nativeSettings(
    crossScalaVersions := "2.11.12" :: Nil
  )

lazy val coreJVM = core.jvm
lazy val coreJS = core.js
lazy val coreNative = core.native

lazy val tests = project
  .in(file("tests"))
  .settings(buildSettings: _*)
  .settings(unmanagedSettings)
  .settings(moduleName := "mercator-tests")
  .settings(
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
    initialCommands in console := """import mercator.tests._; import mercator._;""",
  )
  .dependsOn(coreJVM)

lazy val root = (project in file("."))
  .aggregate(coreJVM, coreJS, coreNative, tests)
  .settings(
    publish := {},
    publishLocal := {}
  )

lazy val buildSettings = Seq(
  organization := "com.propensive",
  name := "mercator",
  version := "0.1.1",
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
  )
)

lazy val publishSettings = Seq(
  homepage := Some(url("http://propensive.com/")),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  autoAPIMappings := true,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <developers>
      <developer>
        <id>propensive</id>
        <name>Jon Pretty</name>
        <url>https://github.com/propensive/mercator/</url>
      </developer>
    </developers>
  )
)

lazy val unmanagedSettings = unmanagedBase :=
  baseDirectory.value / "lib" /
    (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => "2.11"
      case _             => "2.12"
    })

lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
)
