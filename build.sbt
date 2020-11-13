ThisBuild / scalaVersion := "2.13.3"
ThisBuild / organization := "com.propensive"
ThisBuild / organizationName := "Propensive OÜ"
ThisBuild / organizationHomepage := Some(url("https://propensive.com/"))
ThisBuild / version := "0.5.0"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/propensive/mercator"),
    "scm:git@github.com:propensive/mercator.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "propensive",
    name  = "Jon Pretty",
    email = "jon.pretty@propensive.com",
    url   = url("https://twitter.com/propensive")
  )
)

ThisBuild / description := "Automatic creation of typeclass evidence for monad-like types"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/propensive/mercator"))

ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ThisBuild / publishMavenStyle := true

lazy val core = (project in file(".sbt/core"))
  .settings(
    name := "mercator-core",
    Compile / scalaSource := baseDirectory.value / ".." / ".." / "src" / "core",
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided
  )

lazy val test = (project in file(".sbt/test"))
  .dependsOn(core)
  .settings(
    name := "mercator-test",
    scalacOptions ++= Seq("-Xexperimental", "-Xfuture"),
    Compile / scalaSource := baseDirectory.value / ".." / ".." / "src" / "test",
    libraryDependencies += "com.propensive" %% "probably-cli" % "0.5.0",
    libraryDependencies += "com.propensive" %% "contextual-examples" % "2.0.0"
  )
