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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
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
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.internal.wms.ui.WMSConnectionFactory;
import org.locationtech.udig.catalog.ui.ConnectionFactoryManager;
import org.locationtech.udig.catalog.ui.DataSourceSelectionPage;
import org.locationtech.udig.catalog.ui.UDIGConnectionFactoryDescriptor;
import org.locationtech.udig.catalog.ui.workflow.DataSourceSelectionState;
import org.locationtech.udig.catalog.ui.workflow.ResourceSelectionState;
import org.locationtech.udig.catalog.ui.workflow.State;
import org.locationtech.udig.catalog.ui.workflow.Workflow;
import org.locationtech.udig.catalog.ui.workflow.WorkflowWizard;
import org.locationtech.udig.catalog.ui.workflow.WorkflowWizardPageProvider;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.ui.internal.wizard.MapImport;
import org.locationtech.udig.project.ui.internal.wizard.MapImportWizard;
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
import org.wcs.smart.cybertracker.model.PackageMapLayer;
import org.wcs.smart.cybertracker.model.PackageMapLayerManager;
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
	
	private TableViewer lstOther;
	
	private final static Integer[] validZooms = new Integer[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21};
	
	private List<Control> enableControls = new ArrayList<>();
	
	private Composite stackComposite;
	private Composite smartComp, mapDirComp;
	private Font boldFont, normalFont; 

	private Button linkFile, linkSmart;
	
	private List<Path> mapfiles;
	private List<Path> deletedfiles;

	private AbstractCtPackage ctpackage; 
	private List<PackageMapLayer> layers = null;
	private Listener onModified;
	private Runnable onInitilized;
	
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
		
		pp.setMapLayers(layers);
		lstMapFiles.refresh();
	}
	
	@Override
	public String isValid() {
		return null;
	}
	

	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified, Runnable onInitilized) {
		this.ctpackage = (AbstractCtPackage) ctpackage;
		this.onModified = onModified;
		this.onInitilized = onInitilized;
		
		try {
			this.layers = new ArrayList<>(this.ctpackage.getMapLayers());
		}catch (ParseException ex) {
			CyberTrackerPlugIn.displayError(Messages.MapPackageUiContribution_ErrorTitle, Messages.MapPackageUiContribution_MapLayerParseError, ex);
			this.layers = new ArrayList<>();
		}
				
		generator = new MbTileGenerator();
		
		Composite g = new Composite(parent, SWT.NONE);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)g.getLayout()).marginWidth = 0;
		((GridLayout)g.getLayout()).marginHeight = 0;
		
		SmartUiUtils.createHeaderLabel(g, Messages.MapPackageContribution_BasemapOpsTitle);
		
		mapComposite = new Composite(g, SWT.NONE);
		mapComposite.setLayout(new GridLayout());
		mapComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

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
		stackComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		mapDirComp = new Composite(stackComposite, SWT.NONE);
		mapDirComp.setLayout(new GridLayout(3, false));
		((GridLayout)mapDirComp.getLayout()).marginWidth = 0;
		
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
			if (!isInit) onModified.handleEvent(null);
			
		});
		
		ToolItem tiClear = new ToolItem(bt, SWT.PUSH);
		tiClear.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiClear.setToolTipText(Messages.MapPackageUiContribution_removetooltip);
		tiClear.addListener(SWT.Selection, e->{
			for (Iterator<?> iterator = lstMapFiles.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object item = (Object) iterator.next();
				if (mapfiles.remove(item)) deletedfiles.add((Path)item);
			}
			lstMapFiles.refresh();
			if (!isInit) onModified.handleEvent(null);
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
				onModified.handleEvent(null);
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
			SelectBoundsMapDialog mapDialog = new SelectBoundsMapDialog(smartComp.getShell(), def.getUuid(), re, 1.0);
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
			if (!isInit) onModified.handleEvent(null);
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
		
		//spacer
		new Label(smartComp, SWT.NONE);
		
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
			if (!isInit) onModified.handleEvent(null);
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
			if (!isInit) onModified.handleEvent(null);
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
		
		createOptionLayers(g);
		
		return g;
	}

	private void createOptionLayers(Composite parent) {
		
		SmartUiUtils.createHeaderLabel(parent, Messages.MapPackageUiContribution_OtherSection);
		
		Composite otherComposite = new Composite(parent, SWT.NONE);
		otherComposite.setLayout(new GridLayout(2, false));
		otherComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)otherComposite.getLayout()).marginWidth = 0;
		((GridLayout)otherComposite.getLayout()).marginHeight = 0;
		
		lstOther = new TableViewer(otherComposite, SWT.BORDER | SWT.MULTI);
		lstOther.setContentProvider(ArrayContentProvider.getInstance());
		lstOther.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				PackageMapLayer layer = (PackageMapLayer)element;
				StringBuilder sb = new StringBuilder(layer.getType());
				sb.append(" - "); //$NON-NLS-1$
				for (Entry<String,String> part : layer.getProperties().entrySet()) {
					sb.append("  "); //$NON-NLS-1$
					sb.append(part.getKey());
					sb.append(": "); //$NON-NLS-1$
					sb.append(part.getValue());
				}
				return sb.toString();
			}
		});
		lstOther.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)lstOther.getControl().getLayoutData()).widthHint = 100;
		lstOther.setInput(this.layers);
		
		
		ToolBar tbButtons = new ToolBar(otherComposite, SWT.VERTICAL);
		tbButtons.setLayout(new GridLayout());
		tbButtons.setLayoutData(new GridData(SWT.CENTER, SWT.TOP,false, false));
		((GridLayout)tbButtons.getLayout()).marginWidth = 0;
		((GridLayout)tbButtons.getLayout()).marginHeight = 0;
		
		ToolItem btnAdd = new ToolItem(tbButtons, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.addListener(SWT.Selection, e->addOtherLayer());
		
		ToolItem btnDelete = new ToolItem(tbButtons, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.addListener(SWT.Selection, e->deleteOtherLayer());
		btnDelete.setEnabled(false);

		ToolItem btnMoveUp = new ToolItem(tbButtons, SWT.PUSH);
		btnMoveUp.setImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_UP));
		btnMoveUp.setToolTipText(Messages.MapPackageUiContribution_moveuptooltip);
		btnMoveUp.setEnabled(false);
		btnMoveUp.addListener(SWT.Selection, e->moveLayer(-1));
		
		ToolItem btnMoveDown = new ToolItem(tbButtons, SWT.PUSH);
		btnMoveDown.setImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_DOWN));
		btnMoveDown.setToolTipText(Messages.MapPackageUiContribution_movedowntooltip);
		btnMoveDown.setEnabled(false);
		btnMoveDown.addListener(SWT.Selection, e->moveLayer(1));
		
		
		
		Menu mnu = new Menu(lstOther.getControl());
		MenuItem miadd = new MenuItem(mnu, SWT.PUSH);
		miadd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miadd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miadd.addListener(SWT.Selection, e->addOtherLayer());
		MenuItem midelete = new MenuItem(mnu, SWT.PUSH);
		midelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		midelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		midelete.addListener(SWT.Selection, e->deleteOtherLayer());
		midelete.setEnabled(false);
		lstOther.getControl().setMenu(mnu);
		
		lstOther.addSelectionChangedListener(e->{
			btnDelete.setEnabled(!lstOther.getSelection().isEmpty());
			midelete.setEnabled(!lstOther.getSelection().isEmpty());
			btnMoveUp.setEnabled(!lstOther.getSelection().isEmpty());
			btnMoveDown.setEnabled(!lstOther.getSelection().isEmpty());
		});
		
	}
	
	private void moveLayer(int amount) {
		Object item = lstOther.getStructuredSelection().getFirstElement();
		int index = layers.indexOf(item);
		index += amount;
		
		layers.remove(item);
		
		if (index < 0) index = 0;
		if (index > layers.size()) index = layers.size();
		
		layers.add(index, (PackageMapLayer)item);
		
		lstOther.refresh();
		onModified.handleEvent(null);
	}
	
	
	private void addOtherLayer() {
		
		//reuse map import dialog
		MapImport mi = new MapImport() {
			private DataSourceSelectionState dsState;

			@Override
			protected Workflow createWorkflow() {
				dsState = new DataSourceSelectionState(true) {
					// list options to wms only
					@Override
					public List<UDIGConnectionFactoryDescriptor> getShortlist() {
						List<UDIGConnectionFactoryDescriptor> shortlist = new ArrayList<>();
						Collection<UDIGConnectionFactoryDescriptor> descriptors = ConnectionFactoryManager.instance()
								.getConnectionFactoryDescriptors();
						for (UDIGConnectionFactoryDescriptor item : descriptors) {
							if (item.getConnectionFactory().getClass() == WMSConnectionFactory.class) {
								shortlist.add(item);
							}
						}
						return shortlist;
					}
				};

				ResourceSelectionState rsState = new ResourceSelectionState();

				Workflow workflow = new Workflow(new State[] { dsState, rsState });
				return workflow;
			}

			protected Map<Class<? extends State>, WorkflowWizardPageProvider> createPageMapping() {
				Map<Class<? extends State>, WorkflowWizardPageProvider> map = super.createPageMapping();
				addToMap(map, dsState.getClass(), DataSourceSelectionPage.class);
				return map;
			}

			@Override
			protected WorkflowWizard createWorkflowWizard(Workflow workflow,
					java.util.Map<Class<? extends State>, WorkflowWizardPageProvider> map) {

				return new MapImportWizard(workflow, map) {
					@Override
					protected boolean performFinish(IProgressMonitor monitor) {

						List<IGeoResource> resourceList = new ArrayList<IGeoResource>();
						ResourceSelectionState state = getWorkflow().getState(ResourceSelectionState.class);
						if (state != null) {
							java.util.Map<IGeoResource, IService> resourceMap = state.getResources();
							if (resourceMap != null && !resourceMap.isEmpty()) {
								resourceList.addAll(resourceMap.keySet());
							}
						}

						if (resourceList.isEmpty())
							return false;

						for (IGeoResource r : resourceList) {
							try {
								PackageMapLayer newlayer = PackageMapLayerManager.INSTANCE.toMapLayer(r);
								layers.add(newlayer);
							} catch (Exception ex) {
								CyberTrackerPlugIn.displayError(Messages.MapPackageUiContribution_ErrorTitle,
										Messages.MapPackageUiContribution_AddError + ex.getMessage(), ex);
							}
						}
						Display.getDefault().asyncExec(() -> {
							lstOther.refresh();
							onModified.handleEvent(null);

						});
						return true;
					}
				};
			}

		};
		
		mi.getDialog().open();
	}
	
	private void deleteOtherLayer() {
		for (Iterator<?> iterator = lstOther.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			layers.remove(item);
		}
		lstOther.refresh();
		if (!isInit) onModified.handleEvent(null);

		
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
						org.locationtech.udig.project.internal.Map thisMap = ProjectFactory.eINSTANCE.createMap();
						settings.applyTo(thisMap);
						
						ReferencedEnvelope re = thisMap.getBounds(new NullProgressMonitor());
						if (!CRS.equalsIgnoreMetadata(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS)) {
							MathTransform t = CRS.findMathTransform(re.getCoordinateReferenceSystem(), SmartDB.DATABASE_CRS);
							re = new ReferencedEnvelope(JTS.transform(re, t), SmartDB.DATABASE_CRS);
						}
						txtBounds.setData(RE_KEY, re);
						txtBounds.setText(re.toString());
					}catch (Exception ex) {
						txtBounds.setText(Messages.MapPackageUiContribution_BoundsComputationError);
						txtBounds.setData(RE_KEY, new ReferencedEnvelope( -180, 180, -85, 85, SmartDB.DATABASE_CRS));
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
	
	@Override
	public boolean isTab() { 
		return true; 
	}

	@Override
	public String getTabName() {
		return Messages.MapPackageUiContribution_BasemapSection;
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
				onInitilized.run();
			});
			
			return Status.OK_STATUS;
		}	
	};
	
}
