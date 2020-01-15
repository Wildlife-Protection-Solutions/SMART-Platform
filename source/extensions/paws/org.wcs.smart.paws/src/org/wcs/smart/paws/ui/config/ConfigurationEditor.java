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
package org.wcs.smart.paws.ui.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.geotools.data.FeatureStore;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.engine.PawsDataEngine;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsParameter.FixedParameter;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.ui.ErrorText;
import org.wcs.smart.paws.ui.HeaderComposite;
import org.wcs.smart.paws.ui.HidePartsPartListener;
import org.wcs.smart.paws.ui.NewPawsRunHandler;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * PAWS Configuration editor
 * 
 * @author Emily
 *
 */
public class ConfigurationEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.paws.configuration.editor"; //$NON-NLS-1$
	
	private static final String RE_DATA_KEY = "RE";
	private static final String CUSTOM_FILE = "Custom File...";

	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	
	private FormToolkit toolkit;

	private HeaderComposite compHeader;
	private ComboViewer cmbBound,cmbTrainingRes, cmbClassifier; // cmbCrs, cmbRoad, cmbRiver, cmbContour,
	private ListViewer lstOther;
//	private ErrorText txtBounds;
	private ErrorText txtGridSize;
	
	private ClassificationComposite classComposite;

	private boolean isDirty = false;
			
	@Override
	public void dispose() {
		super.dispose();
		toolkit.dispose();
		
		IEventBroker event = parentContext.get(IEventBroker.class);
		if (handlers != null){
			handlers.forEach((h)->event.unsubscribe(h));
		}
		handlers = null;
	}
	
	private Path generateUniqueName(Path path, PawsConfiguration pw) {
		String fname = path.getFileName().toString();
		
		String namepart = SharedUtils.getFilenameWithoutExtension(fname);
		String extpart = SharedUtils.getFilenameExtension(fname);
		
		//ensure name is unique
		Path target = PawsManager.INSTANCE.getDirectory(pw).resolve(  fname );
		int cnt = 1;
		while(Files.exists(target)) {
			fname = namepart + "_" + cnt + "." + extpart;
			target = PawsManager.INSTANCE.getDirectory(pw).resolve(  fname );
			cnt++;
		}
		return target;
		
	}
	private void save(PawsConfiguration pw){
		String valid = validate();
		if (valid != null) {
			MessageDialog.openError(getSite().getShell(), "Error", "Unable to save changes until all error are resovled." + "\n\n" + valid);
			return;
		}

		List<Path[]> filesToCopy = new ArrayList<>();
		List<String> fileNames = new ArrayList<>();
		List<String> deletedFiles = new ArrayList<>();
		
		boolean isNew = false;
		
		
		try(Session s = HibernateManager.openSession(new CopyFilesInterceptor(filesToCopy, fileNames, deletedFiles))){
			
			s.beginTransaction();
			try {
				if (pw.getUuid() == null) {
					String name = pw.getName();
					pw = new PawsConfiguration();
					pw.setConservationArea(SmartDB.getCurrentConservationArea());
					if (name != null){
						pw.setName(name);
						compHeader.setText(name);
					}else{
						pw.setName(compHeader.getText());
					}
					isNew = true;
				}else {
					pw = s.get(PawsConfiguration.class, pw.getUuid());
					pw.setName(compHeader.getText());
				}
				
				s.saveOrUpdate(pw);
	
				fileNames.add(PawsManager.INSTANCE.getDirectory(pw).toString());
				
				//update boundary
				PawsParameter pp = getOrCreateParameter(pw, PawsParameter.FixedParameter.LYR_BOUNDARY);
				Object x = cmbBound.getStructuredSelection().getFirstElement();
				if (x instanceof Area.AreaType) {
					pp.setValue(PawsParameter.AREA_PREFIX + ((Area.AreaType)x).name());
				}else if (x instanceof Path ) {
					Path path = (Path)x;
					Path target = generateUniqueName(path, pw);
					pp.setValue( PawsParameter.FILE_PREFIX + target.getFileName().toString() );
					fileNames.add(SharedUtils.getFilenameWithoutExtension(target.getFileName().toString()));
					filesToCopy.add(new Path[] {path, (Path)target});
						
				}else if (x instanceof PawsParameter) {
					String vname = ((PawsParameter)x).getValue();
					if (vname.startsWith(PawsParameter.FILE_PREFIX)) {
						fileNames.add( SharedUtils.getFilenameWithoutExtension( vname.substring(PawsParameter.FILE_PREFIX.length())  ) );
					}
				}else {
					pp.setValue(null);
				}
				
				//other files
				List<PawsParameter> tokeep = new ArrayList<>();
				for (Object item : (Collection<Object>)lstOther.getInput()) {
					if (item instanceof PawsParameter) {
						tokeep.add((PawsParameter)item);
						String vname = ((PawsParameter)item).getValue();
						if (vname.startsWith(PawsParameter.FILE_PREFIX)) {
							fileNames.add( SharedUtils.getFilenameWithoutExtension( vname.substring(PawsParameter.FILE_PREFIX.length())  ) );
						}
					}else if (item instanceof Area.AreaType) {
						
						PawsParameter newpp = new PawsParameter();
						newpp.setConfiguration(pw);
						newpp.setKey(PawsParameter.FixedParameter.LYR_OTHER.name());
						pw.getParameters().add(newpp);
						pp.setValue(PawsParameter.AREA_PREFIX + ((Area.AreaType)item).name());
						tokeep.add(newpp);
					}else if (item instanceof Path) {
						//import file
						Path path = (Path)item;
						
						Path target = generateUniqueName(path, pw);
						fileNames.add(SharedUtils.getFilenameWithoutExtension(target.getFileName().toString()));
						filesToCopy.add(new Path[] {path, (Path)target});
						
						PawsParameter newpp = new PawsParameter();
						newpp.setConfiguration(pw);
						newpp.setKey(PawsParameter.FixedParameter.LYR_OTHER.name());
						pw.getParameters().add(newpp);
						newpp.setValue(PawsParameter.FILE_PREFIX + target.getFileName().toString());
						tokeep.add(newpp);
					}
				}
				List<PawsParameter> toremove = new ArrayList<>();
				for (PawsParameter pc : pw.getParameters() ) {
					if (pc.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name()) && !tokeep.contains(pc)) toremove.add(pc);
				}
				pw.getParameters().removeAll(toremove);
				for (PawsParameter pc: toremove) {
					if (pc.getValue().startsWith(PawsParameter.FILE_PREFIX)) {
						deletedFiles.add(pc.getValue().substring(PawsParameter.FILE_PREFIX.length()));
					}
				}
				
				pp = getOrCreateParameter(pw, PawsParameter.FixedParameter.GRID_SIZE);
				pp.setValue(txtGridSize.getText());
				
				
//				pp = getOrCreateParameter(pw, PawsParameter.FixedParameter.GRID_CRS);
//				Object crs = cmbCrs.getStructuredSelection().getFirstElement();
//				Projection prj = (Projection)crs;
//				prj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(prj.getDefinition()));
//				if (prj.getUuid() != null){
//					pp.setValue( UuidUtils.uuidToString(prj.getUuid()) + ":" + prj.getParsedCoordinateReferenceSystem().toWKT() );
//				}else{
//					pp.setValue( ":" + prj.getParsedCoordinateReferenceSystem().toWKT() );
//				}
				
				pp = getOrCreateParameter(pw,  PawsParameter.FixedParameter.TRAINING_RES);
				Object tr = cmbTrainingRes.getStructuredSelection().getFirstElement();
				pp.setValue( String.valueOf((Integer)tr) );
				
				pp = getOrCreateParameter(pw,  PawsParameter.FixedParameter.CLASSIFIER_MODEL);
				tr = cmbClassifier.getStructuredSelection().getFirstElement();
				pp.setValue(  ((PawsParameter.ClassifierModel)tr).name() );
				
				classComposite.doSave(pw, s);
				
				s.getTransaction().commit();
			}catch (Exception ex) {
				try {
					s.getTransaction().rollback();
				}catch (Exception e2) {
					PawsPlugIn.log(e2.getMessage(), e2);
				}
				PawsPlugIn.displayLog("Unable to save changes to configuration." + "\n\n" + ex.getMessage(), ex);
				return;
			}
		}
		
		//update combo viewers
		List<Object> citems = (List<Object>)cmbBound.getInput();
		
		PawsParameter pp = pw.findParameter( PawsParameter.FixedParameter.LYR_BOUNDARY.name() );
		citems.remove(pp);
		citems.add(pp);
		
		for (Iterator<Object> iterator = citems.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			if (object instanceof Path) citems.remove(object);
		}
		cmbBound.refresh();
		cmbBound.setSelection(new StructuredSelection(pp));
		
		
		List<PawsParameter> items = (List<PawsParameter>)lstOther.getInput(); 
		items.clear();
		for (PawsParameter ppw : pw.getParameters()) {
			if (ppw.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name())) items.add(ppw);
			
		}
		lstOther.refresh();
	
		if (isNew) setInput(new ConfigEditorInput(pw));
		ConfigurationEditor.this.setPartName(pw.getName());
		fireModified(isNew, pw);
		setDirty(false);
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		PawsConfiguration temp = new PawsConfiguration();
		temp.setUuid(getInputInternal().getUuid());
		save(temp);
	}

	private PawsParameter getOrCreateParameter(PawsConfiguration pw, PawsParameter.FixedParameter param) {
		PawsParameter pp = pw.findParameter(param.name());
		if (pp == null) {
			pp = new PawsParameter();
			pp.setKey(param.name());
			pp.setConfiguration(pw);
			if (pw.getParameters() == null) pw.setParameters(new ArrayList<>());
			pw.getParameters().add(pp);
		}
		return pp;
	}
	
	private void forceCloseEditor(){
		getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(ConfigurationEditor.this, false);
	}
	
	@Override
	public void doSaveAs() {
		
		String currentName = compHeader.getText();
		InputDialog newNameDialog = new InputDialog(getSite().getShell(), "Save As", "Enter the new Configuration name", currentName, 
				e->{
					if (e.isBlank()) return "Name must be supplied";
					return null;
				}); 
		if (newNameDialog.open() != Window.OK) return;
		String newName = newNameDialog.getValue();
		PawsConfiguration pw = new PawsConfiguration();
		pw.setName(newName);
		save(pw);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
		parentContext.get(EPartService.class).addPartListener(HidePartsPartListener.getInstance());
	}

	private ConfigEditorInput getInputInternal() {
		return ((ConfigEditorInput)getEditorInput());
	}
	
	private void subscribeToEvent(String eventTopic, EventHandler handler){
		parentContext.get(IEventBroker.class).subscribe(eventTopic, handler);
		handlers.add(handler);
	}
	
	private void createEventHandlers() {
		//on delete close editor
		handlers = new ArrayList<>();
		
		subscribeToEvent(PawsEvent.PAWS_CONFIG_DELETE, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<PawsConfiguration> items = (Collection<PawsConfiguration>)data;
				for (PawsConfiguration pc : items){
					if (pc.getUuid().equals(getInputInternal().getUuid())) forceCloseEditor();
				}
			}
		});

		//currently this editor is the only thing that can change config
		//so I don't need to worry about this at this time
