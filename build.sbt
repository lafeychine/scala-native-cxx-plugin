val scalaNativeVersion = settingKey[String]("The version of Scala Native used for building.")

name := "scala-native-cxx-plugin"
organization := "io.github.lafeychine"

scalaVersion := "3.2.2"
scalaNativeVersion := "0.4.12"
version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
    "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
    "org.scalatest" %% "scalatest" % "3.2.15" % Test,
    "org.scala-native" %% "scala3lib_native0.4" % scalaNativeVersion.value % Test,
    "org.scala-native" % ("nscplugin_" + scalaVersion.value) % scalaNativeVersion.value % Test
)

Test / fork := true
Test / javaOptions ++= Seq(
    "-Dscalanative.cxxplugin.jar=" + (Compile / Keys.`package`).value.getAbsolutePath(),
    "-Dscalanative.nscplugin.jar=" + (Test / dependencyClasspath).value.files
        .filter(_.getName().contains("nscplugin"))
        .head
        .getAbsolutePath(),
    "-Dscalanative.runtime.cp=" + (Test / fullClasspath).value.files.map(_.getAbsolutePath()).mkString(":")
)
