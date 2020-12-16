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
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.geotools.referencing.CRS;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.opengis.referencing.FactoryException;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsFileManager;
import org.wcs.smart.paws.PawsManager;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsParameter;
import org.wcs.smart.paws.model.PawsParameter.FixedParameter;
import org.wcs.smart.paws.ui.ErrorText;
import org.wcs.smart.paws.ui.HeaderComposite;
import org.wcs.smart.paws.ui.HidePartsPartListener;
import org.wcs.smart.paws.ui.NewPawsRunHandler;
import org.wcs.smart.ui.SmartStyledInputDialog;
import org.wcs.smart.util.SharedUtils;


/**
 * PAWS Configuration editor
 * 
 * @author Emily
 *
 */
public class ConfigurationEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.paws.configuration.editor"; //$NON-NLS-1$
	
	private static final String CUSTOM_FILE = Messages.ConfigurationEditor_CustomFile;

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
		Path target = PawsFileManager.INSTANCE.getDirectory(pw).resolve(  fname );
		int cnt = 1;
		while(Files.exists(target)) {
			fname = namepart + "_" + cnt + "." + extpart; //$NON-NLS-1$ //$NON-NLS-2$
			target = PawsFileManager.INSTANCE.getDirectory(pw).resolve(  fname );
			cnt++;
		}
		return target;
		
	}
	@SuppressWarnings("unchecked")
	private void save(PawsConfiguration pw){
		String valid = validate();
		if (valid != null) {
			MessageDialog.openError(getSite().getShell(), Messages.ConfigurationEditor_ErrorTitls, Messages.ConfigurationEditor_SaveError + "\n\n" + valid); //$NON-NLS-1$
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
	
				fileNames.add(PawsFileManager.INSTANCE.getDirectory(pw).toString());
				
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
						newpp.setValue(PawsParameter.AREA_PREFIX + ((Area.AreaType)item).name());
						
						pw.getParameters().add(newpp);
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
						newpp.setValue(PawsParameter.FILE_PREFIX + target.getFileName().toString());
						
						pw.getParameters().add(newpp);
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
				PawsPlugIn.displayLog(Messages.ConfigurationEditor_SaveError2 + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
			if (object instanceof Path) iterator.remove();
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
		InputDialog newNameDialog = new SmartStyledInputDialog(getSite().getShell(), Messages.ConfigurationEditor_SaveAsTitle, Messages.ConfigurationEditor_SaveAsMsg, currentName, 
				e->{
					if (e.isBlank()) return Messages.ConfigurationEditor_NameRequired;
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
		compHeader.setText(Messages.ConfigurationEditor_ConfigName);
		compHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		compHeader.addListener(SWT.Selection, e->setDirty(true));
		
		createSettingsComp(main.getBody());
		
		Composite c = SmartUiUtils.createHeaderLabel(main.getBody(), Messages.ConfigurationEditor_ClassificationSection);
		((Label)c.getChildren()[0]).setToolTipText(Messages.ConfigurationEditor_ClassificationTooltip);
		
		classComposite = new ClassificationComposite(main.getBody(), this);
		classComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Button btnRun = new Button(main.getBody(), SWT.PUSH);
		btnRun.setText(Messages.ConfigurationEditor_RunBtn);
		btnRun.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnRun.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RUN_ICON));
		btnRun.addListener(SWT.Selection,e->{
			
			if (isDirty) {
				if (!MessageDialog.openQuestion(getSite().getShell(), Messages.ConfigurationEditor_SaveTitle, Messages.ConfigurationEditor_SaveMessage)) return;
				if (!getSite().getPage().saveEditor(this, false)) return;
				if (isDirty) return;
			}
			if (getInputInternal().getUuid() == null) {
				MessageDialog.openError(getSite().getShell(), Messages.ConfigurationEditor_ErrorTitle, Messages.ConfigurationEditor_SaveRequired);
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
				PawsPlugIn.displayLog(Messages.ConfigurationEditor_RunError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
			return Messages.ConfigurationEditor_BoundaryRequired;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
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
		
		
		SmartUiUtils.createHeaderLabel(bmlayers, Messages.ConfigurationEditor_MapLayers);
		
		Composite inner = toolkit.createComposite(bmlayers);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.LYR_BOUNDARY) + ":"); //$NON-NLS-1$
		cmbBound = createBmCombo(inner);
		cmbBound.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(inner, Messages.ConfigurationEditor_OtherLayers);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		l.setToolTipText(Messages.ConfigurationEditor_GISLayersTooltip);
		
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
				atypes.addAll(session.createQuery("SELECT DISTINCT type FROM Area WHERE conservationArea = :ca", Area.AreaType.class) //$NON-NLS-1$
						.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
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
			mi.setText(Messages.ConfigurationEditor_ShapefileOp);
			mi.addListener(SWT.Selection, evt->{
				FileDialog fd = new FileDialog(tb.getShell(), SWT.OPEN | SWT.MULTI);
				fd.setText(Messages.ConfigurationEditor_SelectMsg);
				fd.setFilterExtensions(new String[] {"*.shp", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[] {Messages.ConfigurationEditor_Shapefiles, Messages.ConfigurationEditor_allFiles});
				
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
				((Collection<?>)lstOther.getInput()).remove(item);
				setDirty(true);
				lstOther.refresh();
			}
		});
		lstOther.addSelectionChangedListener(e->tiDelete.setEnabled(!lstOther.getStructuredSelection().isEmpty()));
		
		SmartUiUtils.createHeaderLabel(modelspec, Messages.ConfigurationEditor_ConfigurationsSection);
		
		inner = toolkit.createComposite(modelspec);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.GRID_SIZE) + Messages.ConfigurationEditor_MeterLbl);
		l.setToolTipText(MessageFormat.format("{0}\n{1}", Messages.ConfigurationEditor_unitsTooltip1, Messages.ConfigurationEditor_unitsTooltip2)); //$NON-NLS-1$
		
		txtGridSize = new ErrorText(inner, txt-> {
			try {
				Double.parseDouble(txtGridSize.getText());
				return null;
			}catch (Exception ex) {
				return Messages.ConfigurationEditor_InvalidGridSize;
			}
		});
		txtGridSize.setText("1000"); //$NON-NLS-1$
		txtGridSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		txtGridSize.addListener(SWT.Modify, e->{
			setDirty(true);	
		});
		
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.TRAINING_RES) + ":"); //$NON-NLS-1$
		l.setToolTipText(Messages.ConfigurationEditor_groupByTooltip);
				
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
		cmbTrainingRes.setInput(new Integer[] {1,2,3,4,6,12});
		cmbTrainingRes.setSelection(new StructuredSelection(2));
		cmbTrainingRes.addPostSelectionChangedListener(e->setDirty(true));
		
		l = toolkit.createLabel(inner, PawsManager.INSTANCE.getName(FixedParameter.CLASSIFIER_MODEL) + ":"); //$NON-NLS-1$
		l.setToolTipText(Messages.ConfigurationEditor_classifiertooltip);
		
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
						case DECISION_TREE: return Messages.ConfigurationEditor_DecisionTree ;
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
	
	@SuppressWarnings("unchecked")
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
				fd.setText(Messages.ConfigurationEditor_SelectShapefile);
				fd.setFilterExtensions(new String[] {"*.shp", "*."}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[] {Messages.ConfigurationEditor_Shapefiles, Messages.ConfigurationEditor_allFiles});
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
	
	private Job layersJob = new Job(Messages.ConfigurationEditor_loadingjobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> options = new ArrayList<>();
			List<Projection> allPrjs = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){			
				List<Area.AreaType> types = session.createQuery("SELECT DISTINCT type FROM Area WHERE conservationArea = :ca", Area.AreaType.class) //$NON-NLS-1$
					.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.list();
				options.add(""); //$NON-NLS-1$
				options.addAll(types);
				options.add(CUSTOM_FILE);
				allPrjs.addAll(session.createQuery("FROM Projection WHERE conservationArea = :ca", Projection.class) //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.list());
				allPrjs.forEach(prj->{
					try {
						prj.setParsedCoordinateReferenceSystem(CRS.parseWKT(prj.getDefinition()));
					} catch (FactoryException e) {
						PawsPlugIn.log(e.getMessage(),e);
					}	
				});
			}
			
			Display.getDefault().syncExec(()->{
				cmbBound.setInput(new ArrayList<>(options));
				if (options.contains(Area.AreaType.CA)) cmbBound.setSelection(new StructuredSelection(Area.AreaType.CA));
			});
			
			initFields.schedule();
			
			return Status.OK_STATUS;
		}
		
	};
	
	private Job initFields = new Job(Messages.ConfigurationEditor_initJobname) {

		@SuppressWarnings("unchecked")
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
					throw new Exception(Messages.ConfigurationEditor_ConfigNotFound);					
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

					pp = pw.findParameter(PawsParameter.FixedParameter.TRAINING_RES.name());
					if (pp != null) {
						cmbTrainingRes.setSelection(new StructuredSelection(  Integer.valueOf(pp.getValue()) ));
					}
				
					pp = pw.findParameter(PawsParameter.FixedParameter.CLASSIFIER_MODEL.name());
					if (pp != null) {
						cmbClassifier.setSelection(new StructuredSelection( PawsParameter.ClassifierModel.valueOf( pp.getValue() ) ));
					}
				
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
						PawsPlugIn.displayLog(Messages.ConfigurationEditor_DirCreateError + e.getMessage(), e);
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
									Path copyto = target.getParent().resolve(targetName + "." + ext); //$NON-NLS-1$
									try {
										Files.copy(other, copyto, StandardCopyOption.REPLACE_EXISTING);
									} catch (Throwable e) {
										PawsPlugIn.displayLog(MessageFormat.format(Messages.ConfigurationEditor_CopyError,  other.toString(), copyto.toString()), e);
									}
								}
							});
						}
					} catch (IOException e) {
						PawsPlugIn.displayLog(Messages.ConfigurationEditor_CopyError2 + "\n\n" + e.getMessage(), e); //$NON-NLS-1$
					}
				}

				try {
					//delete any files not referenced
					List<Path> toDelete = new ArrayList<>();
					try(Stream<Path> files = Files.list(root)){
						files.forEach(other->{
					
						String name = SharedUtils.getFilenameWithoutExtension(other.getFileName().toString());
						if (!fileNames.contains(name)) {
							deletedFiles.add(other.getFileName().toString());
							toDelete.add(other);
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
