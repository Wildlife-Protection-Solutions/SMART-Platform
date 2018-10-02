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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
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
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.CtJsonExportUtils;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.ui.SelectBoundsMapDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

public class MapPackageContribution implements IPackageContribution{

	private static final String RE_KEY = "RE"; //$NON-NLS-1$

	//package files
	public static final String MAPFILE = "map.mbtiles"; //$NON-NLS-1$
	
	//preference keys
	private static final String TYPE_PREF_KEY = MapPackageContribution.class.getCanonicalName() + ".type"; //$NON-NLS-1$
	private static final String FOLDER_PREF_KEY = MapPackageContribution.class.getCanonicalName() + ".folder"; //$NON-NLS-1$
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
	
	private Text txtMapDirectory;
	private MbTileGenerator generator;
	private Group mapComposite;
	
	private final static Integer[] validZooms = new Integer[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21};
	
	private List<Control> enableControls = new ArrayList<>();
	
	private enum MapType{SMARTMAP, CUSTOM};
	private MapType mapType = MapType.SMARTMAP;
	
	
	@Override
	public Composite createUi(Composite parent, String prefKey) {
		generator = new MbTileGenerator();
		
		this.prefKey = prefKey;
		
		mapComposite = new Group(parent, SWT.NONE);
		mapComposite.setLayout(new GridLayout());
		mapComposite.setText("Basemap Options");
		
		Composite top = new Composite(mapComposite, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		
		Link linkSmart = new Link(top,  SWT.NONE);
		linkSmart.setText("<a>" + "SMART Basemap" + "</a>");
		
		Link linkFile = new Link(top,  SWT.NONE);
		linkFile.setText("<a>" + "Custom Files" + "</a>");
		
		FontData boldFontData = linkFile.getFont().getFontData()[0];
		boldFontData.setStyle(SWT.BOLD);
		Font boldFont = new Font(top.getShell().getDisplay(), boldFontData);
		linkFile.addListener(SWT.Dispose, e->boldFont.dispose());
		linkSmart.setFont(boldFont);
		
		Font normalFont = linkFile.getFont();
		
		Composite outer = new Composite(mapComposite, SWT.NONE);
		outer.setLayout(new StackLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite mapDirComp = new Composite(outer, SWT.NONE);
		mapDirComp.setLayout(new GridLayout(3, false));
		
		Label l = new Label(mapDirComp, SWT.NONE);
		l.setText("Map Files Directory:");
		
		txtMapDirectory = new Text(mapDirComp, SWT.BORDER);
		txtMapDirectory.setText(""); //$NON-NLS-1$
		txtMapDirectory.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		txtMapDirectory.addListener(SWT.Modify, e->validate());
		
		Button btnBrowse2 = new Button(mapDirComp, SWT.PUSH);
		btnBrowse2.setText("..."); //$NON-NLS-1$
		btnBrowse2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnBrowse2.addListener(SWT.Selection, e->{
			DirectoryDialog fd = new DirectoryDialog(parent.getShell());
			fd.setFilterPath(txtMapDirectory.getText());
			fd.setText("CyberTracker Map Files");
			String dir = fd.open();
			if (dir == null) return;
			txtMapDirectory.setText(dir);
		});
		
		Composite smartComp = new Composite(outer, SWT.NONE);
		smartComp.setLayout(new GridLayout(2, false));
		
		linkFile.addListener(SWT.Selection, e->{
				((StackLayout)outer.getLayout()).topControl = mapDirComp;
				outer.layout();
				linkFile.setFont(boldFont);
				linkSmart.setFont(normalFont);
				top.layout();
				mapType = MapType.CUSTOM;
				
		});
		linkSmart.addListener(SWT.Selection, e->{
			((StackLayout)outer.getLayout()).topControl = smartComp;
			outer.layout();
			linkFile.setFont(normalFont);
			linkSmart.setFont(boldFont);
			top.layout();
			mapType = MapType.SMARTMAP;
		});
		
		l = new Label(smartComp, SWT.NONE);
		l.setText("Basemap:");
		
		cmbBasemap = new ComboViewer(smartComp, SWT.DROP_DOWN | SWT.READ_ONLY);
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
		cmbBasemap.addSelectionChangedListener(e->{
			boolean enabled = getBasemapSelection() != null;
			enableControls.forEach(c->c.setEnabled(enabled));
			basemapSelected();
		});
		
		l = new Label(smartComp, SWT.NONE);
		l.setText("Bounds:");
		enableControls.add(l);
		
		Composite bnds = new Composite(smartComp, SWT.NONE);
		bnds.setLayout(new GridLayout(2, false));
		((GridLayout)bnds.getLayout()).marginWidth = 0;
		((GridLayout)bnds.getLayout()).marginHeight = 0;
		bnds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtBounds = new Text(bnds, SWT.BORDER);
		txtBounds.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtBounds.setText("<map extents>");
		txtBounds.setEditable(false);
		enableControls.add(txtBounds);
		
		Button btnSelectBm = new Button(bnds, SWT.PUSH);
		btnSelectBm.setText("...");
		btnSelectBm.addListener(SWT.Selection, e->{
			BasemapDefinition def = getBasemapSelection();
			if (def == null) return;
			ReferencedEnvelope re = (ReferencedEnvelope) txtBounds.getData(RE_KEY);
			SelectBoundsMapDialog mapDialog = new SelectBoundsMapDialog(smartComp.getShell(), def.getUuid(), re);
			if (mapDialog.open() != Window.OK)  return;
			
			re = mapDialog.getBounds();
			txtBounds.setData(RE_KEY, re);
			txtBounds.setText(re.toString());
			
			updateZoom();
			
		});
		enableControls.add(btnSelectBm);
		
		l = new Label(smartComp, SWT.NONE);
		l.setText("Zoom:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridData)l.getLayoutData()).verticalIndent = 3;
		enableControls.add(l);
		
		Composite zoomComp = new Composite(smartComp, SWT.NONE);
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
		enableControls.add(l);
		
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
		enableControls.add(minBounds.getControl());
		
		l = new Label(left, SWT.NONE);
		l.setText("Max:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		enableControls.add(l);
		
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
		enableControls.add(maxBounds.getControl());
		
		warnImage = new Label(zoomComp, SWT.WRAP);
		warnImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		warnImage.setVisible(false);
		warnImage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		enableControls.add(warnImage);
		
		warnLabel = new Label(zoomComp, SWT.WRAP);
		warnLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		warnLabel.setText("");
		((GridData)warnLabel.getLayoutData()).widthHint = warnLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		enableControls.add(warnLabel);
		loadBm.schedule();
		
		
		String type = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(TYPE_PREF_KEY);
		if (type != null) {
			mapType = MapType.valueOf(type);
			if (mapType == MapType.SMARTMAP) {
				((StackLayout)outer.getLayout()).topControl = smartComp;
				linkSmart.setFont(boldFont);
				linkFile.setFont(normalFont);
				
				ReferencedEnvelope ee = envFromString(CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(BOUNDS_PREF_KEY));
				if (ee != null) {
					txtBounds.setText(ee.toString());
					txtBounds.setData(RE_KEY, ee);
				}
				
				int minZoom = CyberTrackerPlugIn.getDefault().getPreferenceStore().getInt(MINZOOM_PREF_KEY);
				if (minZoom > 0) {
					minBounds.setSelection(new StructuredSelection(minZoom));
				}
				int maxZoom = CyberTrackerPlugIn.getDefault().getPreferenceStore().getInt(MAXZOOM_PREF_KEY);
				if (maxZoom > 0) {
					maxBounds.setSelection(new StructuredSelection(maxZoom));
				}
				checkTiles();
			}else {
				((StackLayout)outer.getLayout()).topControl = mapDirComp;
				linkFile.setFont(boldFont);
				linkSmart.setFont(normalFont);

				String dir = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(FOLDER_PREF_KEY);
				txtMapDirectory.setText(dir);
			}
			outer.layout();
			top.layout();
		}else {
			((StackLayout)outer.getLayout()).topControl = smartComp;
		}

		return mapComposite;
	}

	private ReferencedEnvelope getBounds() {
		return (ReferencedEnvelope) txtBounds.getData(RE_KEY);
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
		
		txtBounds.setData(RE_KEY, null);
		BasemapDefinition bm = getBasemapSelection();
		if (bm == null) return;
			
		//get basemap bounds
		ProgressMonitorDialog dd = new ProgressMonitorDialog(txtBounds.getShell());
		try {
			dd.run(false, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						MapSettings settings = MapSettings.getInstance(bm);
						Map thisMap = ProjectFactory.eINSTANCE.createMap();
						settings.applyTo(thisMap);
						
						ReferencedEnvelope re = thisMap.getBounds(new NullProgressMonitor());
						if (!CRS.equalsIgnoreMetadata(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS)) {
							MathTransform t = CRS.findMathTransform(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS);
							re = new ReferencedEnvelope(JTS.transform(re, t), SmartDB.DATABASE_CRS);
						}
						txtBounds.setData(RE_KEY, re);
						txtBounds.setText(re.toString());
					}catch (Exception ex) {
						throw new InvocationTargetException(ex);
					}
				}
			});
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
		updateZoom();
	}
	
	private void updateZoom() {
		ReferencedEnvelope re = (ReferencedEnvelope) txtBounds.getData(RE_KEY);
		if (re == null) return;
		
		try {
			int[] zooms = generator.suggestZoomLevels(re);
			minBounds.setSelection(new StructuredSelection(zooms[0]));
			maxBounds.setSelection(new StructuredSelection(zooms[1]));
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
		checkTiles();
	}
	
	private void checkTiles() {
		ReferencedEnvelope re = (ReferencedEnvelope)txtBounds.getData(RE_KEY);
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
			String bmUuid = CyberTrackerPlugIn.getDefault().getPreferenceStore().getString(BASEMAP_PREF_KEY);
			if (bmUuid != null) {
				selection = UuidUtils.stringToUuid(bmUuid);
			}
			
			try(Session session = HibernateManager.openSession()){
				items.addAll(
						QueryFactory.buildQuery(session,  BasemapDefinition.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				
				for (Object i : items) {
					((BasemapDefinition)i).getName();
					if (((BasemapDefinition)i).getUuid().equals(selection)) {
						selection = (BasemapDefinition) i;
					}
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
				//todo: we don't want to change min/max zoom settings or envelope
			});
			
			return Status.OK_STATUS;
		} 
		
	};
	
	private String envToString(ReferencedEnvelope env) {
		if (!CRS.equalsIgnoreMetadata(env.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS)) {
			SmartPlugIn.log("Can only save envelopes in lat/long", null);
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(env.getMinX());
		sb.append(",");
		sb.append(env.getMaxX());
		sb.append(",");
		sb.append(env.getMinY());
		sb.append(",");
		sb.append(env.getMaxY());

		return sb.toString();
	}
	
	private ReferencedEnvelope envFromString(String string) {
		if (string.trim().length() == 0) return null;
		
		String[] bits = string.split(",");
		double minx = Double.valueOf(bits[0]);
		double maxx = Double.valueOf(bits[1]);
		double miny = Double.valueOf(bits[2]);
		double maxy = Double.valueOf(bits[3]);
		
		ReferencedEnvelope env = new ReferencedEnvelope(minx, maxx,  miny, maxy, SmartDB.DATABASE_CRS);
		return env;
	}
	
	
	@Override
	public PackageContribution packageFiles(IProgressMonitor monitor) throws IOException {
		Object[] data = new Object[4];
		
		Display.getDefault().syncExec(()->{
			if (mapType == MapType.SMARTMAP) {
				data[0] = getBasemapSelection();
				data[1] = getMinZoom();
				data[2] = getMaxZoom();
				data[3] = getBounds();
			}else if (mapType == MapType.CUSTOM) {
				data[0] = txtMapDirectory.getText();
			}			
		});
		
		CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(TYPE_PREF_KEY, mapType.name());
		
		if (mapType == MapType.SMARTMAP) {
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(BASEMAP_PREF_KEY, UuidUtils.uuidToString( ((BasemapDefinition)data[0]).getUuid()) );
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(BOUNDS_PREF_KEY, envToString(((ReferencedEnvelope)data[3])));
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(MINZOOM_PREF_KEY, (int)data[1]);
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(MAXZOOM_PREF_KEY, (int)data[2]);
		}else {
			CyberTrackerPlugIn.getDefault().getPreferenceStore().setValue(FOLDER_PREF_KEY, (String)data[0]);
		}
		
		if (data[0] == null) return new PackageContribution();
		
		Path tempDir = Files.createTempDirectory("smartmap"); //$NON-NLS-1$
		Path mapFile = tempDir.resolve(MAPFILE);
		
		PackageContribution updates = new PackageContribution() {
			@Override
			public void cleanUp() throws IOException{
				FileUtils.deleteDirectory(tempDir.toFile());
			}
		};
		if (mapType == MapType.SMARTMAP) {
			updates.setProjectMetadata(CtJsonExportUtils.MAP_FILE_DIRECTORY_NAME, MAPFILE);
			try {
				generator.generateMbTiles(mapFile, (ReferencedEnvelope)data[3], (Integer)data[1], (Integer)data[2], (BasemapDefinition)data[0], monitor);
			}catch (Exception ex) {
				throw new IOException(ex);
			}
			if (monitor.isCanceled()) return null;
			updates.addFile(mapFile);
		}else if (mapType == MapType.CUSTOM) {
			Path p = Paths.get((String)data[0]);
			if (Files.exists(p)) {
				updates.addFile(p);
				updates.setProjectMetadata(CtJsonExportUtils.MAP_FILE_DIRECTORY_NAME, p.getFileName().toString());
			}
		}
		
		if (monitor.isCanceled()) return null;
		return updates;
		
	}
}
