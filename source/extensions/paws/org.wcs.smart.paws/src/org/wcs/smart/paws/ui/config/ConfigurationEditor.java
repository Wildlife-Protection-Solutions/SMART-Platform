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
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
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
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsParameter.FixedParameter;
import org.wcs.smart.paws.ui.ErrorText;
import org.wcs.smart.paws.ui.HeaderComposite;
import org.wcs.smart.paws.ui.HidePartsPartListener;
import org.wcs.smart.paws.ui.NewPawsRunHandler;
import org.wcs.smart.ui.SelectBoundsMapDialog;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * Script page of editor
 * 
 * @author Emily
 *
 */
public class ConfigurationEditor extends EditorPart {

	private static final String RE_DATA_KEY = "RE";

	public static final String ID = "org.wcs.smart.paws.configuration.editor"; //$NON-NLS-1$

	private static final String CUSTOM_FILE = "Custom File...";

	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	
	private FormToolkit toolkit;

	private HeaderComposite compHeader;
	private ComboViewer cmbBound, cmbRoad, cmbRiver, cmbContour, cmbCrs, cmbTimeZone;
	private ErrorText txtBounds;
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
				Object[] items = new Object[] {PawsParameter.FixedParameter.LYR_BOUNDARY, cmbBound,
						PawsParameter.FixedParameter.LYR_CONTOUR, cmbContour,
						PawsParameter.FixedParameter.LYR_ROAD, cmbRoad,
						PawsParameter.FixedParameter.LYR_WATER, cmbRiver,
				};
				for (int i = 0; i < items.length; i += 2) {
					PawsParameter pp = getOrCreateParameter(pw, (PawsParameter.FixedParameter)items[i]);
					Object x = ((ComboViewer)items[i+1]).getStructuredSelection().getFirstElement();
					if (x instanceof Area.AreaType) {
						pp.setValue(PawsParameter.AREA_PREFIX + ((Area.AreaType)x).name());
					}else if (x instanceof Path || (isNew && x instanceof InternalPath) ) {
						Path path = null;
						if (x instanceof Path){
							path = (Path)x;
						}else{
							PawsConfiguration temp = new PawsConfiguration();
							temp.setUuid(getInputInternal().getUuid());
							temp.setConservationArea(SmartDB.getCurrentConservationArea());
							path = PawsManager.INSTANCE.getDirectory(temp).resolve( ((InternalPath)x).path );
						}
						//import file
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
						pp.setValue( PawsParameter.FILE_PREFIX + fname );
						fileNames.add(SharedUtils.getFilenameWithoutExtension(fname));
						filesToCopy.add(new Path[] {path, (Path)target});
						
					}else if (x instanceof InternalPath) {
						pp.setValue( PawsParameter.FILE_PREFIX + ((InternalPath)x).path );
						fileNames.add( SharedUtils.getFilenameWithoutExtension( ((InternalPath)x).path) );
					}else {
						pp.setValue(null);
					}
				}
			
				PawsParameter pp = getOrCreateParameter(pw, PawsParameter.FixedParameter.GRID_SIZE);
				pp.setValue(txtGridSize.getText());
				
				pp = getOrCreateParameter(pw, PawsParameter.FixedParameter.GRID_BNDS);
				pp.setValue(txtBounds.getText());
				
				pp = getOrCreateParameter(pw, PawsParameter.FixedParameter.TIMEZONE);
				Object tz = cmbTimeZone.getStructuredSelection().getFirstElement();
				pp.setValue( ((TimeZone)tz).getID() );
				
