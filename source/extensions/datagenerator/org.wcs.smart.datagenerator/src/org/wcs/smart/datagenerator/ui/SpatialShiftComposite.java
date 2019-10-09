/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.datagenerator.ui;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.ZoomCommand;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.ui.render.glass.GlassPane;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ca.Area;
import org.wcs.smart.datagenerator.DataGeneratorPlugIn;
import org.wcs.smart.datagenerator.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.util.GeometryUtils;

/**
 * Panel for gathering spatial shift data. (new center point
 * and shift scale).
 * 
 * @author Emily
 *
 */
public class SpatialShiftComposite  extends Composite{

	private DataGeneratorView view;
	
	@Inject private UISynchronize ui;
	
	private MapViewer currentMapviewer;
	private MapViewer newMapviewer;
	
	private Text txtCx;
	private Text txtCy;
	private Text txtScale;
	
	private ControlDecoration cdCx;
	private ControlDecoration cdCy;
	private ControlDecoration cdScale;
    	    
	private Button btnDoIt;
	
	private Label lblCurrentBounds = null;
	private Envelope currentBounds = null;
	
	private boolean doValidate = true;
	private List<Layer> shapefileLayers;
	
	public SpatialShiftComposite(Composite parent, DataGeneratorView view) {
		super(parent, SWT.NONE);
		this.view = view;
		createContents();
		shapefileLayers = new ArrayList<>();
	}

