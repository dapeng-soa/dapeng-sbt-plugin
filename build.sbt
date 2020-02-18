import sbt.internal.util.complete.Parser

name := "sbt-dapeng"

version := "2.2.0-SNAPSHOT"

scalaVersion := "2.12.2"

sbtPlugin := true

organization := "com.github.dapeng-soa"

resolvers += Resolver.mavenLocal

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")


libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.36",
  "com.alibaba" % "druid" % "1.1.9",
  "org.apache.commons" % "commons-vfs2" % "2.2",
  "com.github.dapeng-soa" % "dapeng-code-generator" % "2.2.0-SNAPSHOT" exclude("javax.servlet", "servlet-api"),
  "com.github.dapeng-soa" % "dapeng-container-impl" % "2.2.0-SNAPSHOT",
  "com.github.dapeng-soa" % "dapeng-bootstrap" % "2.2.0-SNAPSHOT",
  "com.github.dapeng-soa" % "dapeng-client-netty" % "2.2.0-SNAPSHOT"
)
