/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.clustering.base.partitions;

import static io.zeebe.broker.exporter.ExporterManagerService.EXPORTER_PROCESSOR_ID;
import static io.zeebe.broker.exporter.ExporterManagerService.PROCESSOR_NAME;

import io.atomix.cluster.messaging.ClusterEventService;
import io.zeebe.broker.clustering.base.topology.PartitionInfo;
import io.zeebe.broker.exporter.stream.ExporterColumnFamilies;
import io.zeebe.broker.logstreams.ZbStreamProcessorService;
import io.zeebe.broker.logstreams.state.DefaultZeebeDbFactory;
import io.zeebe.broker.logstreams.state.StateReplication;
import io.zeebe.broker.logstreams.state.StateStorageFactory;
import io.zeebe.logstreams.log.LogStream;
import io.zeebe.logstreams.spi.SnapshotController;
import io.zeebe.logstreams.state.StateSnapshotController;
import io.zeebe.logstreams.state.StateStorage;
import io.zeebe.servicecontainer.Injector;
import io.zeebe.servicecontainer.Service;
import io.zeebe.servicecontainer.ServiceStartContext;
import io.zeebe.servicecontainer.ServiceStopContext;

/** Service representing a partition. */
public class Partition implements Service<Partition> {
  public static final String PARTITION_NAME_FORMAT = "raft-atomix-partition-%d";
  private final ClusterEventService eventService;
  private StateReplication processorStateReplication;
  private StateReplication exporterStateReplication;

  public static String getPartitionName(final int partitionId) {
    return String.format(PARTITION_NAME_FORMAT, partitionId);
  }

  private final Injector<LogStream> logStreamInjector = new Injector<>();
  private final Injector<StateStorageFactory> stateStorageFactoryInjector = new Injector<>();
  private final PartitionInfo info;
  private final RaftState state;
  private final int partitionId;

  private LogStream logStream;
  private SnapshotController exporterSnapshotController;
  private StateSnapshotController processorSnapshotController;

  public Partition(ClusterEventService eventService, PartitionInfo partitionInfo, RaftState state) {
    this.info = partitionInfo;
    this.partitionId = partitionInfo.getPartitionId();
    this.state = state;
    this.eventService = eventService;
  }

  @Override
  public void start(final ServiceStartContext startContext) {
    logStream = logStreamInjector.getValue();
    final StateStorageFactory stateStorageFactory = stateStorageFactoryInjector.getValue();

    final String exporterProcessorName = PROCESSOR_NAME;
    final StateStorage exporterStateStorage =
        stateStorageFactory.create(EXPORTER_PROCESSOR_ID, exporterProcessorName);
    exporterStateReplication =
        new StateReplication(eventService, partitionId, exporterProcessorName);
    exporterSnapshotController =
        new StateSnapshotController(
            exporterStateStorage,
            exporterStateReplication,
            DefaultZeebeDbFactory.defaultFactory(ExporterColumnFamilies.class));

    final String streamProcessorName = ZbStreamProcessorService.PROCESSOR_NAME;
    final StateStorage stateStorage = stateStorageFactory.create(partitionId, streamProcessorName);
    processorStateReplication =
        new StateReplication(eventService, partitionId, streamProcessorName);
    processorSnapshotController =
        new StateSnapshotController(
            stateStorage, processorStateReplication, DefaultZeebeDbFactory.DEFAULT_DB_FACTORY);

    if (state == RaftState.FOLLOWER) {
      processorSnapshotController.consumeReplicatedSnapshots();
      exporterSnapshotController.consumeReplicatedSnapshots();
    }
  }

  @Override
  public void stop(ServiceStopContext stopContext) {
    processorStateReplication.close();
    exporterStateReplication.close();
  }

  @Override
  public Partition get() {
    return this;
  }

  public PartitionInfo getInfo() {
    return info;
  }

  public int getPartitionId() {
    return partitionId;
  }

  public SnapshotController getExporterSnapshotController() {
    return exporterSnapshotController;
  }

  public StateSnapshotController getProcessorSnapshotController() {
    return processorSnapshotController;
  }

  public RaftState getState() {
    return state;
  }

  public LogStream getLogStream() {
    return logStream;
  }

  public Injector<LogStream> getLogStreamInjector() {
    return logStreamInjector;
  }

  public Injector<StateStorageFactory> getStateStorageFactoryInjector() {
    return stateStorageFactoryInjector;
  }
}
