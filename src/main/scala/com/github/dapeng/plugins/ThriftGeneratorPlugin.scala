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
//    val sources: Seq[sbt.File] = generateFilesTask.value
    val files: Seq[File] = getFiles(new File(targetFilePath))
//    println("resource file size: " + files.size)
//    files.foreach(file => println(s" generated resource file: ${file.getAbsoluteFile}"))
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
    val scalaFileFolder = new File(targetFilePath + "/scala")

    if (needRegenerateFile(sourceFilePath: String, targetFilePath: String, resourceFilePath: String)) {
      if (!javaFileFolder.exists()) {
        println(s" java file folder does no exists. create new one: ${javaFileFolder}")
        javaFileFolder.mkdirs()
      }
      Scrooge.main(Array("-gen", "java", "-all",
        "-in", sourceFilePath,
        "-out", targetFilePath))

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

//      val newFiles = getFiles(new File(newResourcePath))

//      newFiles.foreach(f => println(s"new generatedFile: ${f.getAbsolutePath}"))

      val oldFiles = new File(targetFilePath + "/resources")
      if (oldFiles.isDirectory) oldFiles.delete()

    } else {
      println("Thrift-Generator-Plugin:  No need to regenerate source files. skip..............")
    }

    getFiles(javaFileFolder) ++ getFiles(scalaFileFolder)
  }

  private def getFiles(path: File): List[File] = {
    if (!path.isDirectory) {
      List(path)
    } else {
      path.listFiles().flatMap(i => getFiles(i)).toList
    }
  }

  /**
    * 判断是否需要重新生产源文件的逻辑:
    *
    * 1. 如果 targetFilePath 不存在  => need regen
    * 2. 如果 resourceFilePath 不存在 => need regen
    * 3. 如果 resourceFilePath 没有xml 文件 => need regen
    * 4. 如果 sourceFilePath 没有java文件或者 scala文件 => need regen
    * 5. 如果 sourceFiles 任一修改时间 > (resourceFile + targetFiles) 的时间 => need regen
    * 6. else false
    * @param sourceFilePath
    * @param targetFilePath
    * @param resourceFilePath
    */
  private def needRegenerateFile(sourceFilePath: String, targetFilePath: String, resourceFilePath: String): Boolean = {
    val sourceFolder = new File(sourceFilePath)
    val targetFileFolder = new File(targetFilePath)
    val resourceFileFolder = new File(resourceFilePath)

    val needRegenerateFile: Boolean = if (!sourceFolder.exists()) { //1.如果 targetFilePath 不存在  => need regen
      println(" sourceFolder not exists: regenerate sourceFiles............")
      true
    } else if (!resourceFileFolder.exists()) { //2.如果 resourceFilePath 不存在 => need regen
      println(" resourceFileFolder not exists: regenerate sourceFiles............")
      true
    } else if (getFiles(resourceFileFolder).filter(_.getName.endsWith(".xml")).size <= 0) { //3. 如果 resourceFilePath 没有xml 文件 => need regen
      println(" resourceFileFolder not exists xml files: regenerate sourceFiles............")
      true
    } else {
      //4. 如果 sourceFilePath 没有java文件或者 scala文件 => need regen
      val targetFiles = getFiles(targetFileFolder)
      if (!targetFiles.exists(_.getName.endsWith("scala")) || !targetFiles.exists(_.getName.endsWith("java"))) {
        println(" no java or scala file found : regenerate sourceFiles............")
        true
      } else {
        val sourceFiles = getFiles(sourceFolder)
//        sourceFiles.foreach(f => println(s" sourceFile: ${f.getName}, modifyTime: ${f.lastModified()}"))

        val generatedFiles = getFiles(resourceFileFolder) ++ getFiles(targetFileFolder)
        //5. 如果 sourceFiles 任一修改时间 > (resourceFile + targetFiles) 的时间 => need regen
        if (generatedFiles.exists(generatedFile => sourceFiles.exists(_.lastModified() > generatedFile.lastModified()))) {
          println(" thrift source files updated : regenerate sourceFiles............")
          true
        } else {
          false //6. else false
        }
      }

    }

    needRegenerateFile
  }

}
