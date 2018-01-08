package com.github.dapeng.plugins

import java.io.FileInputStream
import java.util.Properties

import sbt._
import sbtdocker.DockerKeys.{docker, imageNames}
import sbt.Keys._
import sbtdocker.ImageName
import sbtdocker.DockerPlugin.autoImport.dockerfile
import com.typesafe.sbt.GitPlugin.autoImport._
import sbtassembly.AssemblyKeys
/**
  * Created by lihuimin on 2017/11/7.
  */
object ImageGeneratorPlugin extends AutoPlugin {

  override def requires = sbtdocker.DockerPlugin&&com.typesafe.sbt.GitPlugin&&sbtassembly.AssemblyPlugin

  override lazy val projectSettings = Seq(
    dockerfile in docker := {
      new sbtdocker.mutable.Dockerfile {
        val projectPath = (baseDirectory in Compile).value.getAbsolutePath
        val propertiesFile=new File(projectPath + "/dapeng.properties")
        val dapengVersion=if(propertiesFile.canRead){
          val properties = new Properties()
          properties.load(new FileInputStream(propertiesFile))
          val version=properties.getProperty("dapeng.version")
          if(version!=null) version else "1.2.2"
        }
        from("docker.oa.isuwang.com:5000/system/dapeng-container:"+dapengVersion)

        val containerHome = "/dapeng-container"
        run("mkdir", "-p", containerHome)

        lazy val sourceFilesPath = (baseDirectory in Compile).value.getAbsolutePath + System.getProperty("file.separator") +"docker"+ System.getProperty("file.separator") + "startup.sh"
        lazy val startupFile = new File(sourceFilesPath)

        val appDependency:Seq[File] = (fullClasspathAsJars in Compile).value.map(
          _.data
        )
        val projectName:String= name.value;
        run("mkdir","-p","/apps/"+projectName)
        copy(appDependency, containerHome + "/apps/"+projectName+"/")
        copy(startupFile, containerHome + "/bin/")
        run("chmod", "+x", containerHome + "/bin/startup.sh")
        workDir(containerHome + "/bin")

        cmd("/bin/sh", "-c", containerHome + "/bin/startup.sh && tail -F " + containerHome + "/bin/startup.sh")
      }
    },

    imageNames in docker := Seq (
      ImageName(
        namespace = Some("docker.oa.isuwang.com:5000/product"),
        repository = name.value,
        tag = Some(git.gitHeadCommit.value match { case Some(tag) => tag.substring(0, 7) case None => "latest" })
      )
    )
  )





}

