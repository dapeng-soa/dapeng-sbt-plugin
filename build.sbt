import sbt.internal.util.complete.Parser

name := "sbt-dapeng"

version := "2.1.2-SNAPSHOT"

scalaVersion := "2.12.2"

sbtPlugin := true

organization := "com.github.dapeng-soa"

publishTo := Some("today-snapshots" at "http://nexus.today36524.td/repository/maven-releases/")

credentials += Credentials("Sonatype Nexus Repository Manager", "nexus.today36524.td", "central-services", "E@Z.nrW3")

//允许覆盖deploy
isSnapshot := true

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.4.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")


libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.36",
  "com.alibaba" % "druid" % "1.1.9",
  "org.apache.commons" % "commons-vfs2" % "2.2",
"com.github.dapeng-soa" % "dapeng-code-generator" % "2.1.2-SNAPSHOT" exclude("javax.servlet", "servlet-api"),
  "com.github.dapeng-soa" % "dapeng-container-impl"% "2.1.2-SNAPSHOT",
  "com.github.dapeng-soa" % "dapeng-bootstrap" % "2.1.2-SNAPSHOT",
  "com.github.dapeng-soa" % "dapeng-client-netty"% "2.1.2-SNAPSHOT"
)
