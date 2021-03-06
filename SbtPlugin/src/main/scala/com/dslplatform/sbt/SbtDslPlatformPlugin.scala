package com.dslplatform.sbt

import sbt._
import Keys._
import com.dslplatform.compiler.client.parameters.Targets
import com.dslplatform.compiler.client.parameters.Settings
import sbt.complete.Parsers

import scala.collection.mutable.ArrayBuffer

object SbtDslPlatformPlugin extends AutoPlugin {

  object autoImport {
    val dslLibrary = inputKey[Unit]("Compile DSL into a compiled jar ready for usage.")
    val dslSource = inputKey[Seq[File]]("Compile DSL into generated source ready for usage.")
    val dslMigrate = inputKey[Unit]("Create an SQL migration file based on difference from DSL in project and in the target database.")
    val dslExecute = inputKey[Unit]("Execute custom DSL compiler command")

    val dslLibraries = settingKey[Map[Targets.Option, File]]("Compile libraries to specified outputs)")
    val dslSources = settingKey[Map[Targets.Option, File]]("Generate sources to specified folders)")
    val dslCompiler = settingKey[String]("Path to custom dsl-compiler.exe or port to running instance (requires .NET/Mono) ")
    val dslPostgres = settingKey[String]("JDBC-like connection string to the Postgres database")
    val dslOracle = settingKey[String]("JDBC-like connection string to the Oracle database")
    val dslApplyMigration = settingKey[Boolean]("Apply SQL migration directly to the database")
    val dslNamespace = settingKey[String]("Root namespace for target language")
    val dslSettings = settingKey[Seq[Settings.Option]]("Additional compilation settings")
    val dslDslPath = settingKey[File]("Path to DSL folder")
    val dslDependencies = settingKey[Map[Targets.Option, File]]("Library compilation requires various dependencies. Customize default paths to dependencies")
    val dslSqlPath = settingKey[File]("Output folder for SQL scripts")
    val dslLatest = settingKey[Boolean]("Check for latest versions (dsl-compiler, libraries, etc...)")
    val dslForce = settingKey[Boolean]("Force actions without prompt (destructive migrations, missing folders, etc...)")
    val dslPlugins = settingKey[Option[File]]("Path to additional DSL plugins")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    dslLibrary := {
      val args = Parsers.spaceDelimited("<arg>").parsed
      def compile(dslTarget: Targets.Option, targetPath: File, targetDeps: Option[File]): Unit = {
        Actions.compileLibrary(
          streams.value.log,
          dslTarget,
          targetPath,
          dslDslPath.value,
          dslPlugins.value,
          dslCompiler.value,
          dslNamespace.value,
          dslSettings.value,
          targetDeps,
          dslLatest.value)
      }
      if (args.isEmpty) {
        if (dslLibraries.value.isEmpty) throw new RuntimeException("""dslLibraries is empty.
Either define dslLibraries in build.sbt or provide target argument (eg. revenj.scala).
Usage example: dslLibrary revenj.scala path_to_jar""")
        dslLibraries.value foreach { case (targetArg, targetOutput) =>
          val targetDeps = dslDependencies.value.get(targetArg)
          compile(targetArg, targetOutput, targetDeps)
        }
      } else if (args.length > 2) {
        throw new RuntimeException("Too many arguments. Usage example: dslLibrary revenj.scala path_to_jar")
      } else {
        val targetArg = Actions.findTarget(streams.value.log, args.head)
        val predefinedOutput = dslLibraries.value.get(targetArg)
        if (args.length == 1 && predefinedOutput.isEmpty) {
          throw new RuntimeException("dslLibraries does not contain definition for " + targetArg + """.
Either define it in dslLibraries or provide explicit output path.
Example: dslLibrary revenj.scala path_to_jar""")
        }
        val targetOutput = if (args.length == 2) new File(args.last) else predefinedOutput.get
        val targetDeps = dslDependencies.value.get(targetArg)
        compile(targetArg, targetOutput, targetDeps)
      }
    },
    dslSource := {
      val args = Parsers.spaceDelimited("<arg>").parsed
      def generate(dslTarget: Targets.Option, targetPath: File): Seq[File] = {
        Actions.generateSource(
          streams.value.log,
          dslTarget,
          targetPath,
          dslDslPath.value,
          dslPlugins.value,
          dslCompiler.value,
          dslNamespace.value,
          dslSettings.value,
          dslLatest.value)
      }
      val buffer = new ArrayBuffer[File]()
      if (args.isEmpty) {
        if (dslSources.value.isEmpty) throw new RuntimeException("""dslSources is empty.
Either define dslSources in build.sbt or provide target argument (eg. revenj.scala).
Usage example: dslSource revenj.scala path_to_folder""")
        dslSources.value foreach { case (targetArg, targetOutput) =>
          buffer ++= generate(targetArg, targetOutput)
        }
      } else if (args.length > 2) {
        throw new RuntimeException("Too many arguments. Usage example: dslSource revenj.scala path_to_folder")
      } else {
        val targetArg = Actions.findTarget(streams.value.log, args.head)
        val predefinedOutput = dslSources.value.get(targetArg)
        if (args.length == 1 && predefinedOutput.isEmpty) {
          throw new RuntimeException("dslSources does not contain definition for " + targetArg + """.
Either define it in dslSources or provide explicit output path.
Example: dslLibrary revenj.scala path_to_folder""")
        }
        val targetOutput = if (args.length == 2) new File(args.last) else predefinedOutput.get
        buffer ++= generate(targetArg, targetOutput)
      }
      buffer
    },
    dslMigrate := {
      def migrate(pg: Boolean, jdbc: String): Unit = {
        Actions.dbMigration(
          streams.value.log,
          jdbc,
          pg,
          dslSqlPath.value,
          dslDslPath.value,
          dslPlugins.value,
          dslCompiler.value,
          dslApplyMigration.value,
          dslForce.value,
          dslLatest.value)
      }
      if (dslPostgres.value.nonEmpty) {
        migrate(pg = true, dslPostgres.value)
      }
      if (dslOracle.value.nonEmpty) {
        migrate(pg = false, dslOracle.value)
      } else if (dslPostgres.value.isEmpty) {
        streams.value.log.error("Jdbc connection string not defined for Postgres or Oracle")
      }
    },
    dslExecute := {
      val args = Parsers.spaceDelimited("<arg>").parsed
      Actions.execute(
        streams.value.log,
        dslDslPath.value,
        dslPlugins.value,
        dslCompiler.value,
        args)
    },

    dslLibraries := Map.empty,
    dslSources := Map.empty,
    dslCompiler := "",
    dslPostgres := "",
    dslOracle := "",
    dslApplyMigration := false,
    dslNamespace := "",
    dslSettings := Nil,
    dslDslPath := baseDirectory.value / "dsl",
    dslDependencies := Map.empty,
    dslSqlPath := baseDirectory.value / "sql",
    dslLatest := true,
    dslForce := false,
    dslPlugins := Some(baseDirectory.value)
  )
}
