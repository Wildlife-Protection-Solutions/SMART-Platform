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
package org.wcs.smart.ui.map;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.tool.display.ToolManager;
import org.locationtech.udig.project.ui.internal.tool.display.ToolProxy;
import org.locationtech.udig.project.ui.tool.IToolManager;
import org.locationtech.udig.project.ui.tool.ModalTool;
import org.wcs.smart.udig.AddLayerTool;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.ui.map.tool.BBoxInfoTool;

/**
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MapToolComposite {

	//these tools will only work in a view or editor part.  If using in a dialog
	//you need to use custom tools (see MapComposite.java)
	//This is due to a rendering problem with uDig; the commands are not passed on
	//to the map because the map is not in a "part" and therefore never
	//a active or viewable map.
	//see: https://locationtech.org/mhonarc/lists/udig-dev/msg22172.html
	public static final String UDIG_ZOOM_EXTENT_ID = "org.wcs.smart.udig.ZoomExtents"; //$NON-NLS-1$
	public static final String UDIG_PAN_ID = "org.locationtech.udig.tools.Pan"; //$NON-NLS-1$
	public static final String UDIG_ZOOM_ID = "org.locationtech.udig.tools.Zoom"; //$NON-NLS-1$
	public static final String UDIG_ZOOM_IN_ID = "org.locationtech.udig.tool.default.ZoomIn"; //$NON-NLS-1$
	public static final String UDIG_ZOOM_OUT_ID = "org.locationtech.udig.tool.default.ZoomOut"; //$NON-NLS-1$
	
	private String tools[] = new String[]{AddLayerTool.ID,  //$NON-NLS-1$
			SetBasemapTool.ID, 
			UDIG_ZOOM_EXTENT_ID,
			UDIG_PAN_ID,
			UDIG_ZOOM_ID,
			UDIG_ZOOM_IN_ID,
			UDIG_ZOOM_OUT_ID,
			//InfoTool.ID,
			BBoxInfoTool.ID};
	
	private List<ToolItem> items = new ArrayList<ToolItem>();
	private String currentToolId = null;
	
	public MapToolComposite(){
		
	}
	
	public MapToolComposite(String tools[]){
		this();
		this.tools = tools;
	}

	
	public void createComposite(Composite parent){
		items.clear();
		Composite toolBarComp = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(1, false);
		gl.marginLeft = 0;
		gl.marginRight = 0;
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		toolBarComp.setLayout(gl);
		toolBarComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		ToolBar toolBar = new ToolBar (toolBarComp, SWT.FLAT | SWT.NONE | SWT.VERTICAL);
		IToolManager toolManager = ApplicationGIS.getToolManager();
		
		for (int i = 0; i < tools.length; i ++){
			ToolProxy found = ((ToolManager)toolManager).findToolProxy(tools[i]);
			if (found instanceof ModalTool){
				int style = SWT.CHECK;
				 if (found.getType() != 1){	//modal tool proxy
					 style = SWT.PUSH;
				 }
				final ToolItem item = new ToolItem (toolBar, style);
				item.setImage (found.getImage());
    			item.setToolTipText(found.getToolTipText());
    			item.setData(found);
    			items.add(item);
    			item.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						select(item);
					}
				});
    		}
		}
        toolBar.pack();
	}
		
	/**
	 * Activates the tool with the given id
	 * @param id
	 */
	public void selectTool(String id){
		currentToolId = null;
		for (ToolItem it : items){
			if (  ((ToolProxy)it.getData()).getId().equals(id) ){
				select(it);
				return;
			}
		}
	}

	/**
	 * Returns tool with the given id if it exists
	 * @param id
	 */
	public ToolItem getTool(String id){
		for (ToolItem it : items){
			if (  ((ToolProxy)it.getData()).getId().equals(id) ){
				return it;
			}
		}
		return null;
	}
	
	private void select(ToolItem item){
		ToolProxy mi = ((ToolProxy)item.getData());
		if (mi.getType() == 1){	//modal tool proxy
			for (ToolItem it : items){
				if (!item.equals(it)){
					it.setSelection(false);
				}else{
					it.setSelection(true);
				}
			}
			currentToolId = mi.getId();	
		}
		ApplicationGIS.getToolManager().getToolAction(mi.getId(), mi.getCategoryId()).run();
		
	}
	
	public String getCurrentToolId(){
		return this.currentToolId;
	}
	
	public void selectLastTool(){
		if (this.currentToolId != null){
			selectTool(currentToolId);
		}
	}
	
}


