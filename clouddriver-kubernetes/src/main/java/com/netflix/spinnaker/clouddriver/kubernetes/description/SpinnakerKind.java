/*
 * Copyright 2019 Netflix, Inc.
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

package com.netflix.spinnaker.clouddriver.kubernetes.description;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum SpinnakerKind {
  INSTANCES("instances", false, false),
  CONFIGS("configs", false, false),
  SERVER_GROUPS("serverGroups", true, false),
  LOAD_BALANCERS("loadBalancers", false, true),
  SECURITY_GROUPS("securityGroups", false, true),
  SERVER_GROUP_MANAGERS("serverGroupManagers", true, true),
  UNCLASSIFIED("unclassified", false, false);

  private final String id;
  private final boolean hasClusterRelationship;
  private final boolean hasLogicalRelationship;

  SpinnakerKind(String id, boolean hasClusterRelationship, boolean hasLogicalRelationship) {
    this.id = id;
    // Whether the kind belongs to a Spinnaker cluster
    this.hasClusterRelationship = hasClusterRelationship;
    // Whether the kind is surfaced in the Spinnaker application UI
    this.hasLogicalRelationship = hasLogicalRelationship;
  }

  @Override
  public String toString() {
    return id;
  }

  @JsonCreator
  public static SpinnakerKind fromString(String name) {
    return Arrays.stream(values())
        .filter(k -> k.toString().equalsIgnoreCase(name))
        .findFirst()
        .orElse(UNCLASSIFIED);
  }

  public boolean hasClusterRelationship() {
    return this.hasClusterRelationship;
  }

  public boolean hasLogicalRelationship() {
    return this.hasLogicalRelationship;
  }
}
