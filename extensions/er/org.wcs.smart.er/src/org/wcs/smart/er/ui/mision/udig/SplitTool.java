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
package org.wcs.smart.er.ui.mision.udig;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.project.ui.commands.AbstractDrawCommand;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.tool.SimpleTool;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.ReprojectUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A split tool for mission tracks.  You must set the finishCommand
 * when the tool is activated.
 * 
 * @author Emily
 *
 */
public class SplitTool extends SimpleTool implements KeyListener {
	
	public static final String ID = "org.wcs.smart.er.track.split"; //$NON-NLS-1$
	public static final String CATEGORY_ID = "org.wcs.smart.udig.maptools"; //$NON-NLS-1$
	
	private List<Point> points = new ArrayList<Point>();
	private SplitFeedbackCommand command;
	private Point now;
	
	private FinishCommand finishCommand;
	
	public SplitTool() {
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

	public void setFinishCommand(FinishCommand finish){
		this.finishCommand = finish;
	}
	
	public void onMouseReleased(MapMouseEvent e) {
		Point current = e.getPoint();
		if (points.isEmpty() || !current.equals(points.get(points.size() - 1)))
			points.add(current);
		
		if (command == null || !command.isValid()) {
			command = new SplitFeedbackCommand();
			getContext().sendASyncCommand(command);
		}
		
		if (points.size() == 2){
			disposeCommand();
			executeFinish();
			points.clear();
		}
	}

	@Override
	protected void onMouseDoubleClicked(MapMouseEvent e) {
		if (points.size() == 2){
			disposeCommand();
			executeFinish();
			points.clear();
		}
	}

	/**
     *
     */
	private void disposeCommand() {
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

	private void executeFinish() {
		if (finishCommand != null){
			try {
				List<Coordinate> cs = new ArrayList<Coordinate>();
				for (Point p : points){
					Coordinate c1 = getContext().getViewportModel().pixelToWorld(p.x, p.y);
					
					//convert to lat/lng
					Coordinate c2 = ReprojectUtils.reproject(c1.x, c1.y, getContext().getViewportModel().getCRS(), SmartDB.DATABASE_CRS);
					cs.add(c2);
				}
				
				finishCommand.onFinish(cs);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		
		if (active) {
			Control control = getContext().getViewportPane().getControl();
			control.addKeyListener(this);
		} else {
			Control control = getContext().getViewportPane().getControl();
			control.removeKeyListener(this);
		}

	}

	

	class SplitFeedbackCommand extends AbstractDrawCommand {

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
		}

	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}
	
	public interface FinishCommand{
		public void onFinish(List<Coordinate> points);
	}
}
