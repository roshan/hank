/**
 *  Copyright 2011 LiveRamp
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.liveramp.hank.test;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Level;
import org.junit.Before;

import com.liveramp.hank.coordinator.Host;
import com.liveramp.hank.coordinator.HostCommand;
import com.liveramp.hank.coordinator.HostState;
import com.liveramp.hank.util.Condition;
import com.liveramp.hank.util.FsUtils;
import com.liveramp.hank.util.WaitUntil;

public abstract class BaseTestCase {
  static {
    org.apache.log4j.Logger.getRootLogger().setLevel(Level.ALL);
  }

  protected final String localTmpDir = System.getProperty("tmpDir", "/tmp/hank_local_file_system");

  @Before
  public final void setUpBase() throws Exception {
    FsUtils.rmrf(localTmpDir);
    new File(localTmpDir).mkdirs();
  }

  protected void waitUntilHost(final HostState state, final Host host) throws InterruptedException {
    WaitUntil.orDie(new Condition() {
      @Override
      public boolean test() {
        try {
          return state.equals(host.getState());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  protected void waitUntilCommand(final HostCommand command, final Host host) throws InterruptedException {
    WaitUntil.orDie(new Condition() {
      @Override
      public boolean test() {
        try {
          return command.equals(host.getCurrentCommand());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }
}
