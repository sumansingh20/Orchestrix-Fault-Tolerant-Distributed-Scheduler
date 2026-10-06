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

package io.orchestrix.client

import cats.data.ValidatedNel

import io.orchestrix._
import io.orchestrix.api.{Cluster, Registry, Scheduler}
import io.orchestrix.auth.{Credentials, Passport}
import io.orchestrix.client.core._
import io.orchestrix.net.OrchestrixState
import io.orchestrix.protocol.registry._
import io.orchestrix.protocol.scheduler._

import monix.reactive.Observable

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 10/09/2016.
  */
object OrchestrixClient {

  def apply[P <: Protocol](implicit driver: Driver[P]): OrchestrixClient[P] =
    new OrchestrixClient[P](driver)

}

final class OrchestrixClient[P <: Protocol] private[client] (driver: Driver[P])
    extends Cluster with Registry with Scheduler {
  import driver.specs._

  def channel[E](implicit magnet: ChannelMagnet[E]): Observable[E] = {
    val channelDef = magnet.resolve(driver)
    driver.openChannel[E](channelDef).run(())
  }

  // -- Security

  def authenticate(username: String, password: String)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration
  ): Future[Passport] = {
    val cmd = AnonCmd(Credentials(username, password), timeout)
    driver.invoke[AuthenticateCmd].run(cmd)
  }

  def refreshPassport(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Passport] =
    driver.invoke[RefreshPassportCmd].run(AuthCmd((), timeout, passport))

  def signOut(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Unit] =
    driver.invoke[SingOutCmd].run(AuthCmd((), timeout, passport))

  // -- Cluster

  def clusterState(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[OrchestrixState] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetClusterStateCmd].run(cmd)
  }

  // -- Registry

  def registerJob(job: JobSpec)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[ValidatedNel[OrchestrixError, JobId]] = {
    val cmd = AuthCmd(job, timeout, passport)
    driver.invoke[RegisterJobCmd].run(cmd)
  }

  def fetchJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[JobSpec]] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[GetJobCmd].run(cmd)
  }

  def fetchJobs(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(JobId, JobSpec)]] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetJobsCmd].run(cmd)
  }

  def enableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[JobNotFound, JobEnabled]] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[EnableJobCmd].run(cmd)
  }

  def disableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[JobNotFound, JobDisabled]] = {
    val cmd = AuthCmd(jobId, timeout, passport)
    driver.invoke[DisableJobCmd].run(cmd)
  }

  // -- Scheduler

  def executionPlans(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(PlanId, ExecutionPlan)]] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetPlansCmd].run(cmd)
  }

  def executionPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[ExecutionPlan]] = {
    val cmd = AuthCmd(planId, timeout, passport)
    driver.invoke[GetPlanCmd].run(cmd)
  }

  def scheduleJob(schedule: ScheduleJob)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    val cmd = AuthCmd(schedule, timeout, passport)
    driver.invoke[ScheduleJobCmd].run(cmd)
  }

  def cancelPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Either[ExecutionPlanNotFound, ExecutionPlanCancelled]] = {
    val cmd = AuthCmd(planId, timeout, passport)
    driver.invoke[CancelPlanCmd].run(cmd)
  }

  def executions(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Seq[(TaskId, TaskExecution)]] = {
    val cmd = AuthCmd((), timeout, passport)
    driver.invoke[GetExecutionsCmd].run(cmd)
  }

  def execution(taskId: TaskId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[TaskExecution]] = {
    val cmd = AuthCmd(taskId, timeout, passport)
    driver.invoke[GetExecutionCmd].run(cmd)
  }

}
