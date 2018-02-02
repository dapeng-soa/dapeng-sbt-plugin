package com.github.dapeng.plugins

import com.github.dapeng.code.Scrooge
import sbt.AutoPlugin
import sbt.Keys.{resourceGenerators, _}
import sbt._

import scala.collection.immutable
import scala.io._

// ApiPlugin: idlc
// ServicePlugin: dp-dist, dp-docker, dp-run
object ThriftGeneratorPlugin extends AutoPlugin {


  val generateFiles = taskKey[Seq[java.io.File]]("generate thrift file sources")
  val resourceGenerateFiles = taskKey[Seq[java.io.File]]("generate thrift file sources")

  def generateFilesTask = Def.task {
    lazy val sourceFilesPath = (baseDirectory in Compile).value.getAbsolutePath + "/src/main/resources/thrifts"
    lazy val targetFilePath = target.value + s"/scala-${scalaBinaryVersion.value}/src_managed/main"
    lazy val resourceFilePath = target.value + s"/scala-${scalaBinaryVersion.value}/resource_managed/main"

    generateFiles(sourceFilesPath, targetFilePath, resourceFilePath)
  }

  def generateResourceFileTask = Def.task {
    lazy val targetFilePath = target.value + s"/scala-${scalaBinaryVersion.value}/resource_managed/main"
    val sources: Seq[sbt.File] = generateFilesTask.value
    val files: Seq[File] = getFiles(new File(targetFilePath))
    println("resource file size: " + files.size)
    files.foreach(file => println(s" generated resource file: ${file.getAbsoluteFile}"))
    files
  }

  override lazy val projectSettings = inConfig(Compile)(Seq(
    generateFiles := generateFilesTask.value,
    sourceGenerators += generateFiles.taskValue,
    resourceGenerators += generateResourceFileTask.taskValue,

    mappings in packageSrc ++= {
      val allGenerated: immutable.Seq[sbt.File] = generateFilesTask.value
      val javaPrefix = s"${target.value}/scala-${scalaBinaryVersion.value}/src_managed/main/java/"
      val scalaPrefix = s"${target.value}/scala-${scalaBinaryVersion.value}/src_managed/main/scala/"

      allGenerated.map { file =>
        val path = file.getAbsolutePath

        if(path.startsWith(javaPrefix)){
          (file, path.substring(javaPrefix.length))
        }
        else if(path.startsWith(scalaPrefix)){
          (file, path.substring(scalaPrefix.length))
        }
        else (file, path)
      }
    }

  ))


  def generateFiles(sourceFilePath: String, targetFilePath: String, resourceFilePath: String) = {

    println("Welcome to use generate plugin")
    val javaFileFolder = new File(targetFilePath + "/java")
    if (!javaFileFolder.exists()) {
      println(s" java file folder does no exists. create new one: ${javaFileFolder}")
      javaFileFolder.mkdirs()
    }
    Scrooge.main(Array("-gen", "java", "-all",
      "-in", sourceFilePath,
      "-out", targetFilePath))

    val scalaFileFolder = new File(targetFilePath + "/scala")
    if (!scalaFileFolder.exists()) {
      println(s" java file folder does no exists. create new one: ${scalaFileFolder}")
      scalaFileFolder.mkdirs()
    }
    Scrooge.main(Array("-gen", "scala", "-all",
      "-in", sourceFilePath,
      "-out", targetFilePath))

    val oldResourceFile = new File(s"${targetFilePath}/resources")
    val resourceFiles = getFiles(oldResourceFile)
    val newResourcePath = resourceFilePath

    resourceFiles.foreach(oldFile => {
      val newFile = new File(newResourcePath + s"/${oldFile.getName}")
      IO.copy(Traversable((oldFile, newFile)))
    })

    val newFiles = getFiles(new File(newResourcePath))

    newFiles.foreach(f => println(s"new generatedFile: ${f.getAbsolutePath}"))

    val oldFiles = new File(targetFilePath + "/resources")
    if (oldFiles.isDirectory) oldFiles.delete()

    getFiles(javaFileFolder) ++ getFiles(scalaFileFolder)
  }

  private def getFiles(path: File): List[File] = {
    if (!path.isDirectory) {
      List(path)
    } else {
      path.listFiles().flatMap(i => getFiles(i)).toList
    }
  }

}