	private void createContents() {
		setLayout(new GridLayout());
		
		Composite infoComp = view.createHeader(this, Messages.SpatialShiftComposite_ShiftText);
		infoComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		FormToolkit toolkit = view.toolkit;
		
		Composite top = toolkit.createComposite(this);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		
		toolkit.createLabel(top, Messages.SpatialShiftComposite_CurrentBBox, SWT.NONE);
		toolkit.createLabel(top, Messages.SpatialShiftComposite_NewBbox);
		
		//existing map
		currentMapviewer = new MapViewer(top, SWT.BORDER | SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.SpatialShiftComposite_MapName);
		currentMapviewer.setMap(map);
		currentMapviewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	    ((GridData)currentMapviewer.getControl().getLayoutData()).heightHint = 300;
		
	    
	    //new map
	    newMapviewer = new MapViewer(top, SWT.BORDER | SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.SpatialShiftComposite_MapName);
		newMapviewer.setMap(map);
		newMapviewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite shiftComp = toolkit.createComposite(this,  SWT.NONE);
		shiftComp.setLayout(new GridLayout(3, false));
		((GridLayout)shiftComp.getLayout()).marginWidth = 0;
		((GridLayout)shiftComp.getLayout()).marginHeight = 0;
		
		Label lbl = toolkit.createLabel(shiftComp, Messages.SpatialShiftComposite_FromLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		lblCurrentBounds = toolkit.createLabel(shiftComp, ""); //$NON-NLS-1$
		lblCurrentBounds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Hyperlink refreshLnk = toolkit.createHyperlink(shiftComp, Messages.SpatialShiftComposite_refreshLink, SWT.NONE);
		refreshLnk.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				refreshBounds();
			}
		});
		
		lbl = toolkit.createLabel(shiftComp, Messages.SpatialShiftComposite_ToLabel);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lbl.getLayoutData()).verticalIndent = 3;
		
		Composite cx = toolkit.createComposite(shiftComp, SWT.NONE);
		cx.setLayout(new GridLayout(2, false));
		((GridLayout)cx.getLayout()).marginHeight = 0;
		cx.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		lbl = toolkit.createLabel(cx, Messages.SpatialShiftComposite_xLabel);
		txtCx = toolkit.createText(cx, ""); //$NON-NLS-1$
		txtCx.setData(0);
		txtCx.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtCx.getLayoutData()).widthHint = 100;
		cdCx = view.createCd(txtCx);
		cdCx.hide();
		
		lbl = toolkit.createLabel(cx, Messages.SpatialShiftComposite_yLabel);
		txtCy = toolkit.createText(cx, ""); //$NON-NLS-1$
		txtCy.setData(0);
		txtCy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtCy.getLayoutData()).widthHint = 100;
		cdCy = view.createCd(txtCy);
		cdCy.hide();
		
		lbl = toolkit.createLabel(cx, Messages.SpatialShiftComposite_scaleLabel);
		txtScale = toolkit.createText(cx, "1"); //$NON-NLS-1$
		txtScale.setData(1);
		txtScale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtScale.getLayoutData()).widthHint = 100;
		cdScale = view.createCd(txtScale);
		cdScale.hide();
		
		Hyperlink popShp = toolkit.createHyperlink(cx, Messages.SpatialShiftComposite_ShapefileOp, SWT.NONE);
		popShp.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		
		btnDoIt = toolkit.createButton(cx, Messages.SpatialShiftComposite_ShiftButton, SWT.PUSH);
		btnDoIt.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		btnDoIt.addListener(SWT.Selection, e->view.doSpatialShift());
		
		txtCx.addListener(SWT.Modify, e->updateBounds());
		txtCy.addListener(SWT.Modify, e->updateBounds());
		txtScale.addListener(SWT.Modify, e->updateBounds());
		popShp.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				updateFromData();
			}
		});
		
		
		refreshBounds();
	}
	
	public void refreshBounds() {
		refreshBounds.schedule();
	}
	
	private void updateBounds() {
		if (!doValidate) return;
		
		double cx = 0;
		double cy = 0;
		double scale = 0;
		try {
			cx = Double.parseDouble(txtCx.getText());
			if (cx < -180 || cy > 180) {
				throw new Exception(Messages.SpatialShiftComposite_invalidX);
			}
			txtCx.setData(cx);
		}catch (Exception ex) {
			cdCx.setDescriptionText(ex.getMessage());
			cdCx.show();
			btnDoIt.setEnabled(false);
			return;
		}
		cdCx.hide();
		
		try {
			cy = Double.parseDouble(txtCy.getText());
			if (cy < -90 || cy > 90) {
				throw new Exception(Messages.SpatialShiftComposite_invalidY);
			}
			txtCy.setData(cy);
		}catch (Exception ex) {
			cdCy.setDescriptionText(ex.getMessage());
			cdCy.show();
			btnDoIt.setEnabled(false);
			return;
		}
		cdCy.hide();
		
		try {
			scale = Double.parseDouble(txtScale.getText());
			if (scale < 0) {
				throw new Exception(Messages.SpatialShiftComposite_invalidScale);
			}
			txtScale.setData(scale);
		}catch (Exception ex) {
			cdScale.setDescriptionText(ex.getMessage());
			cdScale.show();
			btnDoIt.setEnabled(false);
			return;
		}
		cdScale.hide();
		
		Coordinate currentCenter = currentBounds.centre();
		Coordinate newCenter = new Coordinate(cx, cy);
		
		
		Coordinate ll = new Coordinate(currentBounds.getMinX(), currentBounds.getMinY());
		ll.x = (currentCenter.x - ll.x) * scale + newCenter.x;
		ll.y = (currentCenter.y - ll.y) * scale + newCenter.y;
		
		Coordinate ur = new Coordinate(currentBounds.getMaxX(), currentBounds.getMaxY());
		ur.x = (currentCenter.x - ur.x) * scale + newCenter.x;
		ur.y = (currentCenter.y - ur.y) * scale + newCenter.y;
		
		
		Envelope newEnvelope = new Envelope(ll, ur);
		addGlassPane(newMapviewer, newEnvelope);
		
		btnDoIt.setEnabled(true);
	}
	
	public double getScale() {
		return (double)txtScale.getData();
	}
	
	public Coordinate getCurrentCenter() {
		return new Coordinate(currentBounds.centre());
	}
	
	public Coordinate getNewCenter() {
		return new Coordinate((double)txtCx.getData(), (double)txtCy.getData());
	}
	
	private void updateFromData() {
		
		LayerSelectionDialog dialog = new LayerSelectionDialog(getShell(), view);
		if (dialog.open() != Window.OK) return;
		
		if (dialog.getBounds() != null) {
			updateXY(dialog.getBounds());
		
		}else if (dialog.getShapefile() != null) {
			Path sfile = dialog.getShapefile();
			//load shapefile
			ProgressMonitorDialog pd = new ProgressMonitorDialog(getShell());
			try {
				pd.run(true,  false,new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							List<IService> services = CatalogPlugin.getDefault().getLocalCatalog().constructServices(sfile.toUri().toURL(),  new NullProgressMonitor());
							if (services.size() == 0) throw new Exception(Messages.SpatialShiftComposite_ResourcesNotFound + sfile.toString());
							List<? extends IGeoResource> resources = services.get(0).resources(new NullProgressMonitor());
							
							if (!shapefileLayers.isEmpty()) {
								DeleteLayersCommand d = new DeleteLayersCommand(shapefileLayers.toArray(new ILayer[shapefileLayers.size()]));
								newMapviewer.getMap().executeSyncWithoutUndo(d);
								shapefileLayers.clear();
							}
							AddLayersCommand add = new AddLayersCommand(resources);
							newMapviewer.getMap().executeSyncWithoutUndo(add);
							shapefileLayers.addAll(add.getLayers());
							
							//compute center and approximate scale
							SimpleFeatureSource fs = resources.get(0).resolve(SimpleFeatureSource.class, new NullProgressMonitor());
							//Reproject
							ReferencedEnvelope re = fs.getFeatures().getBounds();
							Envelope e = JTS.transform(re, CRS.findMathTransform(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS));
							updateXY(e);
						}catch (Exception ex) {
							DataGeneratorPlugIn.log(ex.getMessage(), ex);
							ui.syncExec(()->{
								MessageDialog.openError(getShell(), Messages.SpatialShiftComposite_ReadErrorTitle, MessageFormat.format(Messages.SpatialShiftComposite_ReadErrorMsg, sfile.toString(), ex.getMessage()));
							});
						}
							
						return;
					}
				});
			}catch (Exception ex) {
				DataGeneratorPlugIn.log(ex.getMessage(), ex);
			}
		}
	}
	
	private void updateXY(Envelope e) {
		Coordinate center = new Coordinate(e.getMinX() + (e.getWidth() / 2.0), e.getMinY() + (e.getHeight() / 2.0)); 
		//approximate scale
		double scale = Math.max(e.getWidth() / currentBounds.getWidth(), e.getHeight() / currentBounds.getHeight());
		ui.syncExec(()->{
			try {
				doValidate = false;
			
				txtCx.setText(String.valueOf( ((int)Math.round(center.x * 1000)) / 1000.0 ) );
				txtCy.setText(String.valueOf( ((int)Math.round(center.y * 1000)) / 1000.0 ) );
				txtScale.setText(String.valueOf( ((int)Math.round(scale * 1000)) / 1000.0 ) );
				
			}finally {
				doValidate = true;
				updateBounds();
			}
		});
	}
	
	private void addGlassPane(MapViewer map, Envelope env) {
		
		//transform env to map crs
		try {
			
			MathTransform transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, map.getMap().getViewportModel().getCRS());
			env = JTS.transform(env, transform);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		
		//expand map bounds to include env
		map.getMap().sendCommandSync(new ZoomExtentCommand());
		Envelope e2 = new Envelope(env);
		e2.expandBy(  Math.max(env.getWidth(), env.getHeight()) * 0.1);
		map.getMap().sendCommandSync(new ZoomCommand(e2));
		
		Point c1 = map.getMap().getViewportModel().worldToPixel(new Coordinate(env.getMinX(), env.getMinY()));
		Point c2 = map.getMap().getViewportModel().worldToPixel(new Coordinate(env.getMaxX(), env.getMaxY()));
		
		map.getViewport().setGlass(new GlassPane(map.getViewport()) {

			@Override
			public void draw(GC graphics) {
				graphics.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				graphics.setLineWidth(2);
				graphics.drawRectangle(c1.x, c1.y, c2.x - c1.x, c2.y - c1.y);
			}
			
		});
		map.getMap().getRenderManager().refresh(null);
	}
	
	private Job refreshBounds = new Job("refresh current bounds") { //$NON-NLS-1$
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			newMapviewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
			newMapviewer.getMap().getViewportModelInternal().setCRS(Area.AREA_CRS);
			LoadDefaultLayersJob job2 = new LoadDefaultLayersJob(newMapviewer.getMap(), false);
			job2.schedule();
			try {
				job2.join();
				
				//fix a bug with load default layers job 
				//TODO: fix this in SMART7
				List<Layer> newlayers = new ArrayList<>(newMapviewer.getMap().getLayersInternal());
				newMapviewer.getMap().getLayersInternal().removeAll(newlayers);
				newMapviewer.getMap().getLayersInternal().addAll(0, newlayers);
				newMapviewer.getMap().sendCommandSync(new ZoomExtentCommand());
			
				newMapviewer.getMap().getViewportModelInternal().setCRS(SmartDB.DATABASE_CRS);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			currentMapviewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
			currentMapviewer.getMap().getViewportModelInternal().setCRS(Area.AREA_CRS);
			LoadDefaultLayersJob job = new LoadDefaultLayersJob(currentMapviewer.getMap(), false);
			job.schedule();
			try {
				job.join();
				
				//fix a bug with load default layers job 
				//TODO: fix this in SMART7
				List<Layer> newlayers = new ArrayList<>(currentMapviewer.getMap().getLayersInternal());
				currentMapviewer.getMap().getLayersInternal().removeAll(newlayers);
				currentMapviewer.getMap().getLayersInternal().addAll(0, newlayers);
				
				currentMapviewer.getMap().getViewportModelInternal().setCRS(SmartDB.DATABASE_CRS);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			Envelope env = null;
			try(Session s = HibernateManager.openSession()){
				
				Object[] data = (Object[]) s.createQuery("SELECT min(rawX), min(rawY), max(rawX), max(rawY) FROM Waypoint WHERE sourceId = :source AND conservationArea = :ca") //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.setParameter("source", PatrolWaypointSource.PATROL_WP_SOURCE_ID)  //$NON-NLS-1$
					.uniqueResult();
				
				double minx = 0;
				double miny = 0;
				double maxx = 0;
				double maxy = 0;
				
				if (data[0] != null) {
					minx = (double) data[0];
					miny = (double) data[1];
					maxx = (double) data[2];
					maxy = (double) data[3];	
				}
				
				
				env = new Envelope(minx,  maxx,  miny,  maxy);
				
				List<Patrol> patrols = QueryFactory.buildQuery(s, Patrol.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}) //$NON-NLS-1$
						.list();
			
				for (Patrol p : patrols) {
					for (PatrolLeg l : p.getLegs()) {
						for (PatrolLegDay pld : l.getPatrolLegDays()) {
							try {
								if (pld.getTrack() == null) continue;
								for (LineString ls : pld.getTrack().getLineStrings()) {
									env.expandToInclude(ls.getEnvelopeInternal());
								}
							}catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				}
			}
			
			SpatialShiftComposite.this.currentBounds = env;
			
			currentMapviewer.getMap().sendCommandSync(new ZoomExtentCommand());
			
			addGlassPane(currentMapviewer, env);
			

			ui.asyncExec(()->{
				doValidate = false;
				try {
					txtCx.setText(String.valueOf(SpatialShiftComposite.this.currentBounds.centre().x));
					txtCy.setText(String.valueOf(SpatialShiftComposite.this.currentBounds.centre().y));
				
					double x = ((int)Math.round( SpatialShiftComposite.this.currentBounds.centre().x * 1000.0)) / 1000.0;
					double y = ((int)Math.round( SpatialShiftComposite.this.currentBounds.centre().y * 1000.0)) / 1000.0;
					
					lblCurrentBounds.setText( x + ", " + y   ); //$NON-NLS-1$
					
					lblCurrentBounds.getParent().layout(true);
				}finally {
					doValidate = true;
					updateBounds();
				}
			});
			
			return Status.OK_STATUS;
		}
	};
}