//		subscribeToEvent(PawsEvent.PAWS_CONFIG_MODIFY, (event)->{
//			if (parentContext.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)) return;
//
//			Object data = event.getProperty(IEventBroker.DATA);
//			if (data != null){
//				boolean validate = false;
//				Collection<PawsConfiguration> items = (Collection<PawsConfiguration>)data;
//				for (PawsConfiguration pc : items){
//					if (pc.getUuid().equals(getInputInternal().getUuid())) {
//						validate = true;
//						break;
//					}
//				}
//				if (validate) validateAndRefresh();
//			}
//		});
		
		subscribeToEvent(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, e->{
			initPage();
		});
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void createPartControl(Composite parent) {
	
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form main = toolkit.createForm(parent);
		main.getBody().setLayout(new GridLayout());
		main.getBody().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		compHeader = new HeaderComposite(main.getBody(), toolkit, main.getFont(), main.getForeground());
		compHeader.setText("PAWS Configuration Name");
		compHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		compHeader.addListener(SWT.Selection, e->setDirty(true));
		
		createSettingsComp(main.getBody());
		
		SmartUiUtils.createHeaderLabel(main.getBody(), "Classifications");
	
		classComposite = new ClassificationComposite(main.getBody(), this);
		classComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Button btnRun = new Button(main.getBody(), SWT.PUSH);
		btnRun.setText("Run Analysis Using These Settings");
		btnRun.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRun.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.RUN_ICON));
		btnRun.addListener(SWT.Selection,e->{
			
			if (isDirty) {
				if (!MessageDialog.openQuestion(getSite().getShell(), "Save", "You must save your changes before you can run analysis.  Do you want to save your changes now?")) return;
				if (!getSite().getPage().saveEditor(this, false)) return;
				if (isDirty) return;
			}
			if (getInputInternal().getUuid() == null) {
				MessageDialog.openError(getSite().getShell(), "Error", "Configuration must be saved before you can run analysis");
				return;
			}
			PawsConfiguration config = null; 
			try(Session session = HibernateManager.openSession()){
				config = session.get(PawsConfiguration.class, getInputInternal().getUuid());
				config.getConservationArea().getUuid();
			}
			
			try {
				String id = PawsManager.INSTANCE. generateUniqueName(config.getName(), config.getConservationArea());
				ContextInjectionFactory.make(NewPawsRunHandler.class, parentContext).createAndRun(config, null, id);
			} catch (Exception ex) {
				PawsPlugIn.displayLog("Unable to create new analysis from these settings." + "\n\n" + ex.getMessage(), ex);
			}
			
		});
		
		createEventHandlers();
		
		layersJob.schedule();
	}

	@Override
	public void setFocus() {
	}
	
	
	private String validate() {
		String error = txtGridSize.isValid();
		if (error != null) return error;
		
		Object x = cmbBound.getStructuredSelection().getFirstElement();
		if (!(x instanceof Area.AreaType || x instanceof Path || x instanceof PawsParameter)) {
			return "Conservation Area Boundary layer is required.";
		}
//		
//		x = cmbCrs.getStructuredSelection().getFirstElement();
//		if (!(x instanceof Projection)) {
//			return "Projection is required";
//		}

		return null;
	}
	
	private void createSettingsComp(Composite parent) {
		Composite core = toolkit.createComposite(parent);
		core.setLayout(new GridLayout(2, true));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		
		Composite modelspec = toolkit.createComposite(core);
		modelspec.setLayout(new GridLayout());
		modelspec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)modelspec.getLayout()).marginWidth = 0;
		((GridLayout)modelspec.getLayout()).marginHeight = 0;
		
		Composite bmlayers = toolkit.createComposite(core);
		bmlayers.setLayout(new GridLayout());
		bmlayers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)bmlayers.getLayout()).marginWidth = 0;
		((GridLayout)bmlayers.getLayout()).marginHeight = 0;
		
		
		SmartUiUtils.createHeaderLabel(bmlayers, "Map Layers");
		
		Composite inner = toolkit.createComposite(bmlayers);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.LYR_BOUNDARY) + ":");
		cmbBound = createBmCombo(inner);
		cmbBound.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(inner, "Other Map Layers:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Composite others = new Composite(inner, SWT.NONE);
		others.setLayout(new GridLayout(2, false));
		((GridLayout)others.getLayout()).marginWidth = 0;
		((GridLayout)others.getLayout()).marginHeight = 0;
		others.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstOther = new ListViewer(others, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI );
		lstOther.setContentProvider(ArrayContentProvider.getInstance());
		lstOther.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstOther.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Area.AreaType) return ((Area.AreaType)element).getGuiName(Locale.getDefault());
				if (element instanceof Path) return ((Path)element).toString();
				if (element instanceof PawsParameter) {
					PawsParameter pp = (PawsParameter)element;
					if (pp.getValue().startsWith(PawsParameter.AREA_PREFIX)) {
						String at = pp.getValue().substring(PawsParameter.AREA_PREFIX.length());
						return Area.AreaType.valueOf(at).getGuiName(Locale.getDefault());
						
					}else if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)) {
						String file = pp.getValue().substring(PawsParameter.FILE_PREFIX.length());
						return file;
					}
				}
				return super.getText(element);
			}
		});
		lstOther.setInput(new ArrayList<>());
		
		ToolBar tb = new ToolBar(others, SWT.FLAT | SWT.VERTICAL);
		tb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		ToolItem tiAdd = new ToolItem(tb, SWT.PUSH);
		tiAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		tiAdd.addListener(SWT.Selection, e->{
			Menu mnuTemp = new Menu(tb);
			
			Set<Area.AreaType> atypes = new HashSet<>();
			try(Session session = HibernateManager.openSession()){
				atypes.addAll(session.createQuery("SELECT DISTINCT type FROM Area WHERE conservationArea = :ca", Area.AreaType.class)
						.setParameter("ca", SmartDB.getCurrentConservationArea())
						.list());
			}
			
			for (Area.AreaType t : Area.AreaType.values()) {
				if (!atypes.contains(t)) continue;
				MenuItem mi = new MenuItem(mnuTemp, SWT.PUSH);
				mi.setText(t.getGuiName(Locale.getDefault()));
				mi.addListener(SWT.Selection, evt->{
					((Collection<Object>)lstOther.getInput()).add(t);
					setDirty(true);
					lstOther.refresh();
				});
			}
			MenuItem mi = new MenuItem(mnuTemp, SWT.PUSH);
			mi.setText("Shapefiles...");
			mi.addListener(SWT.Selection, evt->{
				FileDialog fd = new FileDialog(tb.getShell(), SWT.OPEN | SWT.MULTI);
				fd.setText("Select Shapefile");
				fd.setFilterExtensions(new String[] {"*.shp", "*.*"});
				fd.setFilterNames(new String[] {"Shapefile (*.shp)", "All Files (*.*)"});
				
				String fname = fd.open();
				if (fname == null) return;
				for (String s : fd.getFileNames()) {
					((Collection<Object>)lstOther.getInput()).add(Paths.get(fd.getFilterPath()).resolve(s));
				}
				setDirty(true);
				lstOther.refresh();
			});
			mnuTemp.setVisible(true);
		});
		
		ToolItem tiDelete = new ToolItem(tb, SWT.PUSH);
		tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		tiDelete.setEnabled(false);
		tiDelete.addListener(SWT.Selection, e->{
			for (Iterator<Object> iterator = lstOther.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object item = (Object) iterator.next();
				((Collection)lstOther.getInput()).remove(item);
				setDirty(true);
				lstOther.refresh();
			}
		});
		lstOther.addSelectionChangedListener(e->tiDelete.setEnabled(!lstOther.getStructuredSelection().isEmpty()));
		
		SmartUiUtils.createHeaderLabel(modelspec, "Model Configurations");
		
		inner = toolkit.createComposite(modelspec);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
