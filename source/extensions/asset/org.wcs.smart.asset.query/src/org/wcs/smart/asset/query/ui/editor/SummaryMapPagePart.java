package org.wcs.smart.asset.query.ui.editor;


import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.query.common.ui.QueryMapPageEditor;
import org.wcs.smart.query.model.summary.GroupByPart;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.ui.editor.IMapQueryEditor;

public class SummaryMapPagePart extends QueryMapPageEditor{

	IMapQueryEditor editor;
	
	public SummaryMapPagePart(IMapQueryEditor parent) {
		super(parent);
	}

	private Composite parent;
	private Canvas notValidCanvas;
	
	private Control[] kids;
	private Shell temp;
	
	public void createPartControl(Composite parent) {
		this.parent = parent;
		super.createPartControl(parent);
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
    		ex.printStackTrace();
    		//TODO:
    	}
    	if (canDisplayOnMap) {
    		Display.getDefault().asyncExec(()->{
	    		notValidCanvas.dispose();
	    		notValidCanvas = null;
	    		for (Control c : kids) {
	    			c.setParent(parent);
	    		}
	    		temp.dispose();
	    		temp = null;
	    		kids = null;
	    		parent.layout();
    		});
    		super.refresh();
    	}else {
    		Display.getDefault().asyncExec(()->{
    			if (notValidCanvas == null) {
	    			kids = new Control[parent.getChildren().length];
	    			int i = 0;
	    			for (Control c : parent.getChildren()) {
	    				kids[i++] = c;
	    			}
	    			temp = new Shell();
	    			for (Control c : parent.getChildren()) c.setParent(temp);
	    			
	    			notValidCanvas = new Canvas(parent, SWT.NONE);
	    			notValidCanvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	    			notValidCanvas.addPaintListener(new PaintListener() {
						
						@Override
						public void paintControl(PaintEvent e) {
							// TODO Auto-generated method stub
							String str = "Map is not valid for this summary query";
							e.gc.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GREEN));
							e.gc.fillRectangle(0, 0, notValidCanvas.getBounds().width, notValidCanvas.getBounds().height);
							
							e.gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
							int w = e.gc.textExtent(str).x;
							w = (notValidCanvas.getBounds().width - w) / 2;
							e.gc.drawText(str,w, notValidCanvas.getBounds().height / 2);
							
						}
					});
	    			notValidCanvas.moveAbove(parent.getChildren()[0]);
	    			notValidCanvas.setBounds(parent.getBounds());
    			}
    			
//    			parent.addListener(SWT.Resize, e->{
//    				if (notValidCanvas != null) {
//    					System.out.println(parent.getBounds());
//    					notValidCanvas.setBounds(parent.getBounds());		
////    	    			notValidCanvas.moveAbove(parent.getChildren()[0]);
//    					notValidCanvas.redraw();
//
//    					
//    				}
//    			});
    			
    		});
    		
    		//remove layer
    		//disable map
    		
    	}
    }
}
