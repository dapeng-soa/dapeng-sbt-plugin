package com.github.dapeng.plugins

import java.io.{FileWriter, PrintWriter}
import java.sql.{Connection, DriverManager}
import java.text.ParseException

import sbt.Keys.{baseDirectory, logLevel}
import sbt._
import com.github.dapeng.plugins.utils.DbGeneratorUtil._

import scala.collection.mutable

object DbGeneratorPlugin extends AutoPlugin {

  val help =
    """
      Please specific your ip, dbname, package, tableName for generated the entity..
      Default package is:
        com.isuwang.soa.scala.dbName.entity
      TableName is Optional, will generated all tableEntity if tableName not set
      For Example:
       1. generatedDbEntity 127.0.0.1 crm  com.isuwang.soa.scala.crm  crm_companies
          will generate entity like:  com.isuwang.soa.scala.crm.entity.XXX
          will generate enum like: com.isuwang.soa.scala.crm.enum.XXX

       2. generatedDbEntity 127.0.0.1 crm
          will generate entity like:  com.isuwang.soa.scala.crm.entity.XXX
          will generate enum like: com.isuwang.soa.scala.crm.enum.XXX

       3. generatedDbEntity 127.0.0.1 crm com.isuwang.soa.scala.crm.entity
          will generate entity like: com.isuwang.soa.scala.crm.entity.entity.XXX
          will generate enum like: com.isuwang.soa.scala.crm.entity.enum.XXX

      note: enum COMMENT FORMAT should be:
      Comment,EnumIndex:ChineseChars(EnglishChars);enumIndex:ChineseChars(EnglishChars);

      like:
      账户类型,1:资金账户(CAPITAL);2:贷款账号(CREDIT);3:预付账户(PREPAY);
    """.stripMargin

  val user = "root"
  val passwd = "root"

  val generatedDbEntity = inputKey[Unit]("A demo input task.")

  import complete.DefaultParsers._


  override lazy val projectSettings = Seq(
    generatedDbEntity := {
      // get the result of parsing
      val args: Seq[String] = spaceDelimited("").parsed
      if (args.isEmpty) {
        println(help)
      } else {
        println(" start to generated db entity....args: ")
        args foreach println

        val (ipAddress, db, packageName, tableName) = args.size match {
          case 0 =>
            println(help)
            ("", "", "", "")
          case 1 => println(help)
            ("", "", "", "")
          case 2 => (args(0), args(1), s"com.isuwang.soa.scala.${args(1)}", "")
          case 3 => (args(0), args(1), args(2), "")
          case 4 => (args(0), args(1), args(2), args(3))
          case _ => println(help)
            ("", "", "", "")
        }

        val baseTargetPath = (baseDirectory in Compile).value.getAbsolutePath
        val connection = connectJdbc(ipAddress, db, user, passwd)

        if (!tableName.isEmpty) {
          println(s" Found Specific tableName: ${tableName}, start to generateDbEntity..")
          generateDbClass(tableName, db, connection, packageName, baseTargetPath)
        } else {
          println(s" No specific tableName found. will generate ${db} all tables..")

          getTableNamesByDb(db, connection).foreach(item => {
            println(s" start to generated ${db}.${item} entity file...")
            generateDbClass(item, db, connection, packageName, baseTargetPath)
          })
        }
      }
    },
    logLevel in generatedDbEntity := Level.Debug
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

}



