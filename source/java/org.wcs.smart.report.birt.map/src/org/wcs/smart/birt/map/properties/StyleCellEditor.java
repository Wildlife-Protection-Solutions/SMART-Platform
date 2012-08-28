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
package org.wcs.smart.birt.map.properties;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.StyleContent;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.StyleEntry;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.style.sld.SLDContent;

import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.XMLMemento;
import org.hibernate.Session;
import org.wcs.smart.birt.map.BirtMapUtils;
import org.wcs.smart.birt.map.SmartMapItemPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.map.udig.QueryService;
import org.wcs.smart.query.map.udig.QueryServiceFactory;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.util.SmartUtils;

/**
 * Style cell editor for styles
 * 
 * 
 * @author egouge
 * 
 */
public class StyleCellEditor extends DialogCellEditor {

	private Layer layer;
	private QueryService qs;

	private Map map;
	
	public StyleCellEditor(Composite parent) {
		super(parent);
	}

	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow) {

		try {
			map = ProjectFactory.eINSTANCE.createMap();
			LayerDefinition mapLayer = (LayerDefinition) super.getValue();
			final OdaDataSetHandle ds = mapLayer.handle;
			final Object style = BirtMapUtils.mementoToStyle(mapLayer.style);

			Job j = new Job("create map with layer") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					// add a layer to the map

					try {
						Session session = HibernateManager.openSession();
						session.beginTransaction();
						Query q = null;
						try {
							q = QueryHibernateManager.findQuery(session,
									SmartUtils.decodeHex(ds.getQueryText()),
									null);
						} finally {
							session.getTransaction().commit();
							session.close();
						}
						qs = QueryServiceFactory.generateQueryService(q);
						
						List<? extends IGeoResource> resources = qs.resources(null);
						IGeoResource iGeoResource = (IGeoResource) resources.get(0);
						ArrayList<IGeoResource> rsources = new ArrayList<IGeoResource>();
						rsources.add(iGeoResource);
						AddLayersCommand cmd = new AddLayersCommand(resources);
						map.executeSyncWithoutUndo(cmd);
						layer = cmd.getLayers().get(0);
						if (style != null) {
							layer.getStyleBlackboard()
									.put(SLDContent.ID, style);
						}
					} catch (Exception ex) {
						SmartMapItemPlugIn.displayLog("Could not create style editor: " + ex.getMessage(), ex);
					}
					return Status.OK_STATUS;
				}

			};
			j.schedule();
			j.join();

			SmartOpenStyleEditorAction action = new SmartOpenStyleEditorAction(layer);
			action.run();
			
			if (qs != null) {
				CatalogPlugin.getDefault().getLocalCatalog().remove(qs);
			}
			
			if (action.getSelectedStyle() == null){
				return null;
			}
			SLDContent sld = new SLDContent();
			XMLMemento memento = XMLMemento.createWriteRoot("styleEntry"); //$NON-NLS-1$
	        sld.save(memento, action.getSelectedStyle());
	        StringWriter writer = new StringWriter();
            memento.save(writer);
	        return writer.toString();
		} catch (Exception ex) {
			SmartMapItemPlugIn.displayLog("Could not open style dialog. " + ex.getMessage(), ex);
		}
		return null;
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

}
