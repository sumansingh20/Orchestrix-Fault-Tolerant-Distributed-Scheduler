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

package io.orchestrix.cluster

import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.ImplicitSender

import com.typesafe.config.ConfigFactory

import io.orchestrix.ArtifactId
import io.orchestrix.cluster.config.ClusterSettings
import io.orchestrix.cluster.core.OrchestrixGuardian
import io.orchestrix.cluster.journal.OrchestrixTestJournal
import io.orchestrix.multijvm.MultiNodeClusterSpec
import io.orchestrix.protocol.client._
import io.orchestrix.testkit.ImplicitClock

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.util.Success

/**
 * Created by domingueza on 26/08/15.
 */
object OrchestrixNodesConfig extends MultiNodeConfig {
  val scheduler = role(OrchestrixRoles.Scheduler)
  val registry  = role(OrchestrixRoles.Registry)

  commonConfig(debugConfig(on = false))

  nodeConfig(scheduler)(
    ConfigFactory.parseString("akka.cluster.roles=[scheduler]"),
    MultiNodeClusterSpec.clusterConfig
  )
  nodeConfig(registry)(
    ConfigFactory.parseString("akka.cluster.roles=[registry]"),
    MultiNodeClusterSpec.clusterConfig
  )
}

class OrchestrixMultiNodeClusterSpecMultiJvmNode1 extends OrchestrixMultiNodeCluster
class OrchestrixMultiNodeClusterSpecMultiJvmNode2 extends OrchestrixMultiNodeCluster

object OrchestrixMultiNodeCluster {

  val GuardianName = "orchestrix"
  val TestArtifactId = ArtifactId("io.orchestrix", "orchestrix-example-jobs_2.11", "0.1.0")

}

abstract class OrchestrixMultiNodeCluster extends MultiNodeSpec(OrchestrixNodesConfig) with ImplicitSender
  with MultiNodeClusterSpec with ImplicitClock {

  import OrchestrixNodesConfig._
  import OrchestrixMultiNodeCluster._

  implicit val materializer = ActorMaterializer()
  val journal = new OrchestrixTestJournal

  Persistence(system)

  "A Orchestrix cluster" must {
    val Success(settings) = ClusterSettings(system.settings.config)

    "send connect commands from one node to the other one" in {
      awaitClusterUp(registry, scheduler)

      runOn(registry) {
        val bootPromise = Promise[Unit]
        system.actorOf(OrchestrixGuardian.props(settings, journal, bootPromise), GuardianName)
        Await.ready(bootPromise.future, 5 seconds)

        enterBarrier("deployed")

        val schedulerGuardian = system.actorSelection(node(scheduler) / "user" / GuardianName)
        schedulerGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")

        schedulerGuardian ! Disconnect
        expectMsg(Disconnected)

        enterBarrier("disconnected")
      }

      runOn(scheduler) {
        val bootPromise = Promise[Unit]
        system.actorOf(OrchestrixGuardian.props(settings, journal, bootPromise), GuardianName)
        Await.ready(bootPromise.future, 5 seconds)

        enterBarrier("deployed")

        val registryGuardian = system.actorSelection(node(registry) / "user" / GuardianName)
        registryGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")

        registryGuardian ! Disconnect
        expectMsg(Disconnected)

        enterBarrier("disconnected")
      }

      enterBarrier("finished")
    }

  }

}
