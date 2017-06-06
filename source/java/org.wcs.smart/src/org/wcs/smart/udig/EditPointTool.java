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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.tool.AbstractModalTool;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.wcs.smart.udig.IMapEditManager.EditPoint;

/**
 * Tool for moving point features.  The tool looks for a IEditPointManager
 * manager on the map blackboard to determine what to do when the point
 * is moved. See PatrolMapPageEditor for an example.
 * 
 * @author Emily
 *
 */
public class EditPointTool extends AbstractModalTool{

	public static final String ID = "org.wcs.smart.map.tool.edit.point"; //$NON-NLS-1$
	
	private Point screenLocation = null;
	private Point startPoint = null;
	private EditPoint hoverPoint = null;
	private EditPoint feature;
	
	private DrawPointCommand feedbackCommand;
	private HoverPointCommand hoverCommand;

	
	private Listener cancelListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (event.keyCode == SWT.ESC){
				//start over
				screenLocation = null;
				if (feedbackCommand != null){
					feedbackCommand.setValid(false);
					feedbackCommand.dispose();
					feedbackCommand = null;
				}
				getContext().getViewportPane().repaint();
			}
		}
	};
	
	private class DrawPointCommand extends AbstractDrawCommand{
		@Override
		public Rectangle getValidArea() {
			return null;
		}

		@Override
		public void run(IProgressMonitor monitor) throws Exception {
			if (startPoint != null){
				graphics.setColor(Color.YELLOW);
				graphics.setLineWidth(3);
				graphics.drawOval(startPoint.x-6, startPoint.y-6, 12, 12);
			}
			if (screenLocation != null){
				graphics.setBackground(Color.RED);
				graphics.fillOval(screenLocation.x-5, screenLocation.y-5, 10, 10);
			}
		}
	};
	
	
	private class HoverPointCommand extends AbstractDrawCommand{
		@Override
		public Rectangle getValidArea() {
			return null;
		}

		@Override
		public void run(IProgressMonitor monitor) throws Exception {
			if (hoverPoint != null){
				graphics.setColor(Color.YELLOW);
				graphics.setLineWidth(3);
				graphics.drawOval(hoverPoint.getMapPoint().x - 6, hoverPoint.getMapPoint().y - 6, 12, 12);
				
				if(hoverPoint.getInfoString() == null) return;
				int startX = hoverPoint.getMapPoint().x + 5;
				int startY = hoverPoint.getMapPoint().y + 5;
				
				graphics.setColor(new Color(255,255,255,200));
				
				String[] parts = hoverPoint.getInfoString().split("\n"); //$NON-NLS-1$
				
				int width = 0;
				int height = 0;
				for (String p : parts){
					Rectangle2D bounds = graphics.getStringBounds(p);
					width = (int)Math.max(width, bounds.getWidth());
					height += bounds.getHeight();
				}
				
				graphics.fillRoundRect(startX, startY, width+10, height+10, 5, 5);
				
				graphics.setColor(Color.BLACK);
				
				int y = startY + 5;
				for (String p : parts){
					graphics.drawString(p, startX + 5, y, ViewportGraphics.ALIGN_LEFT, ViewportGraphics.ALIGN_BOTTOM);
					y += graphics.getStringBounds(p).getHeight();
				}
				
			}
		}
	};
	
	public EditPointTool() {
		super(MOUSE | MOTION);
	}

	private IMapEditManager getEditManager(){
		Object x = getContext().getMap().getBlackboard().get(IMapEditManager.class.getCanonicalName());
		if (x instanceof IMapEditManager) return (IMapEditManager)x;
		return null;
	}
	
	@Override
	public void setActive( boolean active ) {
		super.setActive(active);
		screenLocation = null;
		if (feedbackCommand != null){
			feedbackCommand.setValid(false);
			feedbackCommand.dispose();
			feedbackCommand = null;
		}
		
		if (active){
			getContext().getViewportPane().getControl().addListener(SWT.KeyUp, cancelListener);
		}else{
			getContext().getViewportPane().getControl().removeListener(SWT.KeyUp, cancelListener);
		}
		getContext().getViewportPane().repaint();
	}
	
    /**
     * This method may be overridden by subclasses
     * 
     * @see org.locationtech.udig.project.ui.render.displayAdapter.MapMouseListener#mousePressed(org.locationtech.udig.project.render.displayAdapter.MapMouseEvent)
     * @see MapMouseEvent
     */
    public void mousePressed( MapMouseEvent e ) {
    	disposeHover();
    	if ((e.buttons & MapMouseEvent.BUTTON1) != MapMouseEvent.BUTTON1) return;
    	IMapEditManager manager = getEditManager();
    	if (manager == null) return;

    	feature = manager.findFeature(e.x, e.y, getContext().getViewportModel());
    	if (feature == null) return;
  
    	screenLocation = new Point(e.x, e.y);
    	startPoint = feature.getMapPoint();
    	if (feedbackCommand == null){
    		feedbackCommand = new DrawPointCommand();
    		getContext().getViewportPane().addDrawCommand(feedbackCommand);
    	}
    	redraw();
    }
    
    public void mouseDragged( MapMouseEvent e ) {
    	if (screenLocation == null) return;
    	screenLocation = new Point(e.x, e.y);
    	redraw();
    }
    
    /**
     * This method may be overridden by subclasses
     * 
     * @see org.locationtech.udig.project.ui.render.displayAdapter.MapMouseListener#mouseReleased(org.locationtech.udig.project.render.displayAdapter.MapMouseEvent)
     * @see MapMouseEvent
     */
    public void mouseReleased( MapMouseEvent e ) {
    	if (screenLocation == null) return;
    	if ((e.button & MapMouseEvent.BUTTON1) != MapMouseEvent.BUTTON1) return;
    	if (feature == null) return;
    	
    	IMapEditManager manager = getEditManager();
    	if (manager == null) return;
    	
    	//if point outside visible map area then don't drop
    	if (!(e.x < 0 || e.y < 0 || e.x > getContext().getViewportPane().getWidth() || e.y > getContext().getViewportPane().getHeight())){
    		manager.moveFeature(feature.getFeature(), e.x, e.y, getContext().getViewportModel());	
    	}
    	
    	feature = null;
    	screenLocation = null;
    	if (feedbackCommand != null){
    		feedbackCommand.dispose();
    		feedbackCommand = null;
    	}
    	redraw();
    }
    
	private void redraw() {
		if (feedbackCommand == null || !feedbackCommand.isValid()) {
			getContext().getViewportPane().repaint();
			return;
		}
		Rectangle area = feedbackCommand.getValidArea();
		if (area != null) {
			getContext().getViewportPane().repaint(area.x, area.y, area.width, area.height);
		} else {
			getContext().getViewportPane().repaint();
		}
	}
    
    /**
     * This method may be overridden by subclasses
     * 
     * @see org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener#mouseHovered(MapMouseEvent)
     */
    public void mouseHovered( MapMouseEvent e ) { 
    	IMapEditManager manager = getEditManager();
    	if (manager == null) return;

    	hoverPoint = manager.findFeature(e.x, e.y, getContext().getViewportModel());
    	if (hoverPoint == null) return;
    	if (hoverCommand == null){
    		hoverCommand = new HoverPointCommand();
    		getContext().getViewportPane().addDrawCommand(hoverCommand);
    	}
    	redraw();
    }
 
    /**
     * This method may be overridden by subclasses
     * 
     * @see org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener#mouseMoved(org.locationtech.udig.project.render.displayAdapter.MapMouseEvent)
     */
    public void mouseMoved( MapMouseEvent e ) { 
    	disposeHover();
    }
    
    private void disposeHover(){
    	if (hoverPoint != null){
    		hoverPoint = null;
    		if (hoverCommand != null){
    			hoverCommand.dispose();
    			hoverCommand = null;
    		}
    		redraw();
    	}
    }
}