				pp = getOrCreateParameter(pw, PawsParameter.FixedParameter.GRID_CRS);
				Object crs = cmbCrs.getStructuredSelection().getFirstElement();
				Projection prj = (Projection)crs;
				prj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(prj.getDefinition()));
				if (prj.getUuid() != null){
					pp.setValue( UuidUtils.uuidToString(prj.getUuid()) + ":" + prj.getParsedCoordinateReferenceSystem().toWKT() );
				}else{
					pp.setValue( ":" + prj.getParsedCoordinateReferenceSystem().toWKT() );
				}
				
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
		Object[] items = new Object[] {PawsParameter.FixedParameter.LYR_BOUNDARY, cmbBound,
				PawsParameter.FixedParameter.LYR_CONTOUR, cmbContour,
				PawsParameter.FixedParameter.LYR_ROAD, cmbRoad,
				PawsParameter.FixedParameter.LYR_WATER, cmbRiver,
		};
		for (int i = 0; i < items.length; i += 2) {
			List<Object> citems = (List<Object>) ((ComboViewer)items[i+1]).getInput();
			for (String fname : deletedFiles) citems.remove(new InternalPath(fname));
			
			
			PawsParameter pp = pw.findParameter( ((PawsParameter.FixedParameter)items[i]).name() );
			InternalPath ip = null;
			if (pp != null && pp.getValue() != null) {
				if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)) {
					ip = new InternalPath(pp.getValue().substring(PawsParameter.FILE_PREFIX.length()));
					if (!citems.contains(ip)) {
						citems.add(citems.size() - 1, ip);
					}
				}
			}
			((ComboViewer)items[i+1]).refresh();
			if (ip != null) ((ComboViewer)items[i+1]).setSelection(new StructuredSelection(ip));
		}

	
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
//		SmartUiUtils.createHeaderLabel(main.getBody(), "PAWS Settings");
		
		createSettingsComp(main.getBody());
		
		SmartUiUtils.createHeaderLabel(main.getBody(), "Classifications");
	
		classComposite = new ClassificationComposite(main.getBody(), this);
		classComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Button btnRun = new Button(main.getBody(), SWT.PUSH);
		btnRun.setText("Run Analysis Using These Settings");
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
				ContextInjectionFactory.make(NewPawsRunHandler.class, parentContext).createAndRun(config, null, null, id);
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
		if (!(x instanceof Area.AreaType || x instanceof Path || x instanceof InternalPath)) {
			return "Conservation Area Boundary layer is required.";
		}
		
		x = cmbTimeZone.getStructuredSelection().getFirstElement();
		if (!(x instanceof TimeZone)) {
			return "Time zone is required";
		}
		
		x = cmbCrs.getStructuredSelection().getFirstElement();
		if (!(x instanceof Projection)) {
			return "Projection is required";
		}
		if (txtBounds.getData(RE_DATA_KEY) == null){
			return "Valid bounds are required.";
		}
		
		return null;
	}

	private void updateBounds(){
		ReferencedEnvelope env = (ReferencedEnvelope) txtBounds.getData(RE_DATA_KEY);
		if (env != null){
			txtBounds.setText(env.getMinX() + "  " + env.getMinY() + "  " + env.getMaxX() + "  " + env.getMaxY());
		}

	}
	
	private void createSettingsComp(Composite parent) {
		Composite core = toolkit.createComposite(parent);
		core.setLayout(new GridLayout(2, true));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		
		Composite bmlayers = toolkit.createComposite(core);
		bmlayers.setLayout(new GridLayout());
		bmlayers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)bmlayers.getLayout()).marginWidth = 0;
		((GridLayout)bmlayers.getLayout()).marginHeight = 0;
		
		Composite gridspec = toolkit.createComposite(core);
		gridspec.setLayout(new GridLayout());
		gridspec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)gridspec.getLayout()).marginWidth = 0;
		((GridLayout)gridspec.getLayout()).marginHeight = 0;
		
		Composite c = SmartUiUtils.createHeaderLabel(bmlayers, "Map Layers");
		
		Composite inner = toolkit.createComposite(bmlayers);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.LYR_BOUNDARY) + ":");
		cmbBound = createBmCombo(inner);
		cmbBound.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.LYR_ROAD) + ":");
		cmbRoad = createBmCombo(inner);
		cmbRoad.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.LYR_WATER) + ":");
		cmbRiver = createBmCombo(inner);
		cmbRiver.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.LYR_CONTOUR) + ":");
		cmbContour = createBmCombo(inner);
		cmbContour.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		c = SmartUiUtils.createHeaderLabel(gridspec, "Grid Specifications");
		
		inner = toolkit.createComposite(gridspec);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.GRID_CRS) + ":");
		l.setToolTipText("Coordinate Reference System");
		
		cmbCrs = new ComboViewer(inner, SWT.READ_ONLY | SWT.FLAT | SWT.BORDER | SWT.DROP_DOWN);
		cmbCrs.setContentProvider(ArrayContentProvider.getInstance());
		cmbCrs.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Projection) {
					return ((Projection) element).getName();
				}
				return super.getText(element);
			}
		});
		cmbCrs.getControl().setBackground(inner.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbCrs.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbCrs.addPostSelectionChangedListener(e->{
			//update bounds if valid
			ReferencedEnvelope re = (ReferencedEnvelope)txtBounds.getData(RE_DATA_KEY);
			CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();

			if (re != null){
				try{
					re = ReprojectUtils.reproject(re, crs);
					txtBounds.setData(RE_DATA_KEY, re);
					updateBounds();
				}catch (Exception ex){
					
				}
			}
			
			setDirty(true);
		});
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.GRID_BNDS) + ":");
		l.setToolTipText("xmin,ymin,xmax,ymax in selected CRS");
		
		Composite bnds = toolkit.createComposite(inner);
		bnds.setLayout(new GridLayout(2, false));
		bnds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridLayout)bnds.getLayout()).marginWidth = 0;
		((GridLayout)bnds.getLayout()).marginHeight = 0;
		
		txtBounds = new ErrorText(bnds, txt-> {
			try {
				String[] bits = txt.split("\\s+");
				if (bits.length != 4) throw new Exception();
				for (int i = 0; i < 4; i ++) Double.parseDouble(bits[i]);
				return null;
			}catch (Exception ex) {
				return "Bounds must be of the format \"xmin ymin xmax ymax\"";
			}
		});
		txtBounds.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		txtBounds.setText("");
		txtBounds.addListener(SWT.Modify, e->{
			try {
				String[] bits = txtBounds.getText().split("\\s+");
				if (bits.length != 4) throw new Exception();
				double xmin= Double.parseDouble(bits[0]);
				double ymin= Double.parseDouble(bits[1]);
				double xmax= Double.parseDouble(bits[2]);
				double ymax= Double.parseDouble(bits[3]);
				CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();
				txtBounds.setData(RE_DATA_KEY, new ReferencedEnvelope(xmin, xmax, ymin, ymax, crs));
				
			}catch (Exception ex) {
				txtBounds.setData(RE_DATA_KEY, null);
			}
			setDirty(true);
		});
		
		Hyperlink lnkBnds = toolkit.createHyperlink(bnds, "set...", SWT.NONE);
		
		Menu mnu = new Menu(lnkBnds);
		
		Object[] items = new Object[] {PawsParameter.FixedParameter.LYR_BOUNDARY, cmbBound,
				PawsParameter.FixedParameter.LYR_ROAD, cmbRoad,
				PawsParameter.FixedParameter.LYR_CONTOUR, cmbContour,
				PawsParameter.FixedParameter.LYR_WATER, cmbRiver,
		};
		for (int i = 0; i < items.length; i +=2){
			MenuItem mi = new MenuItem(mnu, SWT.PUSH);
			mi.setEnabled(false);
			mi.setText("Set to " + PawsManager.INSTANCE.getName((PawsParameter.FixedParameter)items[i]) + " Bounds");
			final ComboViewer cmbViewer = (ComboViewer) items[i+1];
			cmbViewer.addSelectionChangedListener(e->{mi.setEnabled(cmbViewer.getStructuredSelection().getFirstElement() != null);});
			mi.addListener(SWT.Selection, e->{
				Job temp = new Job("computing bounds"){
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Object[] selection = new Object[]{null};
						getSite().getShell().getDisplay().syncExec(()->{
							selection[0] = cmbViewer.getStructuredSelection().getFirstElement();
						});
						
						final ReferencedEnvelope re = getBounds(selection[0]);
						if (re == null) return Status.OK_STATUS;
						getSite().getShell().getDisplay().syncExec(()->{
							try {
								CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();
								ReferencedEnvelope re2 = ReprojectUtils.reproject(re, crs);
								txtBounds.setData(RE_DATA_KEY, re2);
								updateBounds();
							} catch (Exception e1) {
								PawsPlugIn.displayLog(e1.getMessage(), e1);
							}
						});
						return Status.OK_STATUS;
					}
					
				};
				temp.schedule();
				
			});
		}
		new MenuItem(mnu, SWT.SEPARATOR);
		MenuItem mi = new MenuItem(mnu, SWT.PUSH);
		mi.setText("Custom...");
		mi.addListener(SWT.Selection, e->{
			CoordinateReferenceSystem crs = ((Projection)cmbCrs.getStructuredSelection().getFirstElement()).getParsedCoordinateReferenceSystem();
			
			ReferencedEnvelope init = (ReferencedEnvelope) txtBounds.getData(RE_DATA_KEY);
			SelectBoundsMapDialog md = new SelectBoundsMapDialog(Display.getDefault().getActiveShell(), null, init);
			if (md.open() != Window.OK) return;
			ReferencedEnvelope re = md.getBounds();
			try {
				re = ReprojectUtils.reproject(re, crs);
				txtBounds.setData(RE_DATA_KEY, re);
				updateBounds();
			} catch (Exception e1) {
				PawsPlugIn.displayLog(e1.getMessage(), e1);
			}
		});
		
		lnkBnds.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) { }
			@Override
			public void linkEntered(HyperlinkEvent e) { }
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				mnu.setVisible(true);
			}
		});
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.GRID_SIZE) + ":");
		l.setToolTipText("grid units are the units of the selected CRS");
		
		txtGridSize = new ErrorText(inner, txt-> {
			try {
				Integer.parseInt(txtGridSize.getText());
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
		
		l = toolkit.createLabel(inner,PawsManager.INSTANCE.getName(FixedParameter.TIMEZONE) + ":");
		cmbTimeZone = new ComboViewer(inner, SWT.READ_ONLY | SWT.FLAT | SWT.BORDER | SWT.DROP_DOWN);
		cmbTimeZone.setContentProvider(ArrayContentProvider.getInstance());
		cmbTimeZone.getControl().setBackground(inner.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		cmbTimeZone.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbTimeZone.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof TimeZone) return ((TimeZone)element).getDisplayName() + " (" + ((TimeZone)element).getID() + ")";
				return super.getText(element);
			}
		});
		List<TimeZone> zones = new ArrayList<>();
		for (String id : TimeZone.getAvailableIDs()) {
			zones.add(TimeZone.getTimeZone(id));
		}
		cmbTimeZone.setInput(zones);
		cmbTimeZone.setSelection(new StructuredSelection(TimeZone.getDefault()));
		cmbTimeZone.addPostSelectionChangedListener(e->setDirty(true));
		

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
				if (element instanceof InternalPath) return ((InternalPath)element).path;
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
		if (source instanceof InternalPath){
			PawsConfiguration temp = new PawsConfiguration();
			temp.setUuid(getInputInternal().getUuid());
			temp.setConservationArea(SmartDB.getCurrentConservationArea());
			source = PawsManager.INSTANCE.getDirectory(temp).resolve( ((InternalPath)source).path );
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
				// TODO Auto-generated catch block
				e.printStackTrace();
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
				List<Area.AreaType> types = session.createQuery("SELECT DISTINCT type FROM Area WHERE conservationArea = :ca")
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	
				});
			}
			
			final Projection fcurrentPrj = currentPrj;
			
			Display.getDefault().syncExec(()->{
				cmbBound.setInput(new ArrayList<>(options));
				if (options.contains(Area.AreaType.CA)) cmbBound.setSelection(new StructuredSelection(Area.AreaType.CA));
				cmbContour.setInput(new ArrayList<>(options));
				cmbRiver.setInput(new ArrayList<>(options));
				cmbRoad.setInput(new ArrayList<>(options));
				
				cmbCrs.setInput(allPrjs);
				cmbCrs.setSelection(new StructuredSelection(fcurrentPrj));
			});
			
			initFields.schedule();
			
			return Status.OK_STATUS;
		}
		
	};
	
	private Job initFields = new Job("initializing configuration settings ") {

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
					//TODO: ERROR
					
				}
				
				Display.getDefault().syncExec(()->{
					compHeader.setText(pw.getName());
					ConfigurationEditor.this.setPartName(pw.getName());
					
					Object[] items = new Object[] {PawsParameter.FixedParameter.LYR_BOUNDARY, cmbBound,
							PawsParameter.FixedParameter.LYR_CONTOUR, cmbContour,
							PawsParameter.FixedParameter.LYR_ROAD, cmbRoad,
							PawsParameter.FixedParameter.LYR_WATER, cmbRiver,
					};
					for (int i = 0; i < items.length; i +=2) {
						PawsParameter pp = pw.findParameter(((PawsParameter.FixedParameter)items[i]).name());
						if (pp != null && pp.getValue() != null) {
							if (pp.getValue().startsWith(PawsParameter.AREA_PREFIX)) {
								String at = pp.getValue().substring(PawsParameter.AREA_PREFIX.length());
								((ComboViewer)items[i+1]).setSelection(new StructuredSelection(Area.AreaType.valueOf(at)));
							}else if (pp.getValue().startsWith(PawsParameter.FILE_PREFIX)) {
								String file = pp.getValue().substring(PawsParameter.FILE_PREFIX.length());
								InternalPath ip = new InternalPath(file);
								List<Object> obs = (List<Object>) ((ComboViewer)items[i+1]).getInput();
								obs.add(obs.size() - 1, ip);
								((ComboViewer)items[i+1]).refresh();
								((ComboViewer)items[i+1]).setSelection(new StructuredSelection(ip));
							}
						}
					}
	
					
					PawsParameter pp = pw.findParameter(PawsParameter.FixedParameter.GRID_SIZE.name());
					if (pp != null) txtGridSize.setText(pp.getValue());
					
					pp = pw.findParameter(PawsParameter.FixedParameter.TIMEZONE.name());
					if (pp != null) cmbTimeZone.setSelection(new StructuredSelection(TimeZone.getTimeZone(pp.getValue())));
					
					CoordinateReferenceSystem selectedCrs = null;
					pp = pw.findParameter(PawsParameter.FixedParameter.GRID_CRS.name());
					if (pp != null) {
						String uuid = pp.getValue().split(":")[0];
						String def = pp.getValue().substring(uuid.length() + 1);
						
						Projection temp = new Projection();
						boolean add = false;
						if (uuid.isBlank()){
							add = true;
						}else{
							temp.setUuid( UuidUtils.stringToUuid(uuid) );
							
							if (!((List<Projection>)cmbCrs.getInput()).contains(temp)){
								add = true;
							}else{
								Projection prj = ((List<Projection>)cmbCrs.getInput()).get(((List<Projection>)cmbCrs.getInput()).indexOf(temp));
								if (!prj.getDefinition().equals(def)) add = true;
							}
						}
						if (add){
							temp.setUuid(null);
							temp.setDefinition(pp.getValue().substring(uuid.length() + 1));
							temp.setName("Custom");
							((List<Projection>)cmbCrs.getInput()).add(temp);
							cmbCrs.refresh();
							try {
								temp.setParsedCoordinateReferenceSystem(CRS.parseWKT(temp.getDefinition()));
							}catch (Exception ex){
								PawsPlugIn.log(ex.getMessage(), ex);
							}
						}
						cmbCrs.setSelection(new StructuredSelection(temp));
						selectedCrs = temp.getParsedCoordinateReferenceSystem();
					}
					
					pp = pw.findParameter(PawsParameter.FixedParameter.GRID_BNDS.name());
					if (pp != null){
						try{
							String[] bits = pp.getValue().split("\\s+");
							double x1 = Double.parseDouble(bits[0]);
							double y1 = Double.parseDouble(bits[1]);
							double x2 = Double.parseDouble(bits[2]);
							double y2 = Double.parseDouble(bits[3]);
							ReferencedEnvelope re = new ReferencedEnvelope(x1,x2, y1, y2, selectedCrs);
							txtBounds.setData(RE_DATA_KEY, re);
							updateBounds();
						}catch (Exception ex){
							txtBounds.setText("");
						}
						
						
					}
					
					setDirty(false);
				});
				
				classComposite.initialize(pw, s);
			}
			
			
			return Status.OK_STATUS;
		}
	};
	
	class InternalPath {
		String path;
		
		public InternalPath(String pp) {
			this.path = pp;
		}
		
		public int hashCode() {
			return path.hashCode();
		}
		
		public boolean equals(Object other) {
			if (other == null) return false;
			if (this == other) return true;
			if (other.getClass() != this.getClass()) return false;
			return path.equals( ((InternalPath)other).path );
					
		}
	}
	
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
				for (Path[] p : filesToCopy) {
					try {
						if(!Files.exists(p[1].getParent()) ) {
							Files.createDirectories(p[1].getParent());
						}
						
						Path source = p[0];
						Path target = p[1];
						
						Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
						
						//copy over all supporting files (files with the same names)
						//but different extensions.
						String sourceName =  SharedUtils.getFilenameWithoutExtension(source.getFileName().toString());
						String targetName = SharedUtils.getFilenameWithoutExtension(target.getFileName().toString());
						Files.list(source.getParent()).forEach(other->{
							String fName = other.getFileName().toString();
							String n = SharedUtils.getFilenameWithoutExtension(fName);
							String ext = SharedUtils.getFilenameExtension(fName);
							if (n.equalsIgnoreCase(sourceName)) {
								Path copyto = target.getParent().resolve(targetName + "." + ext);
								try {
									Files.copy(other, copyto, StandardCopyOption.REPLACE_EXISTING);
								} catch (IOException e) {
									PawsPlugIn.displayLog(MessageFormat.format("Error copying shapefile supporting files to SMART Data store {0} to {1}",  other.toString(), copyto.toString()), e);
								}
								}
						});
					} catch (IOException e) {
						PawsPlugIn.displayLog("Error copying layer map file to SMART Data store." + "\n\n" + e.getMessage(), e);
					}
				}
				
				//delete any files not referenced
				Path root = Paths.get(fileNames.get(0));
				
				fileNames.remove(0);
				try {
					if (!Files.exists(root)) Files.createDirectories(root);
					Files.list(root).forEach(other->{
						String name = SharedUtils.getFilenameWithoutExtension(other.getFileName().toString());
						if (!fileNames.contains(name)) {
							try {
								deletedFiles.add(other.getFileName().toString());
								Files.delete(other);
							} catch (IOException e) {
								PawsPlugIn.log(e.getMessage(),e);
							}
						}
					});
				} catch (IOException e) {
					PawsPlugIn.log(e.getMessage(),e);
				}
				
			}
		}
		
	};
}
