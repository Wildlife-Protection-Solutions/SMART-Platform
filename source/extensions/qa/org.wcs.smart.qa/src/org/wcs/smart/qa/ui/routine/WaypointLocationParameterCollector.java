/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.routine;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hibernate.Query;
import org.hibernate.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.qa.QaPlugIn;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.model.QaRoutineParameter;
import org.wcs.smart.qa.routine.LocationRoutineType;
import org.wcs.smart.qa.routine.LocationRoutineType.GeometryType;
import org.wcs.smart.qa.ui.configure.IParameterCollector;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;
import com.vividsolutions.jts.io.WKTReader;

/**
 * A parameter collector for getting the polygon to use for validating
 * waypoint locations.
 * 
 * @author Emily
 *
 */
public class WaypointLocationParameterCollector extends IParameterCollector {

	private static final String GEOMETRY_KEY = "GEOMETRY"; //$NON-NLS-1$
	private Composite compShp;
	private Composite compArea;
	private Composite compTxt;
	
	private Button btnOpFile;
	private Button btnOpArea;
	private Button btnOpText;
	
	private ComboViewer cmbViewer ;
	private Text txtWkt;
	private Text txtSummary;
	
	private ControlDecoration cdWkt;
	private ControlDecoration cdArea;
	private ControlDecoration cdFile;
	
	private WKTReader reader = new WKTReader();
	
	private boolean isValid = false;
	
	@Override
	public void createUi(Composite composite) {

		Composite panel = new Composite(composite, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		btnOpFile = new Button(panel, SWT.RADIO);
		btnOpFile.setText("Upload custom polygon from shapefile");
		
		compShp = createShpPanel(panel);
		
		btnOpArea = new Button(panel, SWT.RADIO);
		btnOpArea.setText("Use a Conservation Area defined boundary ");
		
		compArea = createAreaPanel(panel);
		
		btnOpText= new Button(panel, SWT.RADIO);
		btnOpText.setText("Define area using WKT of polygon");
		
		compTxt = createWktPanel(panel);
		
		btnOpFile.addListener(SWT.Selection, e->updatePanels());
		btnOpArea.addListener(SWT.Selection, e->updatePanels());
		btnOpText.addListener(SWT.Selection, e->updatePanels());
		
		btnOpFile.setSelection(true);
		updatePanels();
	}

	public boolean isValid(){
		return this.isValid;
	}
	
	private void validate(){
		isValid = true;
		if (btnOpFile.getSelection()){
			if (txtSummary.getData(GEOMETRY_KEY) == null){
				isValid = false;
				cdFile.setDescriptionText("A shapefile must be selected.");
				cdFile.show();
			}else{
				cdFile.hide();
			}
		}else if (btnOpArea.getSelection()){
			if (cmbViewer.getSelection().isEmpty()){
				isValid = false;
				cdArea.setDescriptionText("Area layer must be selected");
				cdArea.show();
			}else{
				cdArea.hide();
				isValid = true;
			}
		}else if (btnOpText.getSelection()){
			try{
				Geometry g = reader.read(txtWkt.getText());
				if (g instanceof Polygon || g instanceof MultiPolygon){
					cdWkt.hide();
				}else{
					cdWkt.setDescriptionText("Not a valid polygon or multipolygon ");
					cdWkt.show();
					isValid = false;
				}
			}catch (Exception ex){
				cdWkt.setDescriptionText("Not a valid geometry");
				cdWkt.show();
				isValid = false;
			}
		}
		fireListeners();
	}
	private void updatePanels(){
		if (btnOpFile.getSelection()){
			enablePanel(compShp, true);
			enablePanel(compArea, false);
			enablePanel(compTxt, false);
		}else if (btnOpArea.getSelection()){
			enablePanel(compShp, false);
			enablePanel(compArea, true);
			enablePanel(compTxt, false);
		}else if (btnOpText.getSelection()){
			enablePanel(compShp, false);
			enablePanel(compArea, false);
			enablePanel(compTxt, true);
		}
		validate();
	}
	private Composite createShpPanel(Composite parent){
		Composite pnl = new Composite(parent, SWT.NONE);
		pnl.setLayout(new GridLayout(2, false));
		pnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)pnl.getLayoutData()).horizontalIndent = 15;
		
		txtSummary = new Text(pnl, SWT.BORDER | SWT.READ_ONLY);
		txtSummary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		cdFile = new ControlDecoration(txtSummary, SWT.LEFT | SWT.TOP);
		cdFile.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdFile.hide();
		
		Button btnBrowse = new Button(pnl, SWT.PUSH);
		btnBrowse.setText("Select File");
		
		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(parent.getShell());
			fd.setFilterExtensions(new String[]{"*.shp", "*.*"});
			fd.setFilterNames(new String[]{"Shapefiles (*.shp)", "All Files (*.*)"});
			