//		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.GRID_CRS) + ":");
//		l.setToolTipText("Coordinate Reference System");
//		
//		cmbCrs = new ComboViewer(inner, SWT.READ_ONLY | SWT.FLAT | SWT.BORDER | SWT.DROP_DOWN);
//		cmbCrs.setContentProvider(ArrayContentProvider.getInstance());
//		cmbCrs.setLabelProvider(new LabelProvider() {
//			@Override
//			public String getText(Object element) {
//				if (element instanceof Projection) {
//					return ((Projection) element).getName();
//				}
//				return super.getText(element);
//			}
//		});
//		cmbCrs.getControl().setBackground(inner.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
//		cmbCrs.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		cmbCrs.addPostSelectionChangedListener(e->{
			//update bounds if valid
//			ReferencedEnvelope re = (ReferencedEnvelope)txtBounds.getData(RE_DATA_KEY);
//			CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();
//
//			if (re != null){
//				try{
//					re = ReprojectUtils.reproject(re, crs);
//					txtBounds.setData(RE_DATA_KEY, re);
//					updateBounds();
//				}catch (Exception ex){
//					
//				}
//			}
			
//			setDirty(true);
//		});
		
//		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.GRID_BNDS) + ":");
//		l.setToolTipText("xmin,ymin,xmax,ymax in selected CRS");
		
