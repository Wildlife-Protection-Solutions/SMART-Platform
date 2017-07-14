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
package org.wcs.smart.udig;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Control;
import org.geotools.geometry.jts.JTS;
import org.locationtech.udig.catalog.util.CRSUtil;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.tool.SimpleTool;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Tool to compute the list between points.
 * 
 * @author Emily
 *
 */
public class SmartDistanceTool extends SimpleTool implements KeyListener {
	
	public static final String ID = "org.wcs.smart.map.tool.distance"; //$NON-NLS-1$

	private String units;
	private List<Point> points = new ArrayList<Point>();
	private DistanceFeedbackCommand command;
	private Point now;
	
	public SmartDistanceTool() {
		super(MOUSE | MOTION);
	}

	@Override
	protected void onMouseMoved(MapMouseEvent e) {
		now = e.getPoint();
		if (command == null || points.isEmpty())
			return;
		Rectangle area = command.getValidArea();
		if (area != null)
			getContext().getViewportPane().repaint(area.x, area.y, area.width,
					area.height);
		else {
			getContext().getViewportPane().repaint();
		}
	}

	public void onMouseReleased(MapMouseEvent e) {
		Point current = e.getPoint();
		if (points.isEmpty() || !current.equals(points.get(points.size() - 1)))
			points.add(current);
		if (command == null || !command.isValid()) {
			command = new DistanceFeedbackCommand();
			getContext().sendASyncCommand(command);
		}
	}

	@Override
	protected void onMouseDoubleClicked(MapMouseEvent e) {
		disposeCommand();
		points.clear();
	}

	private void disposeCommand() {
		if (command != null) {
			points.clear();
			command.setValid(false);
			Rectangle area = command.getValidArea();
			if (area != null)
				getContext().getViewportPane().repaint(area.x, area.y,
						area.width, area.height);
			else {
				getContext().getViewportPane().repaint();
			}
			command = null;
		}
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		
		disposeCommand();

		if (active) {
			Control control = getContext().getViewportPane().getControl();
			control.addKeyListener(this);
		} else {
			Control control = getContext().getViewportPane().getControl();
			control.removeKeyListener(this);
		}

		// configure units
		// default to metric units unless imperial crs
		units = org.locationtech.udig.ui.preferences.PreferenceConstants.METRIC_UNITS;
		if (CRSUtil.isCoordinateReferenceSystemImperial(context.getCRS())) {
			units = org.locationtech.udig.ui.preferences.PreferenceConstants.IMPERIAL_UNITS;
		}

	}

	private double distance() throws TransformException {
		
		if (points.isEmpty())
			return 0;
		
		Iterator<Point> iter = points.iterator();
		Point start = iter.next();
		double distance = 0;
		
		while (iter.hasNext()) {
			Point current = iter.next();
			Coordinate begin = getContext().pixelToWorld(start.x, start.y);
			Coordinate end = getContext().pixelToWorld(current.x, current.y);
			distance += JTS.orthodromicDistance(begin, end, getContext()
					.getCRS());
			start = current;
		}

		if (now != null) {
			Point current = now;
			Coordinate begin = getContext().pixelToWorld(start.x, start.y);
			Coordinate end = getContext().pixelToWorld(current.x, current.y);
			distance += JTS.orthodromicDistance(begin, end, getContext()
					.getCRS());
			start = current;
		}

		return distance;
	}

	/**
	 * @param distance
	 * @return
	 */
	private String createMessageMetric(double distance) {
		StringBuilder message = new StringBuilder();

		if (distance > 100000.0) {
			message = message.append((int) (distance / 1000.0) + "km"); //$NON-NLS-1$
		} else if (distance > 10000.0) { // km + m
			message = message.append(round(distance / 1000.0, 1) + "km"); //$NON-NLS-1$
		} else if (distance > 1000.0) { // km + m
			message = message.append(round(distance / 1000.0, 2) + "km"); //$NON-NLS-1$
		} else if (distance > 100.0) { // m
			message = message.append(round(distance, 1) + "m"); //$NON-NLS-1$
		} else if (distance > 1.0) { // m
			message = message.append(round(distance, 2) + "m"); //$NON-NLS-1$
		} else { // mm
			message = message.append(round(distance * 1000.0, 1) + "mm"); //$NON-NLS-1$
		}

		return message.toString();
	}

	private String createMessageImperial(double distance) {
		StringBuilder message = new StringBuilder();
		// distance is in meter
		distance = (distance / 1000) * 0.621371192; // convert to miles

		if (distance > 1000.0) {
			message = message.append((int) (distance) + "mi"); //$NON-NLS-1$
		} else if (distance > 100.0) { // mile
			message = message.append(round(distance, 1) + "mi"); //$NON-NLS-1$
		} else if (distance > 1.0) { // mile
			message = message.append(round(distance, 2) + "mi"); //$NON-NLS-1$
		} else if (distance > 0.1) { // mile
			message = message.append((int) (distance * 5280) + "ft"); //$NON-NLS-1$
		} else if (distance > 0.0189) { // mile approx.100ft
			message = message.append(round(distance * 5280, 1) + "ft"); //$NON-NLS-1$
		} else { // mile
			message = message.append(round(distance * 5280 * 12, 1) + "in"); //$NON-NLS-1$
		}

		return message.toString();
	}

	/**
	 * Truncates a double to the given number of decimal places. Note:
	 * truncation at zero decimal places will still show up as x.0, since we're
	 * using the double type.
	 * 
	 * @param value
	 *            number to round-off
	 * @param decimalPlaces
	 *            number of decimal places to leave
	 * @return the rounded value
	 */
	private double round(double value, int decimalPlaces) {
		double divisor = Math.pow(10, decimalPlaces);
		double newVal = value * divisor;
		newVal = (Long.valueOf(Math.round(newVal)).intValue()) / divisor;
		return newVal;
	}

	class DistanceFeedbackCommand extends AbstractDrawCommand {

		public Rectangle getValidArea() {
			return null;
		}

		public void run(IProgressMonitor monitor) throws Exception {
			if (points.isEmpty())
				return;
			graphics.setColor(Color.BLACK);
			Iterator<Point> iter = points.iterator();
			Point start = iter.next();
			while (iter.hasNext()) {
				Point current = iter.next();
				graphics.drawLine(start.x, start.y, current.x, current.y);
				start = current;
			}
			if (start == null || now == null)
				return;
			graphics.drawLine(start.x, start.y, now.x, now.y);

			final String message;
			if (units
					.equals(org.locationtech.udig.ui.preferences.PreferenceConstants.IMPERIAL_UNITS)) {
				message = createMessageImperial(distance());
			} else {
				message = createMessageMetric(distance());
			}
			Rectangle2D rec = graphics.getStringBounds(message);

			graphics.setColor(Color.WHITE);
			graphics.fillRoundRect(now.x + 3, now.y + 5,
					(int) rec.getWidth() + 4, (int) rec.getHeight(), 5, 5);
			graphics.setColor(Color.BLACK);
			graphics.drawString(message, now.x + 5, now.y + 5,
					ViewportGraphics.ALIGN_LEFT, ViewportGraphics.ALIGN_BOTTOM);
		}

	}

	public void reset() {
		points.clear();
		if (command != null) {
			command.setValid(false);
			Rectangle area = command.getValidArea();
			if (area != null)
				getContext().getViewportPane().repaint(area.x, area.y,
						area.width, area.height);
			else {
				getContext().getViewportPane().repaint();
			}
			command = null;
		}
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
		if (e.character == SWT.CR) {
			// finish on enter key
			disposeCommand();
			points.clear();
		}
	}
}
