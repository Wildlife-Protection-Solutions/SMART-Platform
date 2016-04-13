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

import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.report.birt.map.AddLayersCommand;
import org.wcs.smart.report.birt.map.BirtMapFactory;
import org.wcs.smart.report.birt.map.BirtMapUtils;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.SmartMapItemPlugIn;
import org.wcs.smart.report.birt.map.execute.BirtStyleManager;
import org.wcs.smart.report.birt.map.internal.Messages;
import org.wcs.smart.report.birt.map.item.LayerItem;
import org.wcs.smart.report.birt.map.udig.MapGeoResource;
import org.wcs.smart.report.birt.map.udig.MapQueryService;
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
			map = BirtMapFactory.createMap();
			final LayerItem mapLayer = (LayerItem) super.getValue();
			
			Job j = new Job(Messages.StyleCellEditor_CreateMapLayerJobName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// add a layer to the map

					try {
						MapQueryService service = new MapQueryService();
						MapLayerInfo info = new MapLayerInfo(mapLayer.getLayerName(), mapLayer.getLayerStyles(), mapLayer.getLayerType(), mapLayer.getGeometryColumn());
						MapGeoResource iGeoResource = new MapGeoResource((OdaDataSetHandle)mapLayer.getHandle().getDataSet(), info, service);
						
						ArrayList<IGeoResource> thisresources = new ArrayList<IGeoResource>();
						thisresources.add(iGeoResource);
						
						AddLayersCommand cmd = new AddLayersCommand(thisresources, 0);
						cmd.setMap(map);
						cmd.run(new NullProgressMonitor());
						
						layer = cmd.getLayers().get(0);
					
						StyleBlackboard sb = null;
						if (info.getMapStyle() != null){
							//use user defined style for map
							sb = BirtMapUtils.parseStyleString(info.getMapStyle());
						}else{
							
							Session session = HibernateManager.openSession();
							try{
								String queryText = ((OdaDataSetHandle)mapLayer.getHandle().getDataSet()).getQueryText();
								sb = BirtStyleManager.INSTANCE.getStyle(
										((OdaDataSetHandle)mapLayer.getHandle().getDataSet()).getExtensionID(),
										queryText, session);
							}finally{
								session.close();
							}
						}
						
						if (sb != null){
							layer.getStyleBlackboard().clear();
							if (sb.getContent() != null){
							layer.getStyleBlackboard().addAll(sb);
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
