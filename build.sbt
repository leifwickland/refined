lazy val refined = project.in(file("."))
  .settings(projectSettings)
  .settings(compileSettings)
  .settings(scaladocSettings)
  .settings(publishSettings)
  .settings(releaseSettings)
  .settings(siteSettings)
  .settings(miscSettings)
  .settings(styleSettings)

val tutSource = "docs/src"
val tutTarget = "docs"

lazy val docs = project
  .settings(moduleName := "refined-docs")
  .settings(projectSettings)
  .settings(compileSettings)
  .settings(noPublishSettings)
  .settings(tutSettings)
  .settings(
    tutScalacOptions := scalacOptions.value,
    tutSourceDirectory := file(tutSource)
  )
  .dependsOn(refined)

lazy val tutCopy = taskKey[Unit]("")
tutCopy := (tut in docs).value.foreach { case (compiled, name) =>
  IO.copyFile(compiled, file(s"$tutTarget/$name"))
}

val gitPubUrl = "https://github.com/fthomas/refined.git"
val gitDevUrl = "git@github.com:fthomas/refined.git"

lazy val projectSettings = Seq(
  name := "refined",
  description := "Refinement types for Scala",

  organization := "eu.timepit",
  homepage := Some(url("https://github.com/fthomas/refined")),
  startYear := Some(2015),
  licenses += "MIT" -> url("http://opensource.org/licenses/MIT"),

  scmInfo := Some(ScmInfo(homepage.value.get,
    s"scm:git:$gitPubUrl", Some(s"scm:git:$gitDevUrl")))
)

lazy val compileSettings = Seq(
  scalaVersion := "2.11.6",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:experimental.macros",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xfuture",
    "-Xlint",
    //"-Xlog-implicits",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  ),

  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    "com.chuusai" %% "shapeless" % "2.2.2",
    "org.scalacheck" %% "scalacheck" % "1.12.4" % "test"
  ),

  wartremoverErrors in (Compile, compile) ++= Warts.unsafe diff
    Seq(Wart.Any, Wart.AsInstanceOf, Wart.NonUnitStatements, Wart.Throw)
)

lazy val scaladocSettings = Seq(
  scalacOptions in (Compile, doc) ++= Seq(
    "-diagrams",
    "-diagrams-debug",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  ),

  autoAPIMappings := true,
  apiURL := Some(url("http://fthomas.github.io/refined/latest/api/"))
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  pomExtra :=
    <developers>
      <developer>
        <id>fthomas</id>
        <name>Frank S. Thomas</name>
        <url>https://github.com/fthomas</url>
      </developer>
    </developers>
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val releaseSettings = {
  import ReleaseTransformations._

  Seq(
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      releaseStepTask(GhPagesKeys.pushSite),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
}

lazy val siteSettings =
  site.settings ++
  site.includeScaladoc() ++
  ghpages.settings ++
  Seq(git.remoteRepo := gitDevUrl)

lazy val miscSettings = Seq(
  initialCommands := """
    import eu.timepit.refined._
    import eu.timepit.refined.boolean._
    import eu.timepit.refined.char._
    import eu.timepit.refined.collection._
    import eu.timepit.refined.generic._
    import eu.timepit.refined.InferenceRuleAlias._
    import eu.timepit.refined.implicits._
    import eu.timepit.refined.numeric._
    import eu.timepit.refined.string._
    import shapeless.{ ::, HList, HNil }
    import shapeless.nat._
    import shapeless.tag.@@
  """
)

lazy val styleSettings =
  scalariformSettings

addCommandAlias("validate", ";clean;coverage;test;coverageReport;scalastyle;test:scalastyle;doc;tutCopy")