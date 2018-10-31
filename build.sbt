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


ThisBuild / version := "2.0.5"
ThisBuild / organization := "com.github.dapeng-soa"
ThisBuild / description := "this is a plugin for dapeng-soa framework"
ThisBuild / licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

lazy val root = (project in file("."))
  .settings(
    sbtPlugin := true,
    name := "sbt-dapeng",
    publishMavenStyle := false,
    bintrayRepository := "sbt-dapeng",
    bintrayOrganization in bintray := None
  )


libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.36",
  "com.alibaba" % "druid" % "1.1.9",
  "com.github.dapeng" % "dapeng-code-generator" % "2.0.5" exclude("javax.servlet", "servlet-api"),
  "com.github.dapeng" % "dapeng-container-impl"% "2.0.5",
  "com.github.dapeng" % "dapeng-bootstrap" % "2.0.5",
  "com.github.dapeng" % "dapeng-client-netty"% "2.0.5"
)
