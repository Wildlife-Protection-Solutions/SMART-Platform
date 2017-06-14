/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.observation.query.model.types;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.observation.query.model.ObservationQueryResultItem;
import org.wcs.smart.query.common.engine.IResultItem;

/**
 * Observation info provider for all data queries and opens
 * the appropriate editor based on the waypoint uuid and source
 * provided by the result.
 * 
 * @author Emily
 *
 */
public class ObservationResultInfoProvider extends AbstractObservationInfoProvider {

	@Override
	public void doWork(IResultItem resultItem) {
		if (resultItem instanceof ObservationQueryResultItem) {
			UUID waypointUuid = ((ObservationQueryResultItem) resultItem).getWaypointUuid();
			String waypointSourceKey = ((ObservationQueryResultItem) resultItem).getSourceId();
			showItem(waypointUuid, waypointSourceKey);
		} else {
			MessageDialog
					.openError(
							Display.getDefault().getActiveShell(),
							ERROR_STR,
							MessageFormat.format(OP_NOT_SUPPORTED_STR,resultItem.getClass().getName()));
		}

	}


}
