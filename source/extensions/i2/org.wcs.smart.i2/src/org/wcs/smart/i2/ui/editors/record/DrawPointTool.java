/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.record;

import java.awt.Color;
import java.awt.Point;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.tool.SimpleTool;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.map.GeometryFactoryProvider;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * uDig tool for creating location polygons
 * 
 * @author Emily
 *
 */
public class DrawPointTool extends SimpleTool {

	public static final String ID = "org.wcs.smart.i2.record.point.draw"; //$NON-NLS-1$
	
	private Point last;
	
	public DrawPointTool(){
		super(MOUSE|MOTION);
	}
	
	
	private void finish(){
		RecordEditor editor = (RecordEditor) getContext().getMap().getBlackboard().get(RecordEditor.class.getName());
		if (editor == null) return;
		
	
		
		com.vividsolutions.jts.geom.Point p = null;
		String error = null;
		try {
			Coordinate pnt = getContext().getViewportModel().pixelToWorld(last.x, last.y);
			p = GeometryFactoryProvider.getFactory().createPoint(pnt);
			p = (com.vividsolutions.jts.geom.Point) JTS.transform(p, CRS.findMathTransform(getContext().getViewportModel().getCRS(), SmartDB.DATABASE_CRS));
			if (p.isEmpty() || !p.isSimple() || !p.isValid()){
				error = "Invalid point.";
			}
		} catch (Exception e) {
			Intelligence2PlugIn.log(e.getMessage(), e);
			error = e.getMessage();
		}
		if (error != null){
			MessageBubble bubble = new MessageBubble(last.x, last.y, error, (short)5);
			bubble.setHorizontalCornerArc(5);
			bubble.setBubbleColor(new Color(255, 225,225));
			bubble.setTextColor(new Color(120,0,0));
			bubble.setHorizontalBorder(15);
			bubble.setVerticalBorder(15);
			getContext().getViewportPane().addDrawCommand(bubble);
		}else{
			editor.addNewLocation(p, null);
		}
		
	}
    /**
     * Called when a mouse pressed event occurs. It will never be a context-menu request
     * 
     * @param e the mouse event
     */
    protected void onMousePressed( MapMouseEvent e ) {
    	last = new Point(e.x, e.y);
    	finish();
    }
    
}