			String file = fd.open();
			if(file != null){
				try {
					readShapefile(Paths.get(file).toUri().toURL());
				} catch (Exception e1) {
					QaPlugIn.displayLog("Error reading shapefile: " + e1.getMessage(), e1);
				}
			}
		});
		return pnl;
	}
	
	private Composite createAreaPanel(Composite parent){
		Composite pnl = new Composite(parent, SWT.NONE);
		pnl.setLayout(new GridLayout(2, false));
		pnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)pnl.getLayoutData()).horizontalIndent = 15;
		Label l = new Label(pnl, SWT.NONE);
		l.setText("Area Boundary Layer:");
		l.setToolTipText("If no layers appear here you have not configured any geometries for the conservation area boundary layers");
		cmbViewer = new ComboViewer(pnl, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Area.AreaType) return SmartLabelProvider.getAreaTypeName((AreaType) element);
				return super.getText(element);
			}
		});

		cmbViewer.setInput(DialogConstants.LOADING_TEXT);
		cmbViewer.setInput(Area.AreaType.values());
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbViewer.getControl().addListener(SWT.Selection, e->validate());

		cdArea = new ControlDecoration(cmbViewer.getControl(), SWT.LEFT | SWT.TOP);
		cdArea.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdArea.hide();
	
		Job j = new Job("init areas"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Area.AreaType> types = new ArrayList<>();
				
				Session s = HibernateManager.openSession();
				try{
					String query = "SELECT type FROM Area WHERE conservationArea = :ca GROUP BY type having count(*) > 0"; //$NON-NLS-1$
					Query q = s.createQuery(query);
					q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
					
					types.addAll(q.list());
					
				}catch (Exception ex){
					QaPlugIn.displayLog("Unable to read conservation area boundary layers from the database: " + ex.getMessage(), ex);
				}finally{
					s.close();
				}
				Display.getDefault().syncExec(()->{
					ISelection selection = cmbViewer.getSelection();
					cmbViewer.setInput(types);	
					cmbViewer.setSelection(selection);
				});
				return Status.OK_STATUS;
			} 
			
		};
		j.setSystem(true);
		j.schedule();
		
		return pnl;
	}
	
	
	private Composite createWktPanel(Composite parent){
		Composite pnl = new Composite(parent, SWT.NONE);
		pnl.setLayout(new GridLayout(2, false));
		pnl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)pnl.getLayoutData()).horizontalIndent = 15;
		
		Label l = new Label(pnl, SWT.NONE);
		l.setText("WKT:");
		l.setToolTipText("Well Known Text");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		txtWkt = new Text(pnl, SWT.BORDER | SWT.MULTI);
		txtWkt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		cdWkt = new ControlDecoration(txtWkt, SWT.LEFT | SWT.TOP);
		cdWkt.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdWkt.hide();
		
		txtWkt.addListener(SWT.Modify, e->validate());
		return pnl;
	}
	
	private void enablePanel(Composite pnl, boolean enable){
		List<Composite> kids = new ArrayList<>();
		kids.add(pnl);
		pnl.setEnabled(enable);
		while(!kids.isEmpty()){
			Composite kid = kids.remove(0);
			for (Control c : kid.getChildren()){
				c.setEnabled(enable);
				if (c instanceof Composite){
					kids.add((Composite)c);
				}
			}
		}
	}
	
	
	private void readShapefile(URL url) {
		Shell shell = btnOpArea.getShell();
		txtSummary.setData(GEOMETRY_KEY, null);
		final ProgressMonitorDialog ppd = new ProgressMonitorDialog(shell);
		try {
			ppd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Reading shapefile", IProgressMonitor.UNKNOWN);

					SimpleFeatureCollection collection = null;
					try{
						FileDataStore store = FileDataStoreFinder.getDataStore(url);
						collection = store.getFeatureSource().getFeatures();
					}catch (final Exception ex){
						QaPlugIn.displayLog("Error reading shapefile", ex);
						return;
					}
					
					if (collection.getSchema().getCoordinateReferenceSystem() == null){
						//check projection
						shell.getDisplay().syncExec(()->{
								MessageDialog.openError(ppd.getShell(), "Shapefile Error", "No projection defined with shapefile.  Ensure a .prj file exists.");
						});
						return;
					}
					MathTransform transform;
					try {
						transform = CRS.findMathTransform(collection.getSchema().getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS);
					
						List<Geometry> geometries = new ArrayList<>();
						try(SimpleFeatureIterator it = collection.features()) {
							while (it.hasNext()) {
								SimpleFeature sf = it.next();
								if (monitor.isCanceled()) {
									break;
								}
	
								/// - geometry; ensure they are valid and simple
								Geometry geom = (Geometry) sf.getDefaultGeometry();
								geom = JTS.transform(geom, transform);
								if (!geom.isValid() || !geom.isSimple()){
									//try buffer 0 to clean up and check again
									geom = geom.buffer(0);
									if (!geom.isValid() || !geom.isSimple()){
										//still not valid and not simple so error out
										throw new Exception("Invalid geometry found in shapefile.");
									}
								}
								if (geom instanceof Polygon || geom instanceof MultiPolygon){
									geometries.add(geom);
								}
							}
						}
						if(!geometries.isEmpty()){
							Envelope r = geometries.get(0).getEnvelopeInternal();
							for (int i = 1; i < geometries.size(); i ++){
								r.expandToInclude(geometries.get(i).getEnvelopeInternal());
							}
							shell.getDisplay().syncExec(()->{
								txtSummary.setData(GEOMETRY_KEY, geometries);
								txtSummary.setText(MessageFormat.format("Bounds: ({0} {1}, {2}, {3})", r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY()));
							});
						}
						
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null && e.getCause() != null) msg = e.getCause().getMessage();
			QaPlugIn.displayLog("Error reading shapefile: " +msg, e);
		}
		validate();
	}
	
	
	@Override
	public void initUi(QaRoutine routine) {
		QaRoutineParameter areaParam = (QaRoutineParameter)routine.findParameter(LocationRoutineType.LOCATION_PARAM_ID);
		if (areaParam == null) return;
		
		if (areaParam.getStringValue().startsWith(GeometryType.AREA.key)){
			btnOpArea.setSelection(true);
			btnOpFile.setSelection(false);
			btnOpText.setSelection(false);
			
			String areaType = areaParam.getStringValue().split(":")[1];
			Area.AreaType type = Area.AreaType.valueOf(areaType);
			if (type != null){
				cmbViewer.setSelection(new StructuredSelection(type));
			}else{
				cmbViewer.setSelection(null);
			}
		}else if (areaParam.getStringValue().startsWith(GeometryType.FILE.key)){
			btnOpArea.setSelection(false);
			btnOpFile.setSelection(true);
			btnOpText.setSelection(false);
			
			if (areaParam.getByteValue() != null){
				WKBReader reader = new WKBReader();
				Geometry g = null;
				try{
					g = reader.read(areaParam.getByteValue());
				}catch (Exception e){
					QaPlugIn.log(e.getMessage(), e);
				}
				if (g != null){
					List<Geometry> geometries = new ArrayList<Geometry>();
					if (g instanceof GeometryCollection){
						for (int i = 0; i < ((GeometryCollection)g).getNumGeometries(); i ++){
							geometries.add(((GeometryCollection)g).getGeometryN(i));
						}
					}else{
						geometries.add(g);
					}
					if (!geometries.isEmpty()){
						Envelope r = geometries.get(0).getEnvelopeInternal();
						for (int i = 1; i < geometries.size(); i ++){
							r.expandToInclude(geometries.get(i).getEnvelopeInternal());
						}
				
						txtSummary.setData(GEOMETRY_KEY, geometries);
						txtSummary.setText(MessageFormat.format("Bounds: ({0} {1}, {2}, {3})", r.getMinX(), r.getMinY(), r.getMaxX(), r.getMaxY()));
					}
				}
			}
		}else if (areaParam.getStringValue().startsWith(GeometryType.WKT.key)){
			btnOpArea.setSelection(false);
			btnOpFile.setSelection(false);
			btnOpText.setSelection(true);
			
			if (areaParam.getByteValue() != null){
				txtWkt.setText(new String(areaParam.getByteValue()));
			}
		}
		updatePanels();
	}

	@Override
	public void updateParameters(QaRoutine routine) {
		QaRoutineParameter areaParam = (QaRoutineParameter)routine.findParameter(LocationRoutineType.LOCATION_PARAM_ID);
		if (areaParam == null){
			areaParam = new QaRoutineParameter();
			areaParam.setParameterId(LocationRoutineType.LOCATION_PARAM_ID);
			areaParam.setQaRoutine(routine);
			if (routine.getParameters() == null){
				routine.setParameters(new ArrayList<>());
			}
			routine.getParameters().add(areaParam);
		}
		if (btnOpFile.getSelection()){
			List<Geometry> geometries = (List<Geometry>) txtSummary.getData(GEOMETRY_KEY);
				
			WKBWriter writer = new WKBWriter();
			GeometryCollection gc = new GeometryCollection(geometries.toArray(new Geometry[geometries.size()]), GeometryFactoryProvider.getFactory());
			StringBuilder sb = new StringBuilder();
			sb.append(LocationRoutineType.GeometryType.FILE.key);
			areaParam.setStringValue(sb.toString());
			areaParam.setByteValue(writer.write(gc));
		}else if (btnOpArea.getSelection()){
			Area.AreaType t = (Area.AreaType)((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
			StringBuilder sb = new StringBuilder();
			sb.append(LocationRoutineType.GeometryType.AREA.key);
			sb.append(":"); //$NON-NLS-1$
			sb.append(t.name());
			areaParam.setStringValue(sb.toString());
			areaParam.setByteValue(null);
		}else if (btnOpText.getSelection()){
			StringBuilder sb = new StringBuilder();
			sb.append(LocationRoutineType.GeometryType.WKT.key);
			areaParam.setStringValue(sb.toString());
			String wkt = txtWkt.getText();
			areaParam.setByteValue(wkt.getBytes());
		}
		
	}

}
