/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.orchestrix.console.boot

import io.orchestrix.{Info, Logo}
import io.orchestrix.console.dashboard.{ClusterView, DashboardPage}
import io.orchestrix.console.layout._
import io.orchestrix.console.log
import io.orchestrix.console.registry.RegistryPage
import io.orchestrix.console.scheduler.SchedulerPage
import io.orchestrix.console.security.LoginPage

import org.scalajs.dom

import slogging.StrictLogging

import scalacss.internal.mutable.GlobalRegistry

object Boot extends StrictLogging {
  import CssSettings._

  private[this] def inlineStyles() = {
    GlobalRegistry.register(
      LoginPage.Style,
      Footer.Style,
      log.LogDisplay.Style,
      DashboardPage.Style,
      ClusterView.Style,
      RegistryPage.Style,
      SchedulerPage.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument)
  }

  private[this] def setupUILogger(): Unit = {
    import slogging._

    LoggerConfig.factory = ConsoleLoggerFactory()
    LoggerConfig.level = LogLevel.DEBUG
  }

  def main(args: Array[String]): Unit = {
    setupUILogger()

    GlobalStyles.addToDocument()
    inlineStyles()

    logger.info(s"Starting Orchestrix Console ${Info.version}...\n" + Logo)

    val container = dom.document.getElementById("viewport")
    ConsoleApp(log.LogLevel.Info).renderIntoDOM(container)
  }
}
