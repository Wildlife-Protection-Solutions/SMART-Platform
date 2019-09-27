/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.navigation.ui;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelEvent;
import org.locationtech.udig.project.ui.tool.SimpleTool;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;

/**
 * Map tool for adding new linear navigation target. Works in conjunction with
 * ITargeEditor on the map blackboard
 * 
 * @author Emily
 *
 */
public class AddLineTool extends SimpleTool {

	public static final String ID = "org.wcs.smart.ui.map.navigation.addline"; //$NON-NLS-1$
	
	private Color LINE_COLOR = new Color(51,68,107);
	private Color FILL_COLOR = new Color(51,68,107,10);
	
	private List<Point> coordinates = new ArrayList<Point>();
	private Point last;
	private AbstractDrawCommand drawCommand = null;
	
	private Listener cancelListener = new Listener() {
		
		@Override
		public void handleEvent(Event event) {
			if (event.keyCode == SWT.ESC){
				//start over
				coordinates.clear();
				getContext().getViewportPane().repaint();
				
			}
		}
	};
	
	private class FeedbackCommand extends AbstractDrawCommand{	
		
		@Override
		public void run(IProgressMonitor monitor) throws Exception {
			if (coordinates.size() > 1){
				Path p = new Path(Display.getCurrent());
				Point c = coordinates.get(0);
				p.moveTo(c.x,c.y);
				
				for (int i = 1; i < coordinates.size(); i ++){
					c = coordinates.get(i);
					p.lineTo(c.x,  c.y);
				}
				if (last != null) p.lineTo(last.x,  last.y);
				
				p.dispose();
			}
			
			graphics.setColor( new Color(51,68,107));
			for (int i = 1; i < coordinates.size(); i ++){
				Point c = coordinates.get(i - 1);
				Point c2 = coordinates.get(i);
				graphics.drawLine(c.x, c.y, c2.x, c2.y);
				
			}
			if (last != null && coordinates.size() > 0){
				Point c = coordinates.get(coordinates.size() - 1);
				graphics.drawLine(c.x, c.y, last.x, last.y);
			}			
			int radius = 6;
			for (int i = 0; i < coordinates.size(); i ++){
				Point c = coordinates.get(i);
				graphics.setColor(FILL_COLOR);
				graphics.fillOval(c.x - radius/2, c.y -radius/2, radius, radius);
				graphics.setColor(LINE_COLOR);
				graphics.drawOval(c.x - radius/2, c.y -radius/2, radius, radius);
			}
			if (last != null && coordinates.size() > 0){
				Point c = last;
				graphics.setColor(FILL_COLOR);
				graphics.fillOval(c.x - radius/2, c.y - radius/2, radius, radius);
				graphics.setColor( LINE_COLOR);
				graphics.drawOval(c.x - radius/2, c.y - radius/2, radius, radius);
			}
		}


		@Override
		public Rectangle getValidArea() {
			
			Point min = null;
			Point max = null;
			for (Point c : coordinates){
				if (min == null){
					min = new Point(c);
				}else{
					if (c.x < min.x) min.x = c.x;
					if (c.y < min.y) min.y = c.y;
				}
				if (max == null){
					max = new Point(c);
				}else{
					if (c.x > max.x) max.x = c.x;
					if (c.y > max.y) max.y = c.y;
				}
			
			}
			if (last != null){
				if (min == null){
					min = new Point(last);
				}else{
					if (last.x < min.x) min.x = last.x;
					if (last.y < min.y) min.y = last.y;
				}
				if (max == null){
					max = new Point(last);
				}else{
					if (last.x > max.x) max.x = last.x;
					if (last.y > max.y) max.y = last.y;
				}
			}
			
			if (min == null || max == null) return null;
			int extra = 20;
			Rectangle r =  new Rectangle(min.x-extra, min.y-extra, (max.x - min.x)+2*extra, (max.y - min.y)+2*extra);
			return r;
		}
	};
	
	
	public AddLineTool(){
		super(MOUSE|MOTION);
	}
	
	public void setActive( boolean active ) {
		super.setActive(active);
		coordinates.clear();
		if (drawCommand != null){
			drawCommand.setValid(false);
			drawCommand.dispose();
			drawCommand = null;
		}
		
		if (active){
			getContext().getViewportPane().getControl().addListener(SWT.KeyUp, cancelListener);
		}else{
			getContext().getViewportPane().getControl().removeListener(SWT.KeyUp, cancelListener);
		}
		getContext().getViewportPane().repaint();
	}
	
	private void finish(){
		ITargetEditor editor = (ITargetEditor) getContext().getMap().getBlackboard().get(ITargetEditor.ID);
		if (editor == null) return;
		
		Coordinate[] c = new Coordinate[coordinates.size()];
		for (int i = 0; i < coordinates.size(); i ++){
			Point temp = coordinates.get(i);
			c[i] = getContext().getViewportModel().pixelToWorld(temp.x, temp.y);
		}


		try {
			LineString ls = GeometryFactoryProvider.getFactory().createLineString(c);
			ls = (LineString) JTS.transform(ls, CRS.findMathTransform(getContext().getViewportModel().getCRS(), SmartDB.DATABASE_CRS));
			editor.addLinearTarget(ls);
		}catch (Exception ex) {
			CyberTrackerPlugIn.log(ex.getMessage(), ex);
		}	
	}
	
	 /**
     * Called when a double clicked event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseDoubleClicked( MapMouseEvent e ) {
    	coordinates.add(new Point(e.x, e.y));
    	finish();
    	coordinates.clear();

    	redraw(null);
    	
    	if (drawCommand != null){
    		drawCommand.setValid(false);
    		drawCommand.dispose();
    		drawCommand = null;
    	}else{
    		
    	}
    }
    
    private void redraw(Point last){
    	if (drawCommand == null || !drawCommand.isValid() ){
    		getContext().getViewportPane().repaint();
    		return;
    	}
    	this.last = last;    	
    	Rectangle area = drawCommand.getValidArea();
        if (area != null){
            getContext().getViewportPane().repaint(area.x, area.y, area.width, area.height);
        }else {
            getContext().getViewportPane().repaint();
        }
    }
    /**
     * Called when a mouse pressed event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMousePressed( MapMouseEvent e ) {
    	if (drawCommand == null){
    		drawCommand = new FeedbackCommand();
    		getContext().getViewportPane().addDrawCommand(drawCommand);
    	}
    	coordinates.add(new Point(e.x, e.y));
    	redraw(new Point(e.x, e.y));
    }
    /**
     * Called when a mouse released event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseReleased( MapMouseEvent e ) {
        redraw(null);
    }

    /**
     * Called when a entered event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseEntered( MapMouseEvent e ) {
    }
    /**
     * Called when a moved event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseMoved( MapMouseEvent e ) {
    	redraw(new Point(e.x, e.y));
    }    
    /**
     * Called when a hovered event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseHovered( MapMouseEvent e ) {
    }
    /**
     * Called when a exited event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseExited( MapMouseEvent e ) {
    }
    /**
     * Called when a mouse wheel moved event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseWheelMoved( MapMouseWheelEvent e ) {
    }
    
    /**
     * Called when a mouse dragged event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMouseDragged( MapMouseEvent e ) {
    }
}
