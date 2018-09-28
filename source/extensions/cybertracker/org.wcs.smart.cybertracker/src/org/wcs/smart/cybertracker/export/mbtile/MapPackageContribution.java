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
package org.wcs.smart.cybertracker.export.mbtile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

public class MapPackageContribution implements IPackageContribution{

	//package files
	public static final String MAPFILE = "map.mbtiles"; //$NON-NLS-1$
	
	//preference keys
	private static final String BASEMAP_PREF_KEY = MapPackageContribution.class.getCanonicalName() + ".basemap"; //$NON-NLS-1$
	private static final String BOUNDS_PREF_KEY = MapPackageContribution.class.getCanonicalName() + ".bounds"; //$NON-NLS-1$
	private static final String MINZOOM_PREF_KEY = MapPackageContribution.class.getCanonicalName() + ".minzoom"; //$NON-NLS-1$
	private static final String MAXZOOM_PREF_KEY = MapPackageContribution.class.getCanonicalName() + ".maxzoom"; //$NON-NLS-1$
	
	private ComboViewer cmbBasemap;
	private Text txtBounds;
	private ComboViewer minBounds, maxBounds;
	private Label warnLabel;
	private Label warnImage;
	private String prefKey;
	
	private MbTileGenerator generator;
	private Group mapComposite;
	
	private final static Integer[] validZooms = new Integer[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21};
	
