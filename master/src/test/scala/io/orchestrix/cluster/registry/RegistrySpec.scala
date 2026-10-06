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

package io.orchestrix.cluster.registry

import akka.actor.Props
import akka.persistence.Persistence
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestProbe}
import io.orchestrix._
import io.orchestrix.cluster.config.ClusterSettings
import io.orchestrix.reflect.Artifact
import io.orchestrix.cluster.journal.OrchestrixTestJournal
import io.orchestrix.protocol.registry._
import io.orchestrix.resolver.{PureResolver, Resolver}
import io.orchestrix.testkit.OrchestrixActorClusterSuite

import scala.concurrent.duration._

object RegistrySpec {

  final val TestShellJobPackage = JobPackage.shell("echo \"hello\"")
  final val TestShellJobSpec    = JobSpec("Foo", jobPackage = TestShellJobPackage)

  final val TestJarJobPackage = JobPackage.jar(
    ArtifactId(
      "io.orchestrix",
      "orchestrix-example-jobs_2.11",
      "0.1.0"
    ),
    "io.orchestrix.examples.HelloWorldJob"
  )
  final val TestJarJobSpec = JobSpec("Bar", jobPackage = TestJarJobPackage)

  final val TestArtifact = Artifact(TestJarJobPackage.artifactId, List.empty)

}

class RegistrySpec extends OrchestrixActorClusterSuite("RegistrySpec") with ImplicitSender {
  import RegistrySpec._

  val journal = new OrchestrixTestJournal
  Persistence(system)

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    system.eventStream.subscribe(self, classOf[Registry.Signal])
  }

  override protected def afterAll(): Unit = {
    system.eventStream.unsubscribe(self)
    super.afterAll()
  }

  "The registry" should {
    val resolver = new PureResolver(TestArtifact)
    val registry = TestActorRef(
      Props(new Registry(resolver, journal))
        .withDispatcher("akka.actor.default-dispatcher")
    )

    "complete warm up process" in {
      expectMsg(5 seconds, Registry.Ready)
    }

    "return JobNotFound for non existent jobs" in {
      val jobId = JobId("bar")

      registry ! GetJob(jobId)

      expectMsg(JobNotFound(jobId))
    }

    "register shell script jobs" in {
      registry ! RegisterJob(TestShellJobSpec)

      val acceptedMsg = expectMsgType[JobAccepted]
      acceptedMsg.jobId shouldBe JobId(TestShellJobSpec)
    }

    "return the job spec for the shell script" in {
      registry ! GetJob(JobId(TestShellJobSpec))

      expectMsg(TestShellJobSpec)
    }

    "register jar jobs" in {
      registry ! RegisterJob(TestJarJobSpec)

      val acceptedMsg = expectMsgType[JobAccepted]
      acceptedMsg.jobId shouldBe JobId(TestJarJobSpec)
    }

    "return the job spec for the jar" in {
      registry ! GetJob(JobId(TestJarJobSpec))

      expectMsg(TestJarJobSpec)
    }
  }

}
