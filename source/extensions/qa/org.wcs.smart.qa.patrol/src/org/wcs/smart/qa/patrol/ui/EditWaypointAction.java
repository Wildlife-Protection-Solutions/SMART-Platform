/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.patrol.ui;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.model.QaError.Status;
import org.wcs.smart.qa.routine.IQaAction;
import org.wcs.smart.qa.ui.view.EditWaypointDetailsDialog;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * Action implementation for editing waypoint positions.  Applicable
 * for PatrolWaypointDataProvider
 * 
 * @author Emily
 *
 */
public class EditWaypointAction implements IQaAction {

	@Override
	public void doAction(List<QaError> items) {
		if (items.isEmpty()) return;
		QaError item = items.get(0);
		EditWaypointDetailsDialog dialog = new PatrolEditWaypointDialog(Display.getDefault().getActiveShell(), item.getSourceId());
		if (dialog.open() == Window.OK){
			item.setStatus(Status.FIXED);
			Point pnt = (Point)item.getGeometryObject();
			Point to = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(dialog.getUpdatedPoint().getX(), dialog.getUpdatedPoint().getY()));
			item.setFixMessage(MessageFormat.format("Manually moved from ({0}, {1}) to ({2}, {3})", pnt.getX(), pnt.getY(), to.getX(), to.getY()));
			item.setGeometryObject(to);			
		}
	}

	@Override
	public boolean supportsMultiple() {
		return false;
	}

	@Override
	public String getId() {
		return "org.wcs.smart.qa.waypoint.edit"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return "Edit Waypoint...";
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON);
	}

}
