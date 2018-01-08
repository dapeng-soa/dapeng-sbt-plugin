package com.github.dapeng.plugins

import com.github.dapeng.code.Scrooge
import sbt.AutoPlugin
import sbt.Keys._
import sbt._
import scala.io._

// ApiPlugin: idlc
// ServicePlugin: dp-dist, dp-docker, dp-run
object ThriftGeneratorPlugin extends AutoPlugin{


  val generateFiles = taskKey[Seq[java.io.File]]("generate thrift file sources")

  def generateFilesTask = Def.task {
    lazy val sourceFilesPath = (baseDirectory in Compile).value.getAbsolutePath + "/src/main/resources/thrifts"
    lazy val targetFilePath =  (baseDirectory in Compile).value.getAbsolutePath + "/src/main"


    generateFiles(sourceFilesPath,targetFilePath)

    Seq[java.io.File]()
  }

  override lazy val projectSettings = inConfig(Compile)(Seq(
    generateFiles := generateFilesTask.value,
    sourceGenerators += generateFiles.taskValue
  ))

  def generateFiles(sourceFilePath: String, targetFilePath: String) = {

    println("Welcome to use generate plugin")
    val javaFileFolder = new File(targetFilePath + "/java")
    if (!javaFileFolder.exists()) {
      println(s" java file folder does no exists. create new one: ${javaFileFolder}")
      javaFileFolder.mkdir()
    }
    Scrooge.main(Array("-gen", "java", "-all",
      "-in", sourceFilePath,
      "-out", targetFilePath))

    val scalaFileFolder = new File(targetFilePath + "/scala")
    if (!scalaFileFolder.exists()) {
      println(s" java file folder does no exists. create new one: ${scalaFileFolder}")
      scalaFileFolder.mkdir()
    }
    Scrooge.main(Array("-gen", "scala", "-all",
      "-in", sourceFilePath,
      "-out", targetFilePath))

  }

}
