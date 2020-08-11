/*
 * Copyright 2019 Armory
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
 *
 */

package com.netflix.spinnaker.clouddriver.kubernetes.provider.view;

import com.netflix.spinnaker.clouddriver.kubernetes.caching.agent.KubernetesCacheDataConverter;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.view.model.KubernetesV2Manifest;
import com.netflix.spinnaker.clouddriver.kubernetes.caching.view.provider.KubernetesManifestProvider;
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesKind;
import com.netflix.spinnaker.clouddriver.kubernetes.description.manifest.KubernetesManifest;
import com.netflix.spinnaker.clouddriver.kubernetes.model.KubernetesV2JobStatus;
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesSelectorList;
import com.netflix.spinnaker.clouddriver.kubernetes.security.KubernetesV2Credentials;
import com.netflix.spinnaker.clouddriver.model.JobProvider;
import com.netflix.spinnaker.clouddriver.security.AccountCredentialsProvider;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1Pod;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KubernetesV2JobProvider implements JobProvider<KubernetesV2JobStatus> {
  @Getter private final String platform = "kubernetes";
  private final AccountCredentialsProvider accountCredentialsProvider;
  private final KubernetesManifestProvider manifestProvider;

  KubernetesV2JobProvider(
      AccountCredentialsProvider accountCredentialsProvider,
      KubernetesManifestProvider manifestProvider) {
    this.accountCredentialsProvider = accountCredentialsProvider;
    this.manifestProvider = manifestProvider;
  }

  @Nullable
  public KubernetesV2JobStatus collectJob(String account, String location, String id) {
    Optional<V1Job> optionalJob = getKubernetesJob(account, location, id);
    if (!optionalJob.isPresent()) {
      return null;
    }
    V1Job job = optionalJob.get();
    KubernetesV2JobStatus jobStatus = new KubernetesV2JobStatus(job, account);
    KubernetesV2Credentials credentials =
        (KubernetesV2Credentials)
            accountCredentialsProvider.getCredentials(account).getCredentials();

    Map<String, String> selector = job.getSpec().getSelector().getMatchLabels();
    List<KubernetesManifest> pods =
        credentials.list(
            KubernetesKind.POD,
            jobStatus.getLocation(),
            KubernetesSelectorList.fromMatchLabels(selector));

    jobStatus.setPods(
        pods.stream()
            .map(
                p -> {
                  V1Pod pod = KubernetesCacheDataConverter.getResource(p, V1Pod.class);
                  return new KubernetesV2JobStatus.PodStatus(pod);
                })
            .collect(Collectors.toList()));

    return jobStatus;
  }

  @Nullable
  public Map<String, Object> getFileContents(
      String account, String location, String id, String containerName) {
    KubernetesV2Credentials credentials =
        (KubernetesV2Credentials)
            accountCredentialsProvider.getCredentials(account).getCredentials();
    return getKubernetesJob(account, location, id)
        .map(
            job -> {
              try {
                String logContents =
                    credentials.jobLogs(location, job.getMetadata().getName(), containerName);
                return PropertyParser.extractPropertiesFromLog(logContents);
              } catch (Exception e) {
                log.error("Couldn't parse properties for account {} at {}", account, location);
                return null;
              }
            })
        .orElse(null);
  }

  public void cancelJob(String account, String location, String id) {
    throw new NotImplementedException(
        "cancelJob is not implemented for the V2 Kubernetes provider");
  }

  private Optional<V1Job> getKubernetesJob(String account, String location, String id) {
    return Optional.ofNullable(manifestProvider.getManifest(account, location, id, false))
        .map(KubernetesV2Manifest::getManifest)
        .map(m -> KubernetesCacheDataConverter.getResource(m, V1Job.class));
  }
}
