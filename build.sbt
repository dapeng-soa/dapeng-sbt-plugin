import sbt.internal.util.complete.Parser

name := "sbt-dapeng"

version := "0.5-SNAPSHOT"

scalaVersion := "2.12.2"

sbtPlugin := true

organization := "com.github.dapeng"

resolvers += Resolver.mavenLocal

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")


libraryDependencies ++= Seq(
  "com.github.dapeng" % "dapeng-code-generator" % "2.0.1-SNAPSHOT" exclude("javax.servlet", "servlet-api"),
  "com.github.dapeng" % "dapeng-container-impl"% "2.0.1-SNAPSHOT",
  "com.github.dapeng" % "dapeng-bootstrap" % "2.0.1-SNAPSHOT",
  "com.github.dapeng" % "dapeng-client-netty"% "2.0.1-SNAPSHOT"
)
