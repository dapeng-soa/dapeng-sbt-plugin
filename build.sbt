import sbt.internal.util.complete.Parser

name := "sbt-dapeng"

version := "2.0.5"

scalaVersion := "2.12.2"

sbtPlugin := true

organization := "com.github.dapeng-soa"

resolvers += Resolver.mavenLocal

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")


lazy val commonSettings: Seq[Setting[_]] = Seq(
  version in ThisBuild := "2.1.0",
  organization in ThisBuild := "dapeng-soa"
)

lazy val root = (project in file("."))
  .settings(
    sbtPlugin := true,
    name := "sbt-dapeng",
    licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publishMavenStyle := false,
    libraryDependencies ++= Seq(
      "mysql" % "mysql-connector-java" % "5.1.36",
      "com.alibaba" % "druid" % "1.1.9",
      "com.github.dapeng" % "dapeng-code-generator" % "2.0.5" exclude("javax.servlet", "servlet-api"),
      "com.github.dapeng" % "dapeng-container-impl"% "2.0.5",
      "com.github.dapeng" % "dapeng-bootstrap" % "2.0.5",
      "com.github.dapeng" % "dapeng-client-netty"% "2.0.5"
    ),
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Test, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := true
  )