//		Composite bnds = toolkit.createComposite(inner);
//		bnds.setLayout(new GridLayout(2, false));
//		bnds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
//		((GridLayout)bnds.getLayout()).marginWidth = 0;
//		((GridLayout)bnds.getLayout()).marginHeight = 0;
//		
//		txtBounds = new ErrorText(bnds, txt-> {
//			try {
//				String[] bits = txt.split("\\s+");
//				if (bits.length != 4) throw new Exception();
//				for (int i = 0; i < 4; i ++) Double.parseDouble(bits[i]);
//				return null;
//			}catch (Exception ex) {
//				return "Bounds must be of the format \"xmin ymin xmax ymax\"";
//			}
//		});
//		txtBounds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
//		txtBounds.setText("");
//		txtBounds.addListener(SWT.Modify, e->{
//			try {
//				String[] bits = txtBounds.getText().split("\\s+");
//				if (bits.length != 4) throw new Exception();
//				double xmin= Double.parseDouble(bits[0]);
//				double ymin= Double.parseDouble(bits[1]);
//				double xmax= Double.parseDouble(bits[2]);
//				double ymax= Double.parseDouble(bits[3]);
//				CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();
//				txtBounds.setData(RE_DATA_KEY, new ReferencedEnvelope(xmin, xmax, ymin, ymax, crs));
//				
//			}catch (Exception ex) {
//				txtBounds.setData(RE_DATA_KEY, null);
//			}
//			setDirty(true);
//		});
//		
//		Hyperlink lnkBnds = toolkit.createHyperlink(bnds, "set...", SWT.NONE);
//		
//		Menu mnu = new Menu(lnkBnds);
//		
//		Object[] items = new Object[] {PawsParameter.FixedParameter.LYR_BOUNDARY, cmbBound,
////				PawsParameter.FixedParameter.LYR_ROAD, cmbRoad,
////				PawsParameter.FixedParameter.LYR_CONTOUR, cmbContour,
////				PawsParameter.FixedParameter.LYR_WATER, cmbRiver,
//		};
//		for (int i = 0; i < items.length; i +=2){
//			MenuItem mi = new MenuItem(mnu, SWT.PUSH);
//			mi.setEnabled(false);
//			mi.setText("Set to " + PawsManager.INSTANCE.getName((PawsParameter.FixedParameter)items[i]) + " Bounds");
//			final ComboViewer cmbViewer = (ComboViewer) items[i+1];
//			cmbViewer.addSelectionChangedListener(e->{mi.setEnabled(cmbViewer.getStructuredSelection().getFirstElement() != null);});
//			mi.addListener(SWT.Selection, e->{
//				Job temp = new Job("computing bounds"){
//					@Override
//					protected IStatus run(IProgressMonitor monitor) {
//						Object[] selection = new Object[]{null};
//						getSite().getShell().getDisplay().syncExec(()->{
//							selection[0] = cmbViewer.getStructuredSelection().getFirstElement();
//						});
//						
//						final ReferencedEnvelope re = getBounds(selection[0]);
//						if (re == null) return Status.OK_STATUS;
//						getSite().getShell().getDisplay().syncExec(()->{
//							try {
//								CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();
//								ReferencedEnvelope re2 = ReprojectUtils.reproject(re, crs);
//								txtBounds.setData(RE_DATA_KEY, re2);
//								updateBounds();
//							} catch (Exception e1) {
//								PawsPlugIn.displayLog(e1.getMessage(), e1);
//							}
//						});
//						return Status.OK_STATUS;
//					}
//					
//				};
//				temp.schedule();
//				
//			});
//		}
//		new MenuItem(mnu, SWT.SEPARATOR);
//		MenuItem mi = new MenuItem(mnu, SWT.PUSH);
//		mi.setText("Custom...");
//		mi.addListener(SWT.Selection, e->{
//			CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();
//			
//			ReferencedEnvelope init = (ReferencedEnvelope) txtBounds.getData(RE_DATA_KEY);
//			SelectBoundsMapDialog md = new SelectBoundsMapDialog(Display.getDefault().getActiveShell(), null, init);
//			if (md.open() != Window.OK) return;
//			ReferencedEnvelope re = md.getBounds();
//			try {
//				re = ReprojectUtils.reproject(re, crs);
//				txtBounds.setData(RE_DATA_KEY, re);
//				updateBounds();
//			} catch (Exception e1) {
//				PawsPlugIn.displayLog(e1.getMessage(), e1);
//			}
//		});
//		
//		lnkBnds.addHyperlinkListener(new IHyperlinkListener() {
//			@Override
//			public void linkExited(HyperlinkEvent e) { }
//			@Override
//			public void linkEntered(HyperlinkEvent e) { }
//			
//			@Override
//			public void linkActivated(HyperlinkEvent e) {
//				mnu.setVisible(true);
//			}
//		});
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.GRID_SIZE) + " (meter):");
		l.setToolTipText("units are meters; all processing is done in an appropriate utm zone");
		
		txtGridSize = new ErrorText(inner, txt-> {
			try {
				Double.parseDouble(txtGridSize.getText());
				return null;
			}catch (Exception ex) {
				return "Grid size must be valid number.";
			}
		});
		txtGridSize.setText("100");
		txtGridSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		txtGridSize.addListener(SWT.Modify, e->{
			setDirty(true);	
		});
		
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.TRAINING_RES) + ":");
		l.setToolTipText("monthly aggregation value - groups months by this value for training and prediction");
		
		cmbTrainingRes = new ComboViewer(inner, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbTrainingRes.setContentProvider(ArrayContentProvider.getInstance());
		cmbTrainingRes.getControl().setBackground(inner.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbTrainingRes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbTrainingRes.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return String.valueOf((Integer)element);
			}
		});		
		cmbTrainingRes.setInput(new Integer[] {1,2,3,4,5,6,7,8,9,10,11,12});
		cmbTrainingRes.setSelection(new StructuredSelection(2));
		cmbTrainingRes.addPostSelectionChangedListener(e->setDirty(true));
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.CLASSIFIER_MODEL) + ":");
		l.setToolTipText("model to use for the classifier");
		
		cmbClassifier = new ComboViewer(inner, SWT.READ_ONLY | SWT.DROP_DOWN);
		cmbClassifier.setContentProvider(ArrayContentProvider.getInstance());
		cmbClassifier.getControl().setBackground(inner.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbClassifier.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbClassifier.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof PawsParameter.ClassifierModel) { 
					PawsParameter.ClassifierModel tw = (PawsParameter.ClassifierModel)element;
					switch(tw) {
					case DECISION_TREE: return "Decision Tree (recommended)" ;
					case RANDOM_FOREST: return "Random Forest";
					case GAUSSIAN_PROCESS: return "Gaussian Process (slow)";
					}
				};
				return super.getText(element);
			}
		});		
		cmbClassifier.setInput(PawsParameter.ClassifierModel.values());
		cmbClassifier.setSelection(new StructuredSelection(PawsParameter.ClassifierModel.DECISION_TREE));
		cmbClassifier.addPostSelectionChangedListener(e->setDirty(true));
		
	}
	
	public void fireModified(boolean isNew, PawsConfiguration pc) {
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, parentContext.get(MPart.class));
		data.put(IEventBroker.DATA, Collections.singletonList(pc));
		parentContext.get(IEventBroker.class).post(isNew ? PawsEvent.PAWS_CONFIG_NEW : PawsEvent.PAWS_CONFIG_MODIFY, data);
	}
	
	private ComboViewer createBmCombo(Composite parent) {
		Combo cBnd = new Combo(parent, SWT.FLAT | SWT.READ_ONLY | SWT.BORDER);
		cBnd.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		ComboViewer cmbLayer = new ComboViewer(cBnd);
		cmbLayer.setContentProvider(ArrayContentProvider.getInstance());
		cmbLayer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof Area.AreaType) return ((Area.AreaType)element).getGuiName(Locale.getDefault());
				if (element instanceof Path) return ((Path)element).toString();
				if (element instanceof PawsParameter) {
					String v = ((PawsParameter)element).getValue();
					if (v.startsWith(PawsParameter.FILE_PREFIX)) return v.substring(PawsParameter.FILE_PREFIX.length());
					if (v.startsWith(PawsParameter.AREA_PREFIX)) return Area.AreaType.valueOf( v.substring(PawsParameter.AREA_PREFIX.length())).getGuiName(Locale.getDefault());
					return v;
				}
				return super.getText(element);
			}
		});
		
		cmbLayer.addPostSelectionChangedListener(e->{
			Object selection = cmbLayer.getStructuredSelection().getFirstElement();
			if (selection == CUSTOM_FILE) {
				FileDialog fd = new FileDialog(parent.getShell(), SWT.OPEN);
				fd.setText("Select Layer Shapefile");
				fd.setFilterExtensions(new String[] {"*.shp", "*."});
				fd.setFilterNames(new String[] {"Shapefile (*.shp)", "All Files (*.*)"});
				String file = fd.open();
				if (file == null) return;
				Path p = Paths.get(file);
				List<Object> input = (List<Object>) cmbLayer.getInput();
				if (!input.contains(p)) {
					input.add(input.size() - 1, p);
					cmbLayer.refresh();
				}
				cmbLayer.setSelection(new StructuredSelection(p));
				
			}
			setDirty(true);
		});
		return cmbLayer;
	}

	public void setDirty(boolean dirty) {
		this.isDirty = dirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	private void initPage() {
		layersJob.schedule();
	}
	
	
	private ReferencedEnvelope getBounds(Object source){
		
		if (source instanceof PawsParameter){
			String value = ((PawsParameter)source).getValue();
			if (value.startsWith(PawsParameter.AREA_PREFIX)) {
				source = Area.AreaType.valueOf( value.substring(PawsParameter.AREA_PREFIX.length()) );
			}else if (value.startsWith(PawsParameter.AREA_PREFIX)) {
				PawsConfiguration temp = new PawsConfiguration();
				temp.setUuid(getInputInternal().getUuid());
				temp.setConservationArea(SmartDB.getCurrentConservationArea());
				source = PawsManager.INSTANCE.getDirectory(temp).resolve( value.substring(PawsParameter.FILE_PREFIX.length()) );
			}
		}
		
		if (source instanceof Area.AreaType){
		
			//load bounds from database
			ReferencedEnvelope re = null;
			try(Session session = HibernateManager.openSession()){
				List<Area> areas = HibernateManager.loadAreas(((Area.AreaType)source), session);
				
				for (Area a : areas){
					if (re == null){
						re = new ReferencedEnvelope(a.getGeometry().getEnvelopeInternal(), SmartDB.DATABASE_CRS);
					}else{
						re.expandToInclude(a.getGeometry().getEnvelopeInternal());
					}
				}
			}
			return re;
		}
		
		
		if (source  instanceof Path){
			//assume some sort of file store
			//read bounds using udig
			try {
				List<IService> services = CatalogPlugin.getDefault().getServiceFactory().createService(((Path) source).toUri().toURL());
				if (services.isEmpty()) return null;
				for (IService s : services){
					for (IGeoResource r : s.resources(new NullProgressMonitor())){
						if (r.canResolve(FeatureStore.class)){
							ReferencedEnvelope env = r.resolve(FeatureStore.class, new NullProgressMonitor()).getBounds();
							
							r.dispose(new NullProgressMonitor());
							s.dispose(new NullProgressMonitor());
							
							return env;
							
						}
					}
				}
			} catch (Exception e) {
				PawsPlugIn.log(e.getMessage(), e);
			}
		}
		return null;
	}
	
	private Job layersJob = new Job("loading layers") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> options = new ArrayList<>();
			Projection currentPrj = null;
			List<Projection> allPrjs = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){			
				List<Area.AreaType> types = session.createQuery("SELECT DISTINCT type FROM Area WHERE conservationArea = :ca", Area.AreaType.class)
					.setParameter("ca", SmartDB.getCurrentConservationArea())
					.list();
				options.add("");
				options.addAll(types);
				options.add(CUSTOM_FILE);
				
				currentPrj = HibernateManager.getCurrentViewProjection();
				
				allPrjs.addAll(session.createQuery("FROM Projection WHERE conservationArea = :ca", Projection.class)
					.setParameter("ca",  SmartDB.getCurrentConservationArea())
					.list());
				allPrjs.forEach(prj->{
					try {
						prj.setParsedCoordinateReferenceSystem(CRS.parseWKT(prj.getDefinition()));
					} catch (FactoryException e) {
						PawsPlugIn.log(e.getMessage(),e);
					}	
				});
			}
			
			final Projection fcurrentPrj = currentPrj;
			
			Display.getDefault().syncExec(()->{
				cmbBound.setInput(new ArrayList<>(options));
				if (options.contains(Area.AreaType.CA)) cmbBound.setSelection(new StructuredSelection(Area.AreaType.CA));
//				cmbCrs.setInput(allPrjs);
//				cmbCrs.setSelection(new StructuredSelection(fcurrentPrj));
			});
			
			initFields.schedule();
			
			return Status.OK_STATUS;
		}
		
	};
	
	private Job initFields = new Job("initializing configuration settings") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ConfigEditorInput in = (ConfigEditorInput) getEditorInput();
			if (in.getUuid() == null) {
				setDirty(false);
				return Status.OK_STATUS;
			}
			
			try(Session s = HibernateManager.openSession()){
				PawsConfiguration pw = s.get(PawsConfiguration.class, in.getUuid());
				if (pw == null) {
					throw new Exception("Paws Configuration not found");					
				}
				
				Display.getDefault().syncExec(()->{
					compHeader.setText(pw.getName());
					ConfigurationEditor.this.setPartName(pw.getName());
					
					PawsParameter pp = pw.findParameter(PawsParameter.FixedParameter.LYR_BOUNDARY.name());
					if (pp != null && pp.getValue() != null) {
						if (pp.getValue().startsWith(PawsParameter.AREA_PREFIX)) {
							String at = pp.getValue().substring(PawsParameter.AREA_PREFIX.length());
							cmbBound.setSelection(new StructuredSelection(Area.AreaType.valueOf(at)));
						}else if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)) {
							List<Object> obs = (List<Object>) (cmbBound).getInput();
							obs.add(obs.size() - 1, pp);
							cmbBound.refresh();
							cmbBound.setSelection(new StructuredSelection(pp));
						}
					}
					
					
					List<Object> obs = (List<Object>) (lstOther).getInput();
					for (PawsParameter t : pw.getParameters()) {
						if (t.getKey().equals(PawsParameter.FixedParameter.LYR_OTHER.name())){
							obs.add(t);
						}
					}
					lstOther.refresh();
					
					pp = pw.findParameter(PawsParameter.FixedParameter.GRID_SIZE.name());
					if (pp != null) txtGridSize.setText(pp.getValue());

//					CoordinateReferenceSystem selectedCrs = null;
//					pp = pw.findParameter(PawsParameter.FixedParameter.GRID_CRS.name());
//					if (pp != null) {
//						String uuid = pp.getValue().split(":")[0];
//						
//						Projection temp = new Projection();
//						boolean add = false;
//						List<Projection> prjs = (List<Projection>)cmbCrs.getInput();
//						if (uuid.isBlank()){
//							add = true;
//						}else{
//							temp.setUuid( UuidUtils.stringToUuid(uuid) );
//							if (!prjs.contains(temp)){
//								add = true;
//							}
//						}
//						if (add){
//							temp.setUuid(null);
//							temp.setDefinition(pp.getValue().substring(uuid.length() + 1));
//							temp.setName("Custom");
//							prjs.add(temp);
//							cmbCrs.refresh();
//							try {
//								temp.setParsedCoordinateReferenceSystem(CRS.parseWKT(temp.getDefinition()));
//							}catch (Exception ex){
//								PawsPlugIn.log(ex.getMessage(), ex);
//							}
//						}
//						cmbCrs.setSelection(new StructuredSelection(temp));
//						selectedCrs = temp.getParsedCoordinateReferenceSystem();
//						
//						
//					}
					pp = pw.findParameter(PawsParameter.FixedParameter.TRAINING_RES.name());
					if (pp != null) {
						cmbTrainingRes.setSelection(new StructuredSelection(  Integer.valueOf(pp.getValue()) ));
					}
					
					pp = pw.findParameter(PawsParameter.FixedParameter.CLASSIFIER_MODEL.name());
					if (pp != null) {
						cmbClassifier.setSelection(new StructuredSelection( PawsParameter.ClassifierModel.valueOf( pp.getValue() ) ));
					}
					
//					pp = pw.findParameter(PawsParameter.FixedParameter.GRID_BNDS.name());
//					if (pp != null){
//						try{
//							String[] bits = pp.getValue().split("\\s+");
//							double x1 = Double.parseDouble(bits[0]);
//							double y1 = Double.parseDouble(bits[1]);
//							double x2 = Double.parseDouble(bits[2]);
//							double y2 = Double.parseDouble(bits[3]);
//							ReferencedEnvelope re = new ReferencedEnvelope(x1,x2, y1, y2, selectedCrs);
//							txtBounds.setData(RE_DATA_KEY, re);
//							updateBounds();
//						}catch (Exception ex){
//							txtBounds.setText("");
//						}
//						
//						
//					}
					
//<<<<<<< .working
//					if (pc.getType() == Type.DATAMODEL) {
//						btnOpDataModel.setSelection(true);
//						btnOpQuery.setSelection(false);
//						Category c = QueryDataModelManager.getInstance().getCategory(s, pc.getCategoryHkey());
//				        if (c == null){
//				        	pc.cacheLabel( MessageFormat.format("ERROR: Category {0} not found.", pc.getCategoryHkey()) );
//				        }else{
//				        	if (pc.getAttributeKey() != null) {
//								if (pc.getAttributeListItemKey() != null) {
//									AttributeListItem li = QueryDataModelManager.getInstance().getAttributeListItem(s, pc.getAttributeKey(), pc.getAttributeListItemKey());
//									if (li == null) {
//										pc.cacheLabel( MessageFormat.format("ERROR: Attribute list item {0} not found.", pc.getAttributeListItemKey()) );
//									} else {
//										pc.cacheLabel( PawsClassification.createLabel(c, li.getAttribute(), li) );
//										lblDataModel.setData(SRC_VALUE, pc);
//									}
//								}
//
//								if (pc.getAttributeTreeNodeHkey() != null) {
//									AttributeTreeNode node = QueryDataModelManager.getInstance().getAttributeTreeNode( s, pc.getAttributeKey(), pc.getAttributeTreeNodeHkey());
//									if (node == null) {
//										pc.cacheLabel( MessageFormat.format("ERROR: Attribute tree node {0} not found.",pc.getAttributeTreeNodeHkey()));
//									} else {
//										pc.cacheLabel( PawsClassification.createLabel(c, node.getAttribute(), node));
//										lblDataModel.setData(SRC_VALUE, pc);
//									}
//
//								}
//
//							} else {
//								pc.cacheLabel(PawsClassification.createLabel(c, null, null));
//								lblDataModel.setData(SRC_VALUE, pc);
//							}
//						}
//				        lblDataModel.setText(pc.getCachedLabel());
//				        lblDataModel.getParent().layout(true);
//					}
//					if (pc.getType() == Type.QUERY) {
//						btnOpQuery.setSelection(true);
//						btnOpDataModel.setSelection(false);
//			            Query temp = QueryHibernateManager.getInstance().findQuery(s, pc.getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType( pc.getQueryType()));
//			            if (temp != null){
//			                pc.setCachedQuery(temp);
//			                pc.cacheLabel(PawsManager.INSTANCE.createLabel(temp));
//							lblQuery.setData(SRC_VALUE, pc);
//			            }else{
//			                pc.cacheLabel( "QUERY NOT FOUND" );
//			            }
//			            lblQuery.setText(pc.getCachedLabel());
//			            lblQuery.getParent().layout(true);
//					}
//					
//					updateClassificationUi();
//||||||| .merge-left.r7405
//					if (pc.getType() == Type.DATAMODEL) {
//						btnOpDataModel.setSelection(true);
//						btnOpQuery.setSelection(false);
//						Category c = QueryDataModelManager.getInstance().getCategory(s, pc.getCategoryHkey());
//				        if (c == null){
//				        	pc.cacheLabel( MessageFormat.format("ERROR: Category {0} not found.", pc.getCategoryHkey()) );
//				        }else{
//				        	if (pc.getAttributeKey() != null) {
//								if (pc.getAttributeListItemKey() != null) {
//									AttributeListItem li = QueryDataModelManager.getInstance().getAttributeListItem(s, pc.getAttributeKey(), pc.getAttributeListItemKey());
//									if (li == null) {
//										pc.cacheLabel( MessageFormat.format("ERROR: Attribute list item {0} not found.", pc.getAttributeListItemKey()) );
//									} else {
//										pc.cacheLabel( PawsClassification.createLabel(c, li.getAttribute(), li) );
//										lblDataModel.setData(SRC_VALUE, pc);
//									}
//								}
//
//								if (pc.getAttributeTreeNodeHkey() != null) {
//									AttributeTreeNode node = QueryDataModelManager.getInstance().getAttributeTreeNode( s, pc.getAttributeKey(), pc.getAttributeTreeNodeHkey());
//									if (node == null) {
//										pc.cacheLabel( MessageFormat.format("ERROR: Attribute tree node {0} not found.",pc.getAttributeTreeNodeHkey()));
//									} else {
//										pc.cacheLabel( PawsClassification.createLabel(c, node.getAttribute(), node));
//										lblDataModel.setData(SRC_VALUE, pc);
//									}
//
//								}
//
//							} else {
//								pc.cacheLabel(PawsClassification.createLabel(c, null, null));
//								lblDataModel.setData(SRC_VALUE, pc);
//							}
//						}
//				        lblDataModel.setText(pc.getCachedLabel());
//				        lblDataModel.getParent().layout(true);
//					}
//					if (pc.getType() == Type.QUERY) {
//						btnOpQuery.setSelection(true);
//						btnOpDataModel.setSelection(false);
//			            Query temp = QueryHibernateManager.getInstance().findQuery(s, pc.getQueryUuid(), QueryTypeManager.INSTANCE.findQueryType( pc.getQueryType()));
//			            if (temp != null){
//			                pc.setCachedQuery(temp);
//			                pc.cacheLabel(PawsClassification.createLabel(temp));
//							lblQuery.setData(SRC_VALUE, pc);
//			            }else{
//			                pc.cacheLabel( "QUERY NOT FOUND" );
//			            }
//			            lblQuery.setText(pc.getCachedLabel());
//			            lblQuery.getParent().layout(true);
//					}
//					
//					updateClassificationUi();
//=======
//>>>>>>> .merge-right.r7404
					setDirty(false);
				});
				
				classComposite.initialize(pw, s);
			}catch (Exception ex) {
				PawsPlugIn.log(ex.getMessage(), ex);
			}
			
			
			return Status.OK_STATUS;
		}
	};
		
	class CopyFilesInterceptor extends EmptyInterceptor{
		private static final long serialVersionUID = 1L;

		private List<Path[]> filesToCopy;
		List<String> fileNames = new ArrayList<>();
		List<String> deletedFiles = new ArrayList<>();
		
		/**
		 * 
		 * @param filesToCopy list of arrays representing source and target
		 */
		public CopyFilesInterceptor(List<Path[]> filesToCopy, List<String> fileNames, List<String> deletedFiles) {
			super();
			this.filesToCopy = filesToCopy;
			this.fileNames = fileNames;
			this.deletedFiles = deletedFiles;
		}
		
		@Override
		public void afterTransactionCompletion(Transaction tx) {
			if (tx.getStatus() == TransactionStatus.COMMITTED) {
				Path root = Paths.get(fileNames.get(0));
				fileNames.remove(0);
				if (!Files.exists(root)) {
					try {
						Files.createDirectories(root);
					} catch (IOException e) {
						PawsPlugIn.displayLog("Unable to create directory for configuration files. You should resolve the error and re-create the configuration." + e.getMessage(), e);
					}
				}

				
				for (Path[] p : filesToCopy) {
					try {
						Path source = p[0];
						Path target = p[1];
						
						//Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
						
						//copy over all supporting files (files with the same names)
						//but different extensions.
						String sourceName =  SharedUtils.getFilenameWithoutExtension(source.getFileName().toString());
						String targetName = SharedUtils.getFilenameWithoutExtension(target.getFileName().toString());
						try(Stream<Path> files = Files.list(source.getParent())){
							files.forEach(other->{
								String fName = other.getFileName().toString();
								String n = SharedUtils.getFilenameWithoutExtension(fName);
								String ext = SharedUtils.getFilenameExtension(fName);
								if (n.equalsIgnoreCase(sourceName)) {
									Path copyto = target.getParent().resolve(targetName + "." + ext);
									try {
										Files.copy(other, copyto, StandardCopyOption.REPLACE_EXISTING);
//										Files.setPosixFilePermissions(copyto, Collections.singleton(PosixFilePermission.OWNER_WRITE));
									} catch (Throwable e) {
										PawsPlugIn.displayLog(MessageFormat.format("Error copying shapefile supporting files to SMART Data store {0} to {1}",  other.toString(), copyto.toString()), e);
									}
								}
							});
						}
					} catch (IOException e) {
						PawsPlugIn.displayLog("Error copying layer map file to SMART Data store." + "\n\n" + e.getMessage(), e);
					}
				}

				try {
					//delete any files not referenced
					try(Stream<Path> files = Files.list(root)){
						files.forEach(other->{
					
						String name = SharedUtils.getFilenameWithoutExtension(other.getFileName().toString());
						if (!fileNames.contains(name)) {
							deletedFiles.add(other.getFileName().toString());
							try{
								Files.deleteIfExists(other);
							}catch (IOException ex) {
								PawsPlugIn.log(ex.getMessage(), ex);
							}
						}
						});	
					}
					
				} catch (IOException e) {
					PawsPlugIn.log(e.getMessage(),e);
				}
				
			}
		}
		
	};
}
