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
package org.wcs.smart.report.birt.map.properties;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;

import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.report.birt.map.BirtMapUtils;
import org.wcs.smart.report.birt.map.SmartMapItemPlugIn;
import org.wcs.smart.report.birt.map.internal.Messages;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Style cell editor for styles
 * 
 * 
 * @author egouge
 * 
 */
public class StyleCellEditor extends DialogCellEditor {

	private Layer layer;
	private Map map;
	
	private ColumnLabelProvider lblProvider;
	
	public StyleCellEditor(Composite parent, ColumnLabelProvider lblProvider) {
		super(parent);
		this.lblProvider = lblProvider;
	}

	
	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow) {

		try {
			map = ProjectFactory.eINSTANCE.createMap();
			final LayerDefinition mapLayer = (LayerDefinition) super.getValue();
			final OdaDataSetHandle ds = mapLayer.handle;
			
			Job j = new Job(Messages.StyleCellEditor_CreateMapLayerJobName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// add a layer to the map

					try {
						if (mapLayer.mapLayer != null){
							
							List<? extends IGeoResource> resources = mapLayer.mapLayer.createLayer(ds, null);
							IGeoResource iGeoResource = (IGeoResource) resources.get(0);
							ArrayList<IGeoResource> thisresources = new ArrayList<IGeoResource>();
							thisresources.add(iGeoResource);
							
							AddLayersCommand cmd = new AddLayersCommand(resources);
							map.executeSyncWithoutUndo(cmd);
							layer = cmd.getLayers().get(0);
						
							//TODO: this needs testing I don't think layer.getgeoresource returns the correct resource
							StyleBlackboard sb = null;
							if (mapLayer.style != null){
								//use user defined style for map
								sb = BirtMapUtils.parseStyleString(mapLayer.style);
							}else if (mapLayer.mapLayer.getDefaultStyle(ds, layer.getGeoResource()) != null){
								//use default style defined by the map layer
								sb = mapLayer.mapLayer.getDefaultStyle(ds, layer.getGeoResource());
							}else{
								//no defined style; udig will look in the georesource 
								//for an later we'll look in the georesource for sld style
								sb = null;
							}
							
							if (sb != null){
								layer.getStyleBlackboard().clear();
								if (sb.getContent() != null){
									layer.getStyleBlackboard().addAll(sb);
								}
							}
						}
					} catch (Exception ex) {
						SmartMapItemPlugIn.displayLog(Messages.StyleCellEditor_Error_CouldNotCreateStyleEditor + ex.getMessage(), ex);
					}
					return Status.OK_STATUS;
				}

			};
			j.schedule();
			j.join();

			SmartOpenStyleEditorAction action = new SmartOpenStyleEditorAction(layer);
			action.run();
			
			
			CatalogPlugin.getDefault().getLocalCatalog().remove(layer.getGeoResource().service(null));
			if (action.getBlackboard() == null){
				//user cancelled
				return null;
			}
			String savedBlackboard = StyleManager.INSTANCE.asString(action.getBlackboard());
			return savedBlackboard;
			
		} catch (Exception ex) {
			SmartMapItemPlugIn.displayLog(Messages.StyleCellEditor_Error_CouldNotOpenStyleDialog + ex.getMessage(), ex);
		}
		return null;
	}

	@Override
	public boolean dependsOnExternalFocusListener(){
		return false;
	}

	/**
	 * Updates the size of the widget
	 */
	@Override
	public LayoutData getLayoutData() {
		LayoutData data = super.getLayoutData();
		data.minimumHeight = getDefaultLabel().computeSize(SWT.DEFAULT,
				SWT.DEFAULT).y;
		return data;
	}
	
	
	@Override
    protected void updateContents(Object value) {
        if (getDefaultLabel() == null || lblProvider == null) {
			return;
		}
        String text = lblProvider.getText(value);
        if (text != null){
        	getDefaultLabel().setText(text);
        }
    }

}
