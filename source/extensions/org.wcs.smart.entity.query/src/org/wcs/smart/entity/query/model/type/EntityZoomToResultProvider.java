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
package org.wcs.smart.entity.query.model.type;

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.entity.query.model.EntityQueryResultItem;
import org.wcs.smart.observation.query.model.types.AbstractZoomToInfoProvider;

/**
 * Zoom to provider for entity data queries.
 * 
 * @author Emily
 *
 */
public class EntityZoomToResultProvider extends AbstractZoomToInfoProvider {

	@Override
	public void doWork(Object resultItem) {
		if (resultItem instanceof EntityQueryResultItem) {
			EntityQueryResultItem item = (EntityQueryResultItem) resultItem;
			zoomTo(item.getWaypointX(null), item.getWaypointY(null));
		} else {
			MessageDialog.openError(
					Display.getDefault().getActiveShell(),
					ERROR_STR,
					MessageFormat.format(OP_NOT_SUPPORTED_STR,resultItem.getClass().getName()));
		}
	}
}
