package com.github.dapeng.plugins

import com.github.dapeng.code.Scrooge
import sbt.AutoPlugin
import sbt.Keys.{resourceGenerators, _}
import sbt._

import java.io.File

// ApiPlugin: idlc
// ServicePlugin: dp-dist, dp-docker, dp-run
object ThriftGeneratorPlugin extends AutoPlugin {


  val generateFiles = taskKey[(Seq[File],Seq[File])]("generate thrift file sources")
  val resourceGenerateFiles = taskKey[Seq[java.io.File]]("generate thrift file sources")

  def generateSourceFilesTask = Def.task {
    generateFiles.value._1
  }

  def generateResourceFileTask = Def.task {
    generateFiles.value._2
  }

  def runIdlcTask = Def.task {
    val sourceFilesPath = (baseDirectory in Compile).value.getAbsolutePath + "/src/main/resources/thrifts"
    val srcManagedPath = target.value + s"/scala-${scalaBinaryVersion.value}/src_managed/main"
    val resourceManagedPath = target.value + s"/scala-${scalaBinaryVersion.value}/resource_managed/main"

    generateFiles(sourceFilesPath, srcManagedPath, resourceManagedPath)
  }

  override lazy val projectSettings = inConfig(Compile)(Seq(
    generateFiles := runIdlcTask.value,
    sourceGenerators += generateSourceFilesTask.taskValue,
    resourceGenerators += generateResourceFileTask.taskValue,
//    runIdlc := runIdlcTask.value,

    mappings in packageSrc ++= {
      val allGenerated: Seq[File] = generateSourceFilesTask.value
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


  def generateFiles(sourceFilePath: String, srcManagedPath: String, resourceManagedPath: String) = {

    println("Welcome to use generate plugin")
    val javaFileFolder = new File(srcManagedPath + "/java")
    val scalaFileFolder = new File(srcManagedPath + "/scala")

    if (needRegenerateFile(sourceFilePath: String, srcManagedPath: String, resourceManagedPath: String)) {
      if (!javaFileFolder.exists()) {
        println(s" java file folder does no exists. create new one: ${javaFileFolder}")
        javaFileFolder.mkdirs()
      }
      Scrooge.main(Array("-gen", "java", "-all",
        "-in", sourceFilePath,
        "-out", srcManagedPath))

      if (!scalaFileFolder.exists()) {
        println(s" java file folder does no exists. create new one: ${scalaFileFolder}")
        scalaFileFolder.mkdirs()
      }
      Scrooge.main(Array("-gen", "scala", "-all",
        "-in", sourceFilePath,
        "-out", srcManagedPath))

      val oldResourceFile = new File(s"${srcManagedPath}/resources")
      val resourceFiles = getFiles(oldResourceFile)
      val newResourcePath = resourceManagedPath

      resourceFiles.foreach(oldFile => {
        val newFile = new File(newResourcePath + s"/${oldFile.getName}")
        IO.copy(Traversable((oldFile, newFile)))
      })

      val oldFiles = new File(srcManagedPath + "/resources")
      if (oldFiles.isDirectory) oldFiles.delete()

    } else {
      println("Thrift-Generator-Plugin:  No need to regenerate source files. skip..............")
    }

    val sources: List[File] = (getFiles(javaFileFolder) ++ getFiles(scalaFileFolder)).filter(x => x.getName.endsWith(".scala") || x.getName.endsWith(".java"))
    val resources: List[File] = getFiles(new File(resourceManagedPath))

    (sources, resources)
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
