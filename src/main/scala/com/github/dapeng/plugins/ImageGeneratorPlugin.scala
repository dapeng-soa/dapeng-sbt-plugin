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
          if(version!=null) version else "2.0.4"
        }
        from("dapengsoa/dapeng-container:"+dapengVersion)

        val containerHome = "/dapeng-container"
        run("mkdir", "-p", containerHome)

        val appDependency:Seq[File] = (fullClasspathAsJars in Compile).value.map(
          _.data
        )
        val projectName:String= name.value;
        run("mkdir","-p","/apps/"+projectName)
        run("chmod", "+x", containerHome + "/bin/startup.sh")
        workDir(containerHome + "/bin")

        copy(appDependency, containerHome + "/apps/"+projectName+"/")

        //cmd("/bin/sh", "-c", containerHome + "/bin/startup.sh && tail -F " + containerHome + "/bin/startup.sh")
        //使用此命令启动容器 可以使1号线程为应用进程，可以监控到SIGTERM信号，捕捉到该信号进行优雅的关闭容器
        entryPoint(containerHome + "/bin/startup.sh")
      }
    },

    imageNames in docker := Seq (
      ImageName(
        namespace = Some("dapengsoa/biz"),
        repository = name.value,
        tag = Some(git.gitHeadCommit.value match { case Some(tag) => tag.substring(0, 7) case None => "latest" })
      )
    )
  )


}

