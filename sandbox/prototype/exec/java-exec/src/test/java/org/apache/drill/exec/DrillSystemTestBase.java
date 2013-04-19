/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.drill.exec;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.util.MiniZooKeeperCluster;
import org.junit.BeforeClass;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.base.Throwables.propagate;

/**
 * Base class for Drill system tests.
 * Starts one or more Drillbits, an embedded ZooKeeper cluster and provides a configured client for testing.
 */
public class DrillSystemTestBase {

  static final Logger logger = org.slf4j.LoggerFactory.getLogger(DrillConfig.class);

  private static File testDir = new File("target/test-data");
  private static DrillConfig config;
  private static String zkUrl;
  private static int bitPort;
  private static int userPort;

  private List<Drillbit> servers;
  private MiniZooKeeperCluster zkCluster;

  @BeforeClass
  public static void setUp() throws Exception {
    config = DrillConfig.create();
    bitPort = config.getInt(ExecConstants.INITIAL_BIT_PORT);
    userPort = config.getInt(ExecConstants.INITIAL_USER_PORT);
    zkUrl = config.getString(ExecConstants.ZK_CONNECTION);
    setupTestDir();
  }

  private static void setupTestDir() {
    if (!testDir.exists()) {
      testDir.mkdirs();
    }
  }

  private DrillConfig newConfigWithDifferentPorts() {
    return new DrillConfig(config
      .withValue(ExecConstants.INITIAL_BIT_PORT, ConfigValueFactory.fromAnyRef(bitPort++))
      .withValue(ExecConstants.INITIAL_USER_PORT, ConfigValueFactory.fromAnyRef(userPort++)));
  }

  public void startCluster(int numServers) {
    try {
      ImmutableList.Builder<Drillbit> servers = ImmutableList.builder();
      for (int i = 0; i < numServers; i++) {
        DrillConfig config = newConfigWithDifferentPorts();
//        System.out.println("NEW CONFIG");
//        System.out.println(config);
        servers.add(Drillbit.start(config));
      }
      this.servers = servers.build();
    } catch (DrillbitStartupException e) {
      propagate(e);
    }
  }

  public void startZookeeper(int numServers) {
    try {
      this.zkCluster = new MiniZooKeeperCluster();
      zkCluster.setDefaultClientPort(Integer.parseInt(this.zkUrl.split(":")[1]));
      zkCluster.startup(testDir, numServers);
    } catch (IOException e) {
      propagate(e);
    } catch (InterruptedException e) {
      propagate(e);
    }
  }

  public void stopCluster() {
    if (servers != null) {
      for (Drillbit server : servers) {
        try {
          server.close();
        } catch (Exception e) {
          logger.warn("Error shutting down Drillbit", e);
        }
      }
    }
  }

  public void stopZookeeper() {
    try {
      this.zkCluster.shutdown();
    } catch (IOException e) {
      propagate(e);
    }
  }

}