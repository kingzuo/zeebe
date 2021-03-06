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
package io.zeebe.broker.workflow.processor.handlers.catchevent;

import io.zeebe.broker.workflow.model.element.ExecutableCatchEventElement;
import io.zeebe.broker.workflow.processor.BpmnStepContext;
import io.zeebe.broker.workflow.processor.handlers.CatchEventSubscriber;
import io.zeebe.broker.workflow.processor.handlers.element.ElementTerminatingHandler;
import io.zeebe.protocol.intent.WorkflowInstanceIntent;

public class IntermediateCatchEventElementTerminatingHandler<T extends ExecutableCatchEventElement>
    extends ElementTerminatingHandler<T> {
  private final CatchEventSubscriber catchEventSubscriber;

  public IntermediateCatchEventElementTerminatingHandler(
      CatchEventSubscriber catchEventSubscriber) {
    super();
    this.catchEventSubscriber = catchEventSubscriber;
  }

  public IntermediateCatchEventElementTerminatingHandler(
      WorkflowInstanceIntent nextState, CatchEventSubscriber catchEventSubscriber) {
    super(nextState);
    this.catchEventSubscriber = catchEventSubscriber;
  }

  @Override
  protected boolean handleState(BpmnStepContext<T> context) {
    if (super.handleState(context)) {
      catchEventSubscriber.unsubscribeFromEvents(context);
      return true;
    }

    return false;
  }
}
