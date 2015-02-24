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
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.commands.SelectionBoxCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.tool.ModalTool;
import org.locationtech.udig.project.ui.tool.SimpleTool;
import org.locationtech.udig.tool.info.internal.InfoView2;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A tool that puts a BBOX Filter on the layer's Filter
 * 
 * @author egouge
 * @since 1.0
 */
public class BBoxInfoTool extends SimpleTool implements ModalTool {
    
    /**
     * Comment for <code>ID</code>
     */
    public static final String ID = "org.wcs.smart.ui.map.tool.BBoxInfoTool"; //$NON-NLS-1$
    
    private Point start;

	private boolean selecting;

	org.locationtech.udig.project.ui.commands.SelectionBoxCommand shapeCommand;

    Set<String> selectedFids = new HashSet<String>();
    
	/**
	 * Creates a new instance of BBoxSelection
	 */
	public BBoxInfoTool() {
		super(MOUSE | MOTION);
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
			performInfo(bounds);
			selecting = false;
			shapeCommand.setValid(false);
			getContext().getViewportPane().repaint();
		}
	}
    
	private void performInfo(ReferencedEnvelope bbox){
		final InfoView2.InfoRequest request = new InfoView2.InfoRequest();
        request.bbox = bbox;
        request.layers = context.getMapLayers();
        
        Display.getDefault().asyncExec(new Runnable(){
            public void run() {
				InfoView2 infoView=(InfoView2) ApplicationGIS.getView(true, InfoView2.VIEW_ID);
				
				// JONES: deselect current feature so it won't flash when view is activated (it won't be valid
				// one the new search passes.
				if( infoView!=null)
					if( infoView.getSite().getSelectionProvider()!=null )
						infoView.getSite().getSelectionProvider().setSelection(new StructuredSelection());
				
				//JONES: activate view now that there is no current selection. 
		    	IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				if (!page.isPartVisible(infoView)) page.bringToTop(infoView);
                
                // we got here and info was null? Don't want to fail on first attempt
                infoView=(InfoView2) ApplicationGIS.getView(false, InfoView2.VIEW_ID);
                //this ensures the selection is passed along correctly
                infoView.setFocus();
                //run request
				infoView.search( request );
            }
        }); 
	}

}