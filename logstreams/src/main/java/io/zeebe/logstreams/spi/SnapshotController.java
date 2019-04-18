/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package io.zeebe.logstreams.spi;

import io.zeebe.db.ZeebeDb;
import java.io.IOException;
import java.util.function.Consumer;

public interface SnapshotController extends AutoCloseable {
  /**
   * Takes a snapshot based on the given position. The position is a last processed lower bound
   * event position.
   *
   * @param lowerBoundSnapshotPosition the lower bound snapshot position
   */
  void takeSnapshot(long lowerBoundSnapshotPosition);

  /** Takes a snapshot into a temporary folder, will overwrite an existing snapshot. */
  void takeTempSnapshot();

  /**
   * A temporary snapshot is moved into a new snapshot directory and in that way marked as valid.
   * The given position is a last processed lower bound event position.
   *
   * @param lowerBoundSnapshotPosition the lower bound snapshot position
   * @throws IOException thrown if moving the snapshot fails
   */
  void moveValidSnapshot(long lowerBoundSnapshotPosition) throws IOException;

  /**
   * Replicates the latest valid snapshot. The given executor is called for each snapshot chunk in
   * the latest snapshot. The executor should execute/run the given Runnable in a specific
   * environment (e.g. ActorThread).
   *
   * @param executor executor which executed the given Runnable
   */
  void replicateLatestSnapshot(Consumer<Runnable> executor);

  /** Registers to consumes replicated snapshots. */
  void consumeReplicatedSnapshots();

  /**
   * Recovers the state from the latest snapshot and returns the lower bound snapshot position.
   *
   * @return the lower bound position related to the snapshot
   */
  long recover() throws Exception;

  ZeebeDb openDb();

  /**
   * Ensures that only the given maximum of snapshots are kept, the rest will be purged.
   *
   * @param maxSnapshotCount the maximum count of snapshots which should be kept
   */
  void ensureMaxSnapshotCount(int maxSnapshotCount) throws Exception;
}
