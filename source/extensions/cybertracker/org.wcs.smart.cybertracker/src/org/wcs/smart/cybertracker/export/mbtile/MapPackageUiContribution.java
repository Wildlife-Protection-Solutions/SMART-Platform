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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.opengis.referencing.operation.MathTransform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.AbstractCtPackage.BaseMapKeys;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.internal.settings.MapSettings;
import org.wcs.smart.ui.SelectBoundsMapDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * Mappackage ui contribution
 * @author Emily
 *
 */
public class MapPackageUiContribution implements IPackageUiContribution{

	public static final String RE_KEY = "ReferencedEnvelope"; //$NON-NLS-1$
	
	private ComboViewer cmbBasemap;
	private Text txtBounds;
	private ComboViewer minBounds, maxBounds;
	private Label warnLabel;
	private Label warnImage;
	
	private ListViewer lstMapFiles;
	private MbTileGenerator generator;
	private Composite mapComposite;
	
	private final static Integer[] validZooms = new Integer[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21};
	
	private List<Control> enableControls = new ArrayList<>();
	
	private Composite stackComposite;
	private Composite smartComp, mapDirComp;
	private Font boldFont, normalFont; 

	private Button linkFile, linkSmart;
	
	private List<Path> mapfiles;
	private List<Path> deletedfiles;

	private AbstractCtPackage ctpackage; 
	private boolean isInit = false;
	
	@Override
	public void updatePackage(ICtPackage ctpackage) throws Exception{
		if (!(ctpackage instanceof AbstractCtPackage)) return;
		AbstractCtPackage pp = (AbstractCtPackage)ctpackage;

		BasemapDefinition def = getBasemapSelection();
		Integer minZoom = getMinZoom();
		Integer maxZoom = getMaxZoom();
		ReferencedEnvelope evn = getBounds();
		
		if (linkSmart.getSelection()) {
			if (def == null) {
				pp.clearBasemap();
			}else {
				try {
					pp.setBasemap(def.getUuid(), evn, SmartDB.DATABASE_CRS, minZoom, maxZoom);
				} catch (Exception e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
				}
			}
			//delete all files in basemap dir
			Path dir = ICyberTrackerConstants.getBasemapFileStore(ctpackage);
			deletedfiles.clear();
			mapfiles.clear();
			if (Files.exists(dir)) {
				//delete all files
				try(Stream<Path> files = Files.list(dir)){
					files.forEach(f->{
						try {
							Files.delete(f);
						} catch (IOException ex) {
							CyberTrackerPlugIn.displayError(Messages.MapPackageUiContribution_ErrorTitle, MessageFormat.format(Messages.MapPackageUiContribution_DeleteFail + "\n\n" + ex.getMessage(), f.toString()), ex); //$NON-NLS-1$
						}	
					});
				}catch (Exception ex) {
					CyberTrackerPlugIn.displayError(Messages.MapPackageUiContribution_ErrorTitle, MessageFormat.format(Messages.MapPackageUiContribution_DeleteDirFail + "\n\n" + ex.getMessage(), dir.toString()), ex); //$NON-NLS-1$

				}
			}
		}else {
			if (mapfiles.isEmpty()) {
				//no basemap
				pp.clearBasemap();
			}else {
				//import all files into necessary directory
				Path dir = ICyberTrackerConstants.getBasemapFileStore(ctpackage);
				try {
					if (!mapfiles.isEmpty() && !Files.exists(dir))  Files.createDirectories(dir);
				}catch (IOException ex) {
					throw new Exception(Messages.MapPackageUiContribution_CreateDirFail + ex.getMessage(),ex);
				}
					
				for (Path p : deletedfiles) {
					//if not in filestore then do not delete
					if (!p.getParent().equals(dir)) continue;
					try {
						Files.delete(p);
					} catch (IOException ex) {
						CyberTrackerPlugIn.displayError(Messages.MapPackageUiContribution_ErrorTitle, MessageFormat.format(Messages.MapPackageUiContribution_DeleteFileError + "\n\n" + ex.getMessage(), p.toString()), ex); //$NON-NLS-1$
					}
				}
				
				//copy in all files
				List<Path>  newfiles = new ArrayList<>();
				for (Path p : mapfiles) {
					if (p.getParent().equals(dir)) {
						newfiles.add(p);
						continue;	//this file is already in the file store
					}
					Path target = dir.resolve(p.getFileName());
					if (Files.exists(target)) {
						//fail here, otherwise the file will be overwritten  
						//we do have a check below to try the prevent the users from doing this
						throw new Exception(MessageFormat.format(Messages.MapPackageUiContribution_FileExists, target.getFileName().toString()));
					}
					try {
						Files.copy(p, target);
						newfiles.add(target);
					} catch (IOException ex) {
						CyberTrackerPlugIn.displayError(Messages.MapPackageUiContribution_ErrorTitle, MessageFormat.format(Messages.MapPackageUiContribution_ImportError + "\n\n" + ex.getMessage(), p.toString()), ex); //$NON-NLS-1$
					}
					
				}
				mapfiles.clear();
				mapfiles.addAll(newfiles);
				
				//set package
				pp.setBasemapToFiles();
			}
		}
		lstMapFiles.refresh();
	}
	
