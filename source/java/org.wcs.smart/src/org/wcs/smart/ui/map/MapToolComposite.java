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

import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.tool.display.ToolManager;
import net.refractions.udig.project.ui.internal.tool.display.ToolProxy;
import net.refractions.udig.project.ui.tool.IToolManager;
import net.refractions.udig.project.ui.tool.ModalTool;
import net.refractions.udig.tool.info.InfoTool;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.udig.SetBasemapTool;

/**
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MapToolComposite {

	
	private String tools[] = new String[]{"org.wcs.smart.udig.AddLayer", 
			SetBasemapTool.ID, 
			"org.wcs.smart.udig.ZoomExtents", 
			"net.refractions.udig.tools.Pan",
			"net.refractions.udig.tools.Zoom", InfoTool.ID};
	
	private List<ToolItem> items = new ArrayList<ToolItem>();
	
	public MapToolComposite(){
		
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
	
	
	private void select(ToolItem item){
		ToolProxy mi = ((ToolProxy)item.getData());
		if (mi.getType() == 1){	//modal tool proxy
			for (ToolItem it : items){
				if (!item.equals(it)){
					it.setSelection(false);
				}
			}
		}
		ApplicationGIS.getToolManager().getToolAction(mi.getId(), mi.getCategoryId()).run();
	}
	
}
