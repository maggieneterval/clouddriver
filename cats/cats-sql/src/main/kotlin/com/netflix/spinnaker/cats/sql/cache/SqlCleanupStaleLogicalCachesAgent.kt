/*
 * Copyright 2019 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.cats.sql.cache

import com.netflix.spectator.api.Registry
import com.netflix.spinnaker.cats.agent.RunnableAgent
import com.netflix.spinnaker.cats.module.CatsModule
import com.netflix.spinnaker.cats.sql.SqlProviderCache
import com.netflix.spinnaker.clouddriver.cache.CustomScheduledAgent
import com.netflix.spinnaker.clouddriver.core.provider.CoreProvider
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import java.time.Clock
import java.util.concurrent.TimeUnit
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class SqlCleanupStaleLogicalCachesAgent(
  private val applicationContext: ApplicationContext,
  private val registry: Registry,
  private val clock: Clock
) : RunnableAgent, CustomScheduledAgent {

  companion object {
    private val DEFAULT_POLL_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(10)
    private val DEFAULT_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(3)
    private val MAX_LOGICAL_AGE_MILLIS = TimeUnit.MINUTES.toMillis(10)

    private val log = LoggerFactory.getLogger(SqlCleanupStaleLogicalCachesAgent::class.java)
  }

  private val countId = registry.createId("cats.sqlCache.cleanedStaleLogicalKeys.count")
  private val timeId = registry.createId("cats.sqlCache.cleanedStaleLogicalKeys.time")

  override fun run() {
    val start = clock.millis()

    val deleted = getCache().cleanLogical(MAX_LOGICAL_AGE_MILLIS)

    registry.gauge(countId).set(deleted.toDouble())
    registry.gauge(timeId).set((clock.millis() - start).toDouble())
  }

  private fun getCache(): SqlProviderCache {
    return applicationContext.getBean(CatsModule::class.java)
      .providerRegistry
      .providerCaches
      .first() as SqlProviderCache
  }

  override fun getAgentType(): String = javaClass.simpleName
  override fun getProviderName(): String = CoreProvider.PROVIDER_NAME
  override fun getPollIntervalMillis(): Long = DEFAULT_POLL_INTERVAL_MILLIS
  override fun getTimeoutMillis(): Long = DEFAULT_TIMEOUT_MILLIS
}
