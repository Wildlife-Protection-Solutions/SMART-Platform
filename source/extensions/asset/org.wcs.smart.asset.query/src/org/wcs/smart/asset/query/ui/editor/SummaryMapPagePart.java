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
package org.wcs.smart.asset.query.ui.editor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.query.common.ui.QueryMapPageEditor;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.ui.editor.IMapQueryEditor;

/**
 * Map page part for asset summary queries.
 * 
 * @author Emily
 *
 */
public class SummaryMapPagePart extends QueryMapPageEditor{

	private Composite parent;
	private Canvas notValidCanvas;
	private Shell tempShell;
	
	public SummaryMapPagePart(IMapQueryEditor parent) {
		super(parent);
	}
	
	public void createPartControl(Composite parent) {
		this.parent = parent;
		super.createPartControl(parent);
	}
	
	public void dispose() {
		super.dispose();
		if (tempShell != null) tempShell.dispose();
	}
	
	private void showMap() {
		if (notValidCanvas != null) {
			notValidCanvas.dispose();
			notValidCanvas = null;
		}
		if (tempShell != null) {
			for (Control c : tempShell.getChildren()) c.setParent(parent);
			tempShell.dispose();
			tempShell = null;
		}
		parent.layout();
	}
	
	private void showInvalidMessage() {
		if (notValidCanvas == null) {
			tempShell = new Shell();
			for (Control c : parent.getChildren()) c.setParent(tempShell);
			
			notValidCanvas = new Canvas(parent, SWT.NONE);
			notValidCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			notValidCanvas.addPaintListener(new PaintListener() {
				
				@Override
				public void paintControl(PaintEvent e) {
					
					e.gc.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
					e.gc.fillRectangle(0, 0, notValidCanvas.getBounds().width, notValidCanvas.getBounds().height);
					
					e.gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
					
					String str = Messages.SummaryMapPagePart_MapNotValid1;
					int w = e.gc.textExtent(str).x;
					w = (notValidCanvas.getBounds().width - w) / 2;
					e.gc.drawText(str,w, notValidCanvas.getBounds().height / 2);
					
					str = Messages.SummaryMapPagePart_MapNotValid2;
					Point size = e.gc.textExtent(str);
					w = (notValidCanvas.getBounds().width - size.x) / 2;
					e.gc.drawText(str,w, notValidCanvas.getBounds().height / 2 + (size.y + 5));
					
					str = Messages.SummaryMapPagePart_MapNotValid3;
					size = e.gc.textExtent(str);
					w = (notValidCanvas.getBounds().width - size.x) / 2;
					e.gc.drawText(str,w, notValidCanvas.getBounds().height / 2 + size.y * 2 + 5);
					
				}
			});
			notValidCanvas.moveAbove(parent.getChildren()[0]);
			notValidCanvas.setBounds(parent.getBounds());
		}
	}
	
	
    /**
     * Refresh the service on the map
     */
    public void refresh(){
    	AssetSummaryQuery assetQuery = (AssetSummaryQuery) ((AssetSummaryEditor)getParentEditor()).getQueryProxy().getQuery();
    	
    	boolean canDisplayOnMap = false;
    	try {
    		GroupByPart rowGroupBy = assetQuery.getQueryDefinition().getRowGroupByPart();
	    	if (rowGroupBy.getGroupBys().size() == 1) {
	    		IGroupBy gb = rowGroupBy.getGroupBys().get(0);
	    		if (gb instanceof AssetGroupBy) {
	    			AssetGroupBy agb = (AssetGroupBy)gb;
	    			if (agb.getOption() == AssetFilterOption.STATION ||
	    					agb.getOption() == AssetFilterOption.STATIONLOCATION) {
	    				canDisplayOnMap = true;
	    			}
	    		}
	    	}
    	}catch (Exception ex) {
    		AssetQueryPlugIn.log(ex.getMessage(), ex);
    	}
    	if (canDisplayOnMap) {
    		Display.getDefault().asyncExec(()->{
	    		showMap();
    		});
    		super.refresh();
    	}else {
    		Display.getDefault().asyncExec(()->{
    			showInvalidMessage();
    		});
    	}
    }
}