	@Override
	public String isValid() {
		return null;
	}
	

	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onValidate) {
		this.ctpackage = (AbstractCtPackage) ctpackage;
		
		generator = new MbTileGenerator();
		
		Composite g = new Composite(parent, SWT.NONE);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite header = new Composite(g, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label headerLabel = new Label(header, SWT.NONE);
		headerLabel.setText(Messages.MapPackageContribution_BasemapOpsTitle);
		WidgetElement.setCSSClass(header, SmartUiUtils.HEADER_CLASS);
		
		mapComposite = new Composite(g, SWT.NONE);
		mapComposite.setLayout(new GridLayout());
		
		Composite top = new Composite(mapComposite, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		
		linkSmart = new Button(top,  SWT.RADIO);
		linkSmart.setText(Messages.MapPackageContribution_BasemapOp); 
		linkSmart.setSelection(true);
		
		linkFile = new Button(top,  SWT.RADIO);
		linkFile.setText( Messages.MapPackageContribution_FilesOp);
		
		FontData boldFontData = linkFile.getFont().getFontData()[0];
		boldFontData.setStyle(SWT.BOLD);
		boldFont = new Font(top.getShell().getDisplay(), boldFontData);
		linkFile.addListener(SWT.Dispose, e->boldFont.dispose());
		linkSmart.setFont(boldFont);
		
		normalFont = linkFile.getFont();
		
		stackComposite = new Composite(mapComposite, SWT.NONE);
		stackComposite.setLayout(new StackLayout());
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		mapDirComp = new Composite(stackComposite, SWT.NONE);
		mapDirComp.setLayout(new GridLayout(3, false));
		((GridLayout)mapDirComp.getLayout()).marginWidth = 0;
		((GridLayout)mapDirComp.getLayout()).marginHeight = 0;
		
		Label l = new Label(mapDirComp, SWT.NONE);
		l.setText(Messages.MapPackageUiContribution_MapFilesLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		lstMapFiles = new ListViewer(mapDirComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		lstMapFiles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstMapFiles.setContentProvider(ArrayContentProvider.getInstance());
		lstMapFiles.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((Path)element).getFileName().toString();
			}
		});
		

		ToolBar bt = new ToolBar(mapDirComp, SWT.FLAT | SWT.VERTICAL);
		bt.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		ToolItem tiAdd = new ToolItem(bt, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.setToolTipText(Messages.MapPackageUiContribution_addtooltip);
		tiAdd.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(mapDirComp.getShell(), SWT.OPEN | SWT.MULTI);
			fd.setText(Messages.MapPackageUiContribution_FileDialogTitle);
			fd.setText(Messages.MapPackageUiContribution_FileDialogMessage);
			if (fd.open() == null) return;
			
			Path root = Paths.get(fd.getFilterPath());
			for (String s : fd.getFileNames()) {
				Path f = root.resolve(s);
				if (!Files.exists(f)) continue;
				if (mapfiles.contains(f)) continue; //same file
				
				//check duplicate filenames
				boolean isdup = false;
				for(Path p : mapfiles) {
					if (p.getFileName().toString().equalsIgnoreCase(f.getFileName().toString())) {
						isdup = true; 
						break;
					}
				}
				if (isdup) {
					MessageDialog.openWarning(bt.getShell(), Messages.MapPackageUiContribution_DuplicateTitle, MessageFormat.format(Messages.MapPackageUiContribution_DuplciateMessage, f.getFileName().toString()));
				}else {
					mapfiles.add(f);
				}
			}
			lstMapFiles.refresh();
			if (!isInit) onValidate.handleEvent(null);
			
		});
		
		ToolItem tiClear = new ToolItem(bt, SWT.PUSH);
		tiClear.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiClear.setToolTipText(Messages.MapPackageUiContribution_removetooltip);
		tiClear.addListener(SWT.Selection, e->{
			for (Iterator<Object> iterator = lstMapFiles.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object item = (Object) iterator.next();
				if (mapfiles.remove(item)) deletedfiles.add((Path)item);
			}
			lstMapFiles.refresh();
			if (!isInit) onValidate.handleEvent(null);
		});
		
		smartComp = new Composite(stackComposite, SWT.NONE);
		smartComp.setLayout(new GridLayout(2, false));
		
		linkFile.addListener(SWT.Selection, e->{
				((StackLayout)stackComposite.getLayout()).topControl = mapDirComp;
				stackComposite.layout();
				linkFile.setFont(boldFont);
				linkSmart.setFont(normalFont);
				top.layout();
				
		});
		linkSmart.addListener(SWT.Selection, e->{
			((StackLayout)stackComposite.getLayout()).topControl = smartComp;
			stackComposite.layout();
			linkFile.setFont(normalFont);
			linkSmart.setFont(boldFont);
			top.layout();
		});
		
		l = new Label(smartComp, SWT.NONE);
		l.setText(Messages.MapPackageContribution_BasemapLabel);
		
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
			if (!isInit) {
				basemapSelected();
				onValidate.handleEvent(null);
			}
		});
		
		l = new Label(smartComp, SWT.NONE);
		l.setText(Messages.MapPackageContribution_BoundsLabel);
		enableControls.add(l);
		
		Composite bnds = new Composite(smartComp, SWT.NONE);
		bnds.setLayout(new GridLayout(2, false));
		((GridLayout)bnds.getLayout()).marginWidth = 0;
		((GridLayout)bnds.getLayout()).marginHeight = 0;
		bnds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtBounds = new Text(bnds, SWT.BORDER);
		txtBounds.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtBounds.setText(Messages.MapPackageContribution_MapExtents);
		txtBounds.setEditable(false);
		enableControls.add(txtBounds);
		
		Button btnSelectBm = new Button(bnds, SWT.PUSH);
		btnSelectBm.setText("..."); //$NON-NLS-1$
		btnSelectBm.addListener(SWT.Selection, e->{
			BasemapDefinition def = getBasemapSelection();
			if (def == null) return;
			ReferencedEnvelope re = (ReferencedEnvelope) txtBounds.getData(RE_KEY);
			SelectBoundsMapDialog mapDialog = new SelectBoundsMapDialog(smartComp.getShell(), def.getUuid(), re);
			if (mapDialog.open() != Window.OK)  return;
			
			re = mapDialog.getBounds();
			if (!CRS.equalsIgnoreMetadata(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS)) {
				try {
					MathTransform t = CRS.findMathTransform(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS);
					re = new ReferencedEnvelope(JTS.transform(re, t), SmartDB.DATABASE_CRS);
				}catch (Exception ex) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
					return;
				}
			}
			
			txtBounds.setData(RE_KEY, re);
			txtBounds.setText(re.toString());
			
			updateZoom();
			if (!isInit) onValidate.handleEvent(null);
		});
		enableControls.add(btnSelectBm);
		
		l = new Label(smartComp, SWT.NONE);
		l.setText(Messages.MapPackageContribution_ZoomLabel);
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
		l.setText(Messages.MapPackageContribution_MinLabel);
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
			if (!isInit) onValidate.handleEvent(null);
		});
		minBounds.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		enableControls.add(minBounds.getControl());
		
		l = new Label(left, SWT.NONE);
		l.setText(Messages.MapPackageContribution_MaxLabel);
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
			if (!isInit) onValidate.handleEvent(null);
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
		warnLabel.setText(""); //$NON-NLS-1$
		((GridData)warnLabel.getLayoutData()).widthHint = warnLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		enableControls.add(warnLabel);
		
		((StackLayout)stackComposite.getLayout()).topControl = smartComp;
		
		loadBm.schedule();
		
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
			warnLabel.setText(MessageFormat.format(Messages.MapPackageContribution_TilesCnt, tiles));
			warnImage.setVisible(false);
		}else {
			warnImage.setVisible(true);
			warnLabel.setText(MessageFormat.format(Messages.MapPackageContribution_TilesWarnCnt, tiles));
		}
		mapComposite.getParent().layout(true);
	}
	
	private void clearWarn() {
		warnLabel.setText(""); //$NON-NLS-1$
		warnImage.setVisible(false);
		mapComposite.getParent().layout(true);
	}
	
	
	private void initialize() {
		isInit = true;
		try {
			mapfiles = new ArrayList<>();
			deletedfiles = new ArrayList<>();
			lstMapFiles.setInput(mapfiles);
			cmbBasemap.setSelection(new StructuredSelection(Messages.MapPackageContribution_NoBasemap));
			if (ctpackage != null && ctpackage.getBasemapDef() != null && !ctpackage.getBasemapDef().isBlank()) {
				
				JSONObject obj = null;
				try {
					obj = (JSONObject) (new JSONParser()).parse(ctpackage.getBasemapDef());
				} catch (ParseException e) {
					CyberTrackerPlugIn.log(e.getMessage(), e);
					return;
				}
						
				if (obj.containsKey(BaseMapKeys.BM.jsonkey)) {
				
					((StackLayout)stackComposite.getLayout()).topControl = smartComp;
					linkSmart.setFont(boldFont);
					linkFile.setFont(normalFont);
					linkSmart.setSelection(true);
					linkFile.setSelection(false);
					
					String bmUuid = (String) obj.get(BaseMapKeys.BM.jsonkey);
					if (bmUuid != null && !bmUuid.trim().isEmpty()) {
						if (cmbBasemap.getInput() instanceof List) {
							UUID uuid = UuidUtils.stringToUuid(bmUuid);
							BasemapDefinition temp = new BasemapDefinition();
							temp.setUuid(uuid);
							cmbBasemap.setSelection(new StructuredSelection(temp));
						}
					}
					
					Double xmin = (Double) obj.get(BaseMapKeys.XMIN.jsonkey);
					Double xmax = (Double) obj.get(BaseMapKeys.XMAX.jsonkey);
					Double ymin = (Double) obj.get(BaseMapKeys.YMIN.jsonkey);
					Double ymax = (Double) obj.get(BaseMapKeys.YMAX.jsonkey);
					
					if (xmin != null && xmax != null && ymin != null && ymax != null) {
						ReferencedEnvelope ee = new ReferencedEnvelope(xmin, xmax, ymin, ymax, SmartDB.DATABASE_CRS);
						if (ee != null) {
							txtBounds.setText(ee.toString());
							txtBounds.setData(RE_KEY, ee);
						}
					}
					
					Long minZoom = (Long) obj.get(BaseMapKeys.MINZOOM.jsonkey);
					if (minZoom != null && minZoom > 0) {
						minBounds.setSelection(new StructuredSelection(minZoom.intValue()));
					}
					
					Long maxZoom = (Long) obj.get(BaseMapKeys.MAXZOOM.jsonkey);
					if (maxZoom != null && maxZoom > 0) {
						maxBounds.setSelection(new StructuredSelection(maxZoom.intValue()));
					}
					checkTiles();
					
					//delete all previously imported map files
				}else {
					((StackLayout)stackComposite.getLayout()).topControl = mapDirComp;
					linkFile.setFont(boldFont);
					linkSmart.setFont(normalFont);
					linkSmart.setSelection(false);
					linkFile.setSelection(true);
	
					String dir = (String)obj.get(BaseMapKeys.FILE.jsonkey);
					//list all files
					if (dir != null) {
						Path mapdir = ICyberTrackerConstants.getBasemapFileStore(ctpackage);
						if (Files.exists(mapdir)) {
							try(Stream<Path> items = Files.list(mapdir)){
								items.forEach(i->mapfiles.add(i));
							}catch (IOException ex) {
								CyberTrackerPlugIn.displayError(Messages.MapPackageUiContribution_ErrorTitle, Messages.MapPackageUiContribution_DirReadError + ex.getMessage(), ex);
							}
						}
					}
				}
				lstMapFiles.refresh();
				stackComposite.layout();
				linkSmart.getParent().layout();
			}
		}finally {
			isInit = false;
		}
	}
	
	
	
	private Job loadBm = new Job(Messages.MapPackageContribution_loadingJobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> items = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				items.addAll(
						QueryFactory.buildQuery(session,  BasemapDefinition.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
			}
			items.add(0, Messages.MapPackageContribution_NoBasemap);
			Display.getDefault().asyncExec(()->{
				cmbBasemap.setInput(items);
				initialize();
			});
			
			return Status.OK_STATUS;
		}	
	};
	
}
