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
package org.wcs.smart.ui.map.tool;

import java.awt.Point;
import java.awt.Rectangle;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.project.ui.commands.SelectionBoxCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.tool.ModalTool;
import org.locationtech.udig.project.ui.tool.SimpleTool;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Tool that allows users to select a bounding box on the map.  A
 * single selection listener can be added that is fired when the bounding
 * box is selected.
 * 
 * @author Emily
 *
 */
public class TrackPointSelectionTool  extends SimpleTool implements ModalTool {
    
    /**
     * Comment for <code>ID</code>
     */
    public static final String ID = "org.wcs.smart.ui.map.tool.TrackPointSelector"; //$NON-NLS-1$
    
    private Point start;

	private boolean selecting;

	private org.locationtech.udig.project.ui.commands.SelectionBoxCommand shapeCommand;

    private PointSelectionListener listener;
    
	/**
	 * Creates a new instance of BBoxSelection
	 */
	public TrackPointSelectionTool() {
		super(MOUSE | MOTION);
	}
	
	/**
	 * Adds a new listener.  Only a single listener exists at a
	 * time, this will overwrite previous calls to this function.
	 * @param listener
	 */
	public void addListener(PointSelectionListener listener){
		this.listener = listener;
	}
    
	/**
	 * @see org.locationtech.udig.project.ui.tool.SimpleTool#onMouseDragged(org.locationtech.udig.project.render.displayAdapter.MapMouseEvent)
	 */
	protected void onMouseDragged(MapMouseEvent e) {
		Point end = e.getPoint();
		if(start == null) return; 
		shapeCommand.setShape(
				new Rectangle(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.abs(start.x - end.x), Math.abs(start.y - end.y)));
		context.getViewportPane().repaint();
	}

	/**
	 * @see org.locationtech.udig.project.ui.tool.AbstractTool#mousePressed(org.locationtech.udig.project.render.displayAdapter.MapMouseEvent)
	 */
	public void onMousePressed(MapMouseEvent e) {
		shapeCommand = new SelectionBoxCommand();

		if (((e.button & MapMouseEvent.BUTTON1) != 0)) {
			selecting = true;

			start = e.getPoint();
			shapeCommand.setValid(true);
			shapeCommand.setShape(new Rectangle(start.x, start.y, 0, 0));
			context.sendASyncCommand(shapeCommand);
		}
	}

	/**
	 * @see org.locationtech.udig.project.ui.tool.AbstractTool#mouseReleased(org.locationtech.udig.project.render.displayAdapter.MapMouseEvent)
	 */
	public void onMouseReleased(MapMouseEvent e) {
		if (selecting) {
			Point end = e.getPoint();
			ReferencedEnvelope bounds = null;
			if (start == null || start.equals(end)) {
				bounds = getContext()
						.getBoundingBox(e.getPoint(),3);
			} else {
				Coordinate c1 = context.getMap().getViewportModel()
						.pixelToWorld(start.x, start.y);
				Coordinate c2 = context.getMap().getViewportModel()
						.pixelToWorld(end.x, end.y);

				bounds = new ReferencedEnvelope(c1.x, c2.x, c1.y, c2.y, context.getMap().getViewportModel().getCRS());
				
			}
			performSelection(bounds);
			selecting = false;
			shapeCommand.setValid(false);
			getContext().getViewportPane().repaint();
		}
	}
	
	private void performSelection(ReferencedEnvelope bbox){
		listener.selection(bbox);
	}
	
	/**
	 * Listener interface
	 * @author Emily
	 *
	 */
	public interface PointSelectionListener{
		void selection(ReferencedEnvelope bbox);
	}
}


