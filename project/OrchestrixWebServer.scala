import sbt._
import sbt.Keys._

import com.lightbend.sbt.SbtAspectj

import com.typesafe.sbt.web.Import._

import spray.revolver.RevolverPlugin

import org.irundaia.sbt.sass.SbtSassify

import play.twirl.sbt.SbtTwirl

import webscalajs.WebScalaJS

trait OrchestrixAppKeys {
  val sigarLoader = taskKey[File]("Sigar loader jar file")
  val sigarLoaderOptions = taskKey[Seq[String]]("JVM options for the Sigar loader")
}
object OrchestrixAppKeys extends OrchestrixAppKeys

object OrchestrixApp extends AutoPlugin {
  import RevolverPlugin.autoImport.reStart
  import SbtAspectj.autoImport._
  import OrchestrixAppKeys._

  override def requires: Plugins = SbtAspectj && RevolverPlugin

  lazy val defaultServerSettings: Seq[Def.Setting[_]] = Seq(
    sigarLoader := findSigarLoader(update.value),
    sigarLoaderOptions := Seq(s"-javaagent:${sigarLoader.value.getAbsolutePath}"),
    sigarLoaderOptions in Test := sigarLoaderOptions.value :+ s"-Dkamon.sigar.folder=${baseDirectory.value / "target" / "native"}",
    baseDirectory in reStart := baseDirectory.value / "target",
    aspectjVersion in Aspectj := "1.8.13",
    aspectjSourceLevel in Aspectj := "-1.8",
    javaOptions in reStart ++= (aspectjWeaverOptions in Aspectj).value ++ (sigarLoaderOptions in Test).value,
    javaOptions in Test ++= (sigarLoaderOptions in Test).value,
    parallelExecution in Test := false
  )

  override def projectSettings: Seq[Def.Setting[_]] = defaultServerSettings

  private[this] def findSigarLoader(report: UpdateReport) =
    report.matching(artifactFilter(name = "sigar-loader", `type` = "jar")).head

}

object OrchestrixWebServer extends AutoPlugin {
  import WebScalaJS.autoImport.{devCommands, scalaJSPipeline}

  override def requires: Plugins = OrchestrixApp && SbtSassify && SbtTwirl

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    devCommands in scalaJSPipeline ++= Seq("test", "testQuick", "testOnly", "docker:publishLocal")
  )

}