	@Override
	public Composite createUi(Composite parent, String prefKey) {
		generator = new MbTileGenerator();
		
		this.prefKey = prefKey;
		
		mapComposite = new Group(parent, SWT.NONE);
		mapComposite.setLayout(new GridLayout(2, false));
		mapComposite.setText("Basemap Options");
		
		Label l = new Label(mapComposite, SWT.NONE);
		l.setText("Basemap:");
		
		cmbBasemap = new ComboViewer(mapComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbBasemap.setContentProvider(ArrayContentProvider.getInstance());
		cmbBasemap.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof BasemapDefinition) return ((BasemapDefinition) element).getName();
				return super.getText(element);
				
			}
		});
		cmbBasemap.setInput(new String[] {DialogConstants.LOADING_TEXT});
		cmbBasemap.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbBasemap.addSelectionChangedListener(e->basemapSelected());
		
		l = new Label(mapComposite, SWT.NONE);
		l.setText("Bounds:");
		
		Composite bnds = new Composite(mapComposite, SWT.NONE);
		bnds.setLayout(new GridLayout(2, false));
		((GridLayout)bnds.getLayout()).marginWidth = 0;
		((GridLayout)bnds.getLayout()).marginHeight = 0;
		bnds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtBounds = new Text(bnds, SWT.BORDER);
		txtBounds.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtBounds.setText("<map extents>");
		txtBounds.setEditable(false);
		
		Button btnSelectBm = new Button(bnds, SWT.PUSH);
		btnSelectBm.setText("...");
		
		l = new Label(mapComposite, SWT.NONE);
		l.setText("Zoom:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite zoomComp = new Composite(mapComposite, SWT.NONE);
		zoomComp.setLayout(new GridLayout(3, false));
		((GridLayout)zoomComp.getLayout()).marginWidth = 0;
		((GridLayout)zoomComp.getLayout()).marginHeight = 0;
		zoomComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite left = new Composite(zoomComp, SWT.NONE);
		left.setLayout(new GridLayout(4, false));
		((GridLayout)left.getLayout()).marginWidth = 0;
		((GridLayout)left.getLayout()).marginHeight = 0;
		left.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		l = new Label(left, SWT.NONE);
		l.setText("Min:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		minBounds = new ComboViewer(left, SWT.DROP_DOWN | SWT.READ_ONLY);
		minBounds.setContentProvider(ArrayContentProvider.getInstance());
		minBounds.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Integer)element).toString();
			}
		});
		minBounds.setInput(validZooms);
		minBounds.setSelection(new StructuredSelection(validZooms[0]));
		minBounds.addSelectionChangedListener(e->{
			if (getMinZoom() > getMaxZoom()) {
				minBounds.setSelection(new StructuredSelection(getMaxZoom()));
			}
			checkTiles();
		});
		minBounds.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		
		l = new Label(left, SWT.NONE);
		l.setText("Max:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		maxBounds = new ComboViewer(left, SWT.DROP_DOWN | SWT.READ_ONLY);
		maxBounds.setContentProvider(ArrayContentProvider.getInstance());
		maxBounds.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Integer)element).toString();
			}
		});
		maxBounds.setInput(validZooms);
		maxBounds.setSelection(new StructuredSelection(validZooms[validZooms.length - 1]));
		maxBounds.addSelectionChangedListener(e->{
			if (getMinZoom() > getMaxZoom()) {
				maxBounds.setSelection(new StructuredSelection(getMinZoom()));
			}
			checkTiles();
		});
		maxBounds.getControl().setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		warnImage = new Label(zoomComp, SWT.WRAP);
		warnImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		warnImage.setVisible(false);
		warnImage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

		
		warnLabel = new Label(zoomComp, SWT.WRAP);
		warnLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		warnLabel.setText("");
		((GridData)warnLabel.getLayoutData()).widthHint = warnLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		loadBm.schedule();
		
		return mapComposite;
	}

	private ReferencedEnvelope getBounds() {
		return (ReferencedEnvelope) txtBounds.getData("RE");
	}
	
	private BasemapDefinition getBasemapSelection() {
		Object o = cmbBasemap.getStructuredSelection().getFirstElement();
		if (o instanceof BasemapDefinition) return ((BasemapDefinition)o);
		return null;
	}
	
	private Integer getMinZoom() {
		return (Integer) minBounds.getStructuredSelection().getFirstElement();
	}
	
	private Integer getMaxZoom() {
		return (Integer) maxBounds.getStructuredSelection().getFirstElement();
	}
	
	private void basemapSelected() {
		try {
			txtBounds.setData("RE", null);
			BasemapDefinition bm = getBasemapSelection();
			if (bm == null) return;
			
			//get basemap bounds
			MapSettings settings = MapSettings.getInstance(bm);
			Map thisMap = ProjectFactory.eINSTANCE.createMap();
			settings.applyTo(thisMap);
			
			ReferencedEnvelope re = thisMap.getBounds(new NullProgressMonitor());
			if (!CRS.equalsIgnoreMetadata(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS)) {
				MathTransform t = CRS.findMathTransform(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS);
				re = new ReferencedEnvelope(JTS.transform(re, t), SmartDB.DATABASE_CRS);
			}
			txtBounds.setData("RE", re);
			txtBounds.setText(re.toString());
			
			int[] zooms = generator.suggestZoomLevels(re);
			minBounds.setSelection(new StructuredSelection(zooms[0]));
			maxBounds.setSelection(new StructuredSelection(zooms[1]));
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
		
		checkTiles();
	}
	
	private void checkTiles() {
		ReferencedEnvelope re = (ReferencedEnvelope)txtBounds.getData("RE");
		if (re == null) {
			clearWarn();
			return;
		}

		int tiles = generator.getTileCount(re, getMinZoom(), getMaxZoom());
		if (tiles < 100_000) {
			warnLabel.setText(MessageFormat.format("{0} tiles", tiles));
			warnImage.setVisible(false);
		}else {
			warnImage.setVisible(true);
			warnLabel.setText(MessageFormat.format("{0} tiles.  Recommend reducing bounds or zoom levels", tiles));
		}
		mapComposite.getParent().layout(true);
	}
	
	private void clearWarn() {
		warnLabel.setText("");
		warnImage.setVisible(false);
		mapComposite.getParent().layout(true);
	}
	
	
	/*
	 * append the conservation area uuid to preference key so each conservation
	 * area will have it's own preferences
	 */
	private String getPreferenceKey(String key) {
		return key + "." + prefKey + "." + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private Job loadBm = new Job("loading basemaps") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> items = new ArrayList<>();
			
			Object selection = null;
			try(Session session = HibernateManager.openSession()){
				items.addAll(
						QueryFactory.buildQuery(session,  BasemapDefinition.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				
				for (Object i : items) {
					((BasemapDefinition)i).getName();
//					if (((BasemapDefinition)i).getUuid().equals(selectionUuid)) {
//						selection = (BasemapDefinition) i;
//					}
				}
			}
			items.add(0, "(NONE)");
			if (selection == null) {
				selection = items.get(0);
			}
			
			
			final Object fselection = selection;
			Display.getDefault().asyncExec(()->{
				cmbBasemap.setInput(items);
				cmbBasemap.setSelection(new StructuredSelection(fselection));
			});
			
			return Status.OK_STATUS;
		} 
		
	};
	
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public PackageContribution packageFiles(IProgressMonitor monitor) throws IOException {
		
		Object[] data = new Object[4];
		
		Display.getDefault().syncExec(()->{
			
			data[0] = getBasemapSelection();
			data[1] = getMinZoom();
			data[2] = getMaxZoom();
			data[3] = getBounds();
			
			
//			//TODO: save preferences to preference store
//			CtIncidentPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(MapPackageContribution.COLLECT_PREF_KEY), (Boolean)selection[0]);
//			if (selection[1] instanceof ConfigurableModel) {
//				CtIncidentPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(MapPackageContribution.CM_PREF_KEY), UuidUtils.uuidToString( ((ConfigurableModel)selection[1]).getUuid()) );
//			}else {
//				CtIncidentPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(MapPackageContribution.CM_PREF_KEY), DATAMODEL );
//			}
		});
//		
		if (data[0] == null) return new PackageContribution();
		
		Path tempDir = Files.createTempDirectory("smartmap"); //$NON-NLS-1$
		Path mapFile = tempDir.resolve(MAPFILE);
		
		PackageContribution updates = new PackageContribution() {
			@Override
			public void cleanUp() throws IOException{
				FileUtils.deleteDirectory(tempDir.toFile());
			}
		};
		try {
			generator.generateMbTiles(mapFile, (ReferencedEnvelope)data[3], (Integer)data[1], (Integer)data[2], (BasemapDefinition)data[0], monitor);
		}catch (Exception ex) {
			throw new IOException(ex);
		}
		
		updates.addFile(mapFile);
		return updates;
		
	}
}
