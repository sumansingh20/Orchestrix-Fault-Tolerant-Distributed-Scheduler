import sbt._
import sbt.Keys._

import com.lightbend.sbt.SbtAspectj.autoImport.Aspectj
import com.lightbend.sbt.AspectjKeys._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.{JavaAppPackaging, JavaServerAppPackaging}
import com.typesafe.sbt.packager.docker.Cmd
import com.typesafe.sbt.packager.docker.DockerPlugin

trait OrchestrixPackagerKeys {
  val extraJvmParams = settingKey[Seq[String]]("Extra JVM parameters")
}
object OrchestrixPackagerKeys extends OrchestrixPackagerKeys

abstract class OrchestrixPackager extends AutoPlugin {

  protected val linuxHomeLocation = "/opt/orchestrix"

  override def requires: Plugins = DockerPlugin

  protected val defaultJvmParams = Seq(
    "-Dconfig.file=${app_home}/../conf/application.conf",
    "-Dlog4j.configurationFile=${app_home}/../conf/log4j2.xml"
  )

  protected lazy val defaultPackagingSettings: Seq[Def.Setting[_]] = Seq(
    OrchestrixPackagerKeys.extraJvmParams := defaultJvmParams,
    bashScriptExtraDefines ++= OrchestrixPackagerKeys.extraJvmParams.value.map(p => s"""addJava "$p""""),
    maintainer in Docker := "A. Alonso Dominguez",
    dockerRepository := Some("orchestrix"),
    dockerUpdateLatest := true,
    dockerExposedVolumes := Seq(
      s"$linuxHomeLocation/conf"
    ),
    defaultLinuxInstallLocation in Docker := linuxHomeLocation,
    dockerCommands ++= Seq(
      Cmd("ENV", "ORCHESTRIX_HOME", linuxHomeLocation)
    ),
    packageName := name.value,
    packageName in Universal := s"${moduleName.value}-${version.value}",
    executableScriptName := moduleName.value
  )

}

object OrchestrixAppPackager extends OrchestrixPackager {

  val autoImport = OrchestrixPackagerKeys

  override def requires: Plugins = super.requires && JavaAppPackaging

  override lazy val projectSettings = defaultPackagingSettings

}

object OrchestrixServerPackager extends OrchestrixPackager {
  import OrchestrixAppKeys._

  val autoImport = OrchestrixPackagerKeys
  import autoImport._

  override def requires: Plugins = super.requires && JavaServerAppPackaging && OrchestrixApp

  override lazy val projectSettings = defaultPackagingSettings ++ Seq(
    mappings in Universal ++= Seq(
      (aspectjWeaver in Aspectj).value.get -> "bin/aspectjweaver.jar",
      sigarLoader.value                    -> "bin/sigar-loader.jar"
    ),
    extraJvmParams := defaultJvmParams ++ Seq(
      "-javaagent:${app_home}/aspectjweaver.jar",
      "-javaagent:${app_home}/sigar-loader.jar"
    ),
    dockerExposedVolumes ++= Seq(
      s"$linuxHomeLocation/resolver/cache",
      s"$linuxHomeLocation/resolver/local"
    )
  )

}