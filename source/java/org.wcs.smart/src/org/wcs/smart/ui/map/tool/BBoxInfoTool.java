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

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.commands.AbstractDrawCommand;
import org.locationtech.udig.project.ui.commands.SelectionBoxCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.tool.ModalTool;
import org.locationtech.udig.project.ui.tool.SimpleTool;
import org.locationtech.udig.tool.info.internal.InfoView2;
import org.locationtech.udig.ui.graphics.ViewportGraphics;
import org.wcs.smart.ui.map.tool.IInfoToolProvider.InfoPoint;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A tool that puts a BBOX Filter on the layer's Filter.  
 * Also adds the ability to display additional details on mouse hover over
 * features.  To use this feature put a IInfoToolProvider on the layers blackboard with
 * the key IInfoToolProvider.class.getCanonicalName.  Currently designed to support
 * points and will highlight the area selected with a yellow dot.
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

	private org.locationtech.udig.project.ui.commands.SelectionBoxCommand shapeCommand;
    
    private InfoPoint infoPoint;

    /*
     * for displaying hover details
     */
    private HoverPointCommand hoverCommand;
    
    private class HoverPointCommand extends AbstractDrawCommand{
		@Override
		public Rectangle getValidArea() {
			return null;
		}

		@Override
		public void run(IProgressMonitor monitor) throws Exception {
			if (infoPoint != null){
				graphics.setColor(Color.YELLOW);
				graphics.setLineWidth(3);
				graphics.drawOval(infoPoint.getMapPoint().x - 6, infoPoint.getMapPoint().y - 6, 12, 12);
				
				if (infoPoint.getInfoString() == null) return;
				int startX = infoPoint.getMapPoint().x + 5;
				int startY = infoPoint.getMapPoint().y + 5;
				
				graphics.setColor(new Color(255,255,255,200));
				
				String[] parts = infoPoint.getInfoString().split("\n"); //$NON-NLS-1$
				
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
    
	/**
	 * Creates a new instance of BBoxSelection
	 */
	public BBoxInfoTool() {
		super(MOUSE | MOTION);
	}
    
	/*
	 * Find the info provider for mousing over
	 */
	private IInfoToolProvider getInfoProvider(){
		IInfoToolProvider provider = (IInfoToolProvider) getContext().getMap().getBlackboard().get(IInfoToolProvider.class.getCanonicalName());
		return provider;
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
						.getBoundingBox(e.getPoint(),5);
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

	/**
     * This method may be overridden by subclasses
     * 
     * @see org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener#mouseHovered(MapMouseEvent)
     */
    protected void onMouseHovered( MapMouseEvent e ) { 
    	IInfoToolProvider manager = getInfoProvider();
    	if (manager == null) return;

    	infoPoint = manager.findFeature(e.x, e.y, getContext().getViewportModel());
    	if (infoPoint == null) return;
    	if (hoverCommand == null){
    		hoverCommand = new HoverPointCommand();
    		getContext().getViewportPane().addDrawCommand(hoverCommand);
    		getContext().getViewportPane().repaint();
    	}
    }
 
    /**
     * This method may be overridden by subclasses
     * 
     * @see org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener#mouseMoved(org.locationtech.udig.project.render.displayAdapter.MapMouseEvent)
     */
    public void onMouseMoved( MapMouseEvent e ) { 
    	disposeHover();
    }
    
    private void disposeHover(){
    	if (infoPoint != null){
    		infoPoint = null;
    		if (hoverCommand != null){
    			hoverCommand.dispose();
    			hoverCommand = null;
    		}
    		getContext().getViewportPane().repaint();
    	}
    }
}