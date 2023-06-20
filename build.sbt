import java.io.File.pathSeparator

val scalaNativeVersion = settingKey[String]("The version of Scala Native used for building.")

ThisBuild / organization := "io.github.lafeychine"

ThisBuild / scalaVersion := "3.3.0"
ThisBuild / scalaNativeVersion := "0.4.14"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val lib = project

lazy val plugin = project
    .settings(
        name := "scala-native-cxx-plugin",
        libraryDependencies ++= Seq(
            "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
            "org.scalatest" %% "scalatest" % "3.2.16" % Test,
            "org.scala-native" %% "scala3lib_native0.4" % scalaNativeVersion.value % Test,
            "org.scala-native" % ("nscplugin_" + scalaVersion.value) % scalaNativeVersion.value % Test
        ),
        Test / fork := true,
        Test / javaOptions ++= Seq(
            "-Dscalanative.cxxlib.jar=" + (lib / Compile / Keys.`package`).value.getAbsolutePath(),
            "-Dscalanative.cxxplugin.jar=" + (Compile / Keys.`package`).value.getAbsolutePath(),
            "-Dscalanative.nsclib.cp=" + (Test / fullClasspath).value.files.map(_.getAbsolutePath()).mkString(pathSeparator),
            "-Dscalanative.nscplugin.jar=" + (Test / dependencyClasspath).value.files
                .filter(_.getName().contains("nscplugin"))
                .head
                .getAbsolutePath()
        )
    )

lazy val sandbox = project
    .enablePlugins(ScalaNativePlugin)
    .settings(
        Compile / scalacOptions += "-Xplugin:" + (plugin / Compile / Keys.`package`).value.getAbsolutePath(),
        Compile / unmanagedJars += (lib / Compile / Keys.`package`).value
    )
