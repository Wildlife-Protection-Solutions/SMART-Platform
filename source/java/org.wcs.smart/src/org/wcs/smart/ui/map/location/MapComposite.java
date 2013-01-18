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
package org.wcs.smart.ui.map.location;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.location.tool.SelectionTool;

/**
 * Map Composite
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class MapComposite extends Composite implements MapPart {

	private MapViewer mapViewer;
	private Map map;

	/**
	 * @param parent
	 * @param style
	 */
	public MapComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}

	private void createControls() {

		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		mapViewer = new MapViewer(this,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.MapComposite_Map_Name);
		mapViewer.setMap(map);
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(SmartDB.DATABASE_CRS);

		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				MapToolComposite.UDIG_ZOOM_EXTENT_ID,
				MapToolComposite.UDIG_PAN_ID,
				MapToolComposite.UDIG_ZOOM_ID,
				SelectionTool.ID };

		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(this);
		new MapInfoAreaComposite(this, SWT.NONE, mapViewer) ;

		tools.selectTool(MapToolComposite.UDIG_PAN_ID);

		LoadDefaultLayersJob layer = new LoadDefaultLayersJob(map, true, null);
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		layer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				map.getRenderManager().refresh(null);
			}
		});
		layer.schedule();

		getShell().addListener(SWT.Resize, new Listener(){
			Job j = new Job(Messages.MapComposite_MapResizeJob_Title){
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					map.getRenderManager().refresh(null);
					return Status.OK_STATUS;
				}
			};

			@Override
			public void handleEvent(Event event) {
				j.schedule(500);
			}
		});
	}

	@Override
	public Map getMap() {
		return map;
	}

	@Override
	public void openContextMenu() {
		mapViewer.openContextMenu();

	}

	@Override
	public void setFont(Control textArea) {
		mapViewer.setFont(textArea);

	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
		mapViewer.setSelectionProvider(selectionProvider);

	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

}
