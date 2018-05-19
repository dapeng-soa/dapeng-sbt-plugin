package com.github.dapeng.plugins

import java.io.{File, FileInputStream}
import java.sql.Connection
import java.util.Properties

import com.github.dapeng.plugins.RunContainerPlugin.loadSystemProperties
import com.github.dapeng.plugins.utils.DbGeneratorUtil._
import sbt.Keys.{baseDirectory, logLevel}
import sbt._

import collection.JavaConverters._


object DbGeneratePlugin extends AutoPlugin {

  val help =
    """
      Please specific your package, tableName for generated the entity..
      Your should specific plugin.db.name, plugin.db.url, plugin.db.user, plugin.db.password properties in dapeng.properties
      like:
       plugin.db.url=jdbc:mysql://127.0.0.1/order_db?useUnicode=true&characterEncoding=utf8
       plugin.db.user=root
       plugin.db.password=root
       plugin.db.name=order_db
      ----------------------------------------------------------------------------------------------------
      Default package is:
        com.github.dapeng.soa.scala.dbName.entity
      TableName is Optional, will generated all tableEntity if tableName not set
      For Example:
       1. > dbGenerate
           will generate entity like:  com.github.dapeng.soa.scala.crm.entity.XXX
           will generate enum like: com.github.dapeng.soa.scala.crm.enum.XXX

       1. > dbGenerate com.github.dapeng.soa.scala.crm  crm_companies
          will generate entity like:  com.github.dapeng.soa.scala.crm.entity.XXX
          will generate enum like: com.github.dapeng.soa.scala.crm.enum.XXX

       3. > dbGenerate com.github.dapeng.soa.scala.crm.entity
          will generate entity like: com.github.dapeng.soa.scala.crm.entity.entity.XXX
          will generate enum like: com.github.dapeng.soa.scala.crm.entity.enum.XXX

      note: enum COMMENT FORMAT should be:
      Comment,EnumIndex:Chars(EnglishChars);enumIndex:Chars(EnglishChars);

      like:
      账户类型,1:资金账户(CAPITAL);2:贷款账号(CREDIT);3:预付账户(PREPAY);
    """.stripMargin

  val dbGenerate = inputKey[Unit]("A demo input task.")

  import complete.DefaultParsers._


  override lazy val projectSettings = Seq(
    dbGenerate := {

      print(help)

      //load the sys properties
      val projectPath: String = (baseDirectory in Compile).value.getAbsolutePath
      loadSystemProperties(new File(projectPath + "/dapeng.properties"))


      val db = System.getProperty("plugin.db.name")
      println(s"====================generating dbName: ${db}====================================")

      // get the result of parsing
      val args: Seq[String] = spaceDelimited("").parsed
      println(" start to generated db entity....args: ")
      args foreach println

      val (packageName, tableName) = args.size match {
        case 0 =>
          (s"com.github.dapeng.soa.scala", "")
        case 1 =>
          (args(0), "")
        case 2 => (args(0), args(1))
        case _ =>
          println(help)
          ("", "")
      }

      println(s"db: ${db}, packageName: ${packageName}, tableName: ${tableName}")


      val baseTargetPath = (baseDirectory in Compile).value.getAbsolutePath
      val connection = connectJdbc

      if (connection.isDefined) {
        if (!tableName.isEmpty) {
          println(s" Found Specific tableName: ${tableName}, start to generateDbEntity..")
          generateDbClass(tableName, db, connection.get, packageName, baseTargetPath)
        } else {
          println(s" No specific tableName found. will generate ${db} all tables..")

          getTableNamesByDb(db, connection.get).foreach(item => {
            println(s" start to generated ${db}.${item} entity file...")
            generateDbClass(item, db, connection.get, packageName, baseTargetPath)
          })
        }
      } else {
        println(" Failed to connect mysql....please check your config in dapeng.properties file...")
      }

    },
    logLevel in dbGenerate := Level.Debug
  )

  def generateDbClass(tableName: String, db: String, connection: Connection, packageName: String, baseTargetPath: String): Unit = {

    val columns = getTableColumnInfos(tableName.toLowerCase, db, connection)
    val dbClassTemplate = toDbClassTemplate(tableName, packageName, columns)
    val targetPath = baseTargetPath + "/src/main/scala/" + packageName.split("\\.").mkString("/") + "/"

    generateEntityFile(dbClassTemplate, targetPath + "entity/", s"${toFirstUpperCamel(tableNameConvert(tableName))}.scala")

    columns.foreach(column => {
      generateEnumFile(tableName, column._1, column._3, targetPath + "enum/", packageName, s"${toFirstUpperCamel(tableNameConvert(tableName)) + toFirstUpperCamel(column._1)}.scala")
    })
  }


  def loadSystemProperties(file: File): Unit = {
    if (file.canRead) {
      val properties = new Properties()
      properties.load(new FileInputStream(file))

      val results = properties.keySet().asScala.map(_.toString)
      results.foreach(keyString => {
        System.setProperty(keyString, properties.getProperty(keyString))
      })
    }
  }

}



