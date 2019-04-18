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
package io.zeebe.logstreams.state;

import io.zeebe.logstreams.impl.Loggers;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/** Handles how snapshots/databases are stored on the file system. */
public class StateStorage {

  private static final Logger LOG = Loggers.ROCKSDB_LOGGER;

  private static final String DEFAULT_RUNTIME_DIRECTORY = "runtime";
  private static final String DEFAULT_SNAPSHOTS_DIRECTORY = "snapshots";
  static final String TEMP_SNAPSHOT_DIRECTORY = "tmp/";

  private final File runtimeDirectory;
  private final File snapshotsDirectory;
  private File tmpSnapshotDirectory;

  public StateStorage(final String rootDirectory) {
    this.runtimeDirectory = new File(rootDirectory, DEFAULT_RUNTIME_DIRECTORY);
    this.snapshotsDirectory = new File(rootDirectory, DEFAULT_SNAPSHOTS_DIRECTORY);
    initTempSnapshotDirectory();
  }

  public StateStorage(final File runtimeDirectory, final File snapshotsDirectory) {
    this.runtimeDirectory = runtimeDirectory;
    this.snapshotsDirectory = snapshotsDirectory;
    initTempSnapshotDirectory();
  }

  private void initTempSnapshotDirectory() {
    tmpSnapshotDirectory = new File(snapshotsDirectory, TEMP_SNAPSHOT_DIRECTORY);
  }

  public File getRuntimeDirectory() {
    return runtimeDirectory;
  }

  public File getSnapshotsDirectory() {
    return snapshotsDirectory;
  }

  public File getTmpSnapshotDirectoryFor(String position) {
    final String path = String.format("%s-%s", position, TEMP_SNAPSHOT_DIRECTORY);

    return new File(snapshotsDirectory, path);
  }

  public File getSnapshotDirectoryFor(long position) {
    final String path = String.format("%d", position);

    return new File(snapshotsDirectory, path);
  }

  public File getTempSnapshotDirectory() {
    return tmpSnapshotDirectory;
  }

  public List<File> list() {
    final File[] snapshotFolders = snapshotsDirectory.listFiles();
    final List<File> snapshots = new ArrayList<>();

    if (snapshotFolders == null || snapshotFolders.length == 0) {
      return snapshots;
    }

    for (final File folder : snapshotFolders) {
      if (folder.isDirectory()) {
        if (isNumber(folder.getName())) {
          try {
            snapshots.add(folder);
          } catch (final Exception ex) {
            LOG.warn("error listing snapshot {}", folder.getAbsolutePath(), ex);
          }
        }
      }
    }

    return snapshots;
  }

  private boolean isNumber(String name) {
    try {
      Long.parseLong(name);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public List<File> listByPositionAsc() {
    return list()
        .stream()
        .sorted(Comparator.comparingLong(f -> Long.parseLong(f.getName())))
        .collect(Collectors.toList());
  }

  public List<File> listByPositionDesc() {
    final List<File> list = listByPositionAsc();
    Collections.reverse(list);
    return list;
  }
}
