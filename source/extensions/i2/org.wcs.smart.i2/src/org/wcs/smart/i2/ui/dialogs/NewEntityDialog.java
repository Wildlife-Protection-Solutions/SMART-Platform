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
package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityManager;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.OtherAttributeGroup;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;

/**
 * New entity dialog 
 * @author Emily
 *
 */
public class NewEntityDialog extends SmartStyledTitleDialog{

	private static final String LAST_TYPE_KEY = "org.wcs.smart.i2.ui.dialogs.newentity.lasttype"; //$NON-NLS-1$
	private static final String LAST_PROFILE_KEY = "org.wcs.smart.i2.ui.dialogs.newentity.lastprofile"; //$NON-NLS-1$

	private static final String MESSAGE = Messages.NewEntityDialog_Message;
	
	private TableComboViewer cmbProfile;	
	private TableComboViewer cmbEntityType;	
	private Composite attributePanel;
	private List<AttributeFieldEditor> attributeControls;
	
	private IntelEntity newEntity = null;
	
	@Inject
	private IEventBroker broker;
	@Inject
	private IEclipseContext context;
	
	private boolean openOnCreate;
	private IntelProfile profile;
	
	private Job loadEntityTypes = new Job(Messages.NewEntityDialog_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<IntelEntityType> types = new ArrayList<IntelEntityType>();
			final List<IntelProfile> profiles = new ArrayList<>();
			
			try(Session s = HibernateManager.openSession()){
				types.addAll(EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea()));
				
				if (profile != null) {
					profile = s.get(IntelProfile.class, profile.getUuid());
					profile.getEntityTypes().size();
				}else {
					Set<IntelProfile> temp = ProfilesManager.INSTANCE.getActiveProfiles()
							.stream().filter(e->IntelSecurityManager.INSTANCE.canCreateEntity(e)).collect(Collectors.toSet());
					if (temp.size() != 0) {
						profile = s.get(IntelProfile.class, temp.iterator().next().getUuid());
						profile.getEntityTypes().size();
					}
					for (IntelProfile p : temp) {
						profiles.add( s.get(IntelProfile.class, p.getUuid()) );
					}
					profiles.forEach(p->p.getEntityTypes().size());
				}
			}
			
			IntelProfile ptoSelect = null;
			if (profile != null) {
				ptoSelect = profile;
			}else if (profiles.size() > 0) {
				ptoSelect = profiles.get(0);
			}
			if (profile != null) {
				String uuid = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(LAST_PROFILE_KEY);
				if (uuid != null) {
					try{
						UUID u = UuidUtils.stringToUuid(uuid);
						for (IntelProfile t : profiles){
							if (t.getUuid().equals(u)){
								ptoSelect = t;
								break;
							}
						}
					}catch (Exception ex){
						//eat me
					}
				}
			}
			IntelEntityType toSelect = types.isEmpty() ? null : types.get(0);
			String uuid = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(LAST_TYPE_KEY);
			if (uuid != null){
				try{
					UUID u = UuidUtils.stringToUuid(uuid);
					for (IntelEntityType t : types){
						if (t.getUuid().equals(u)){
							toSelect = t;
							break;
						}
					}
				}catch (Exception ex){
					//eat me
				}
			}
			
			final IntelEntityType sel = toSelect;
			final IntelProfile psel = ptoSelect;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (cmbProfile == null) {
						if (psel != null) cmbEntityType.setInput(psel.getEntityTypes().stream().map(a->a.getEntityType()).collect(Collectors.toList()));
						if (sel != null) cmbEntityType.setSelection(new StructuredSelection(sel));
						return;
					}
					if (cmbProfile.getControl().isDisposed()) return;
					
					cmbProfile.setInput(profiles);
					if (psel != null) {
						cmbProfile.setSelection(new StructuredSelection(psel));
						cmbEntityType.setInput(psel.getEntityTypes().stream().map(a->a.getEntityType()).collect(Collectors.toList()));
						if (sel != null) cmbEntityType.setSelection(new StructuredSelection(sel));
					}
				}
			});
			return Status.OK_STATUS;
		}
		
	};
	
	/**
	 * Creates a new entity dialog that will open the entity when the dialog is closed
	 * @param parentShell
	 */
	public NewEntityDialog(Shell parentShell) {
		this(parentShell, true, null);
	}
	
	/**
	 * Creates a new entity dialog that will open the entity when the dialog is closed
	 * @param parentShell
	 */
	public NewEntityDialog(Shell parentShell, IntelProfile profile) {
		this(parentShell, true, profile);
	}
	
	/**
	 * Creates a new entity dialog that will not open the entity with the dialog is closed
	 * @param parentShell
	 * @param openOnCreate
	 */
	public NewEntityDialog(Shell parentShell, boolean openOnCreate, IntelProfile profile) {
		super(parentShell);
		this.openOnCreate = openOnCreate;
		this.profile = profile;
	}
	
	/**
	 * Creates a new entity dialog that will not open the entity with the dialog is closed
	 * @param parentShell
	 * @param openOnCreate
	 */
	public NewEntityDialog(Shell parentShell, boolean openOnCreate) {
		this(parentShell, openOnCreate, null);
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x,(int)(p.y*1.6));
	}
	
	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		
		control.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				cd.dispose();
				
			}
		});
		return cd;
	}
	
	protected void okPressed() {
		IntelEntityType type = null;
		Object x = ((IStructuredSelection)cmbEntityType.getSelection()).getFirstElement();
		if (x instanceof IntelEntityType){
			type = (IntelEntityType)x;
		}else{
			MessageDialog.openWarning(getShell(), Messages.NewEntityDialog_ErrorDialogTitle, Messages.NewEntityDialog_InvalidType);
			return;
		}
		
		IntelProfile thisprofile = null;
		if (cmbProfile != null) {
			thisprofile = null;
			x = cmbProfile.getStructuredSelection().getFirstElement();
			if (x instanceof IntelProfile) {
				thisprofile = (IntelProfile) x;
			}else {
				MessageDialog.openWarning(getShell(), Messages.NewEntityDialog_ErrorDialogTitle, Messages.NewEntityDialog_ProfileRequired);
				return;
			}
		}else {
			thisprofile = profile;
		}
		newEntity = new IntelEntity();
		newEntity.setProfile(thisprofile);
		newEntity.setConservationArea(SmartDB.getCurrentConservationArea());
		newEntity.setEntityType(type);
		newEntity.setAttributes(new ArrayList<IntelEntityAttributeValue>());
		for (AttributeFieldEditor c : attributeControls){
			IntelAttribute attribute = c.getAttribute();
			
			IntelEntityAttributeValue ev = new IntelEntityAttributeValue();
			ev.setAttribute(attribute);
			ev.setEntity(newEntity);
			
			boolean add = c.updateValue(ev);
			if (add){
				newEntity.getAttributes().add(ev);
			}
		}
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(newEntity);
				session.flush();
				newEntity.createDataModelItem(session);
				session.getTransaction().commit();
			}catch (Exception ex){
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				MessageDialog.openWarning(getShell(), Messages.NewEntityDialog_ErrorDialogTitle, Messages.NewEntityDialog_SaveError + ex.getMessage());
				Intelligence2PlugIn.log(ex.getMessage(), ex);
				newEntity = null;
				return;
			}
		}
		
		//fire events
		try{
			broker.send(IntelEvents.ENTITY_NEW, newEntity);
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
		}
		
		//open editor
		if (openOnCreate) {
			(new OpenEntityHandler()).openEntity(newEntity, context);
		}
		super.okPressed();
	}
	
	public IntelEntity getNewEntity(){
		return this.newEntity;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	private void modified(){
		boolean isError = false;
		for (AttributeFieldEditor e : attributeControls){
			if (!e.isValid()){
				isError = true;
				break;
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(!isError);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(parent, SWT.NONE);
		l.setText(Messages.NewEntityDialog_Profile);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		if (profile != null) {
			Composite t = new Composite(parent, SWT.NONE);
			t.setLayout(new GridLayout(2, false));
			((GridLayout)t.getLayout()).marginWidth = 0;
			((GridLayout)t.getLayout()).marginHeight = 0;
			
			l = new Label(t, SWT.NONE);
			l.setImage(Resources.INSTANCE.getImage(profile));
			
			l = new Label(t, SWT.NONE);
			l.setText(profile.getName());
			
		}else {
			cmbProfile = new TableComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
			cmbProfile.setContentProvider(ArrayContentProvider.getInstance());
			cmbProfile.setLabelProvider(new ProfileLabelProvider());
			cmbProfile.setInput(new String[]{DialogConstants.LOADING_TEXT});
			cmbProfile.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			SmartUiUtils.configure(cmbProfile);
			cmbProfile.getControl().setEnabled(profile == null);
			cmbProfile.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Object x = ((IStructuredSelection)cmbProfile.getSelection()).getFirstElement();
					if (x instanceof IntelProfile){
						IntelProfile type = (IntelProfile)x;
						Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(LAST_PROFILE_KEY, UuidUtils.uuidToString(type.getUuid()));
						
						cmbEntityType.setInput(type.getEntityTypes().stream().map(m->m.getEntityType()).collect(Collectors.toList()));
						if (!type.getEntityTypes().isEmpty()) {
							cmbEntityType.setSelection(new StructuredSelection(type.getEntityTypes().iterator().next()));
						}else {
							cmbEntityType.setSelection(null);
						}
					}				
				}
			});
		}
		
		l = new Label(parent, SWT.NONE);
		l.setText(Messages.NewEntityDialog_EntityTypeLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		cmbEntityType = new TableComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbEntityType.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityType.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityType.setInput(new String[]{Messages.NewEntityDialog_SelectProfile});
		cmbEntityType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		SmartUiUtils.configure(cmbEntityType);
		cmbEntityType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)cmbEntityType.getSelection()).getFirstElement();
				if (x instanceof IntelEntityType){
					IntelEntityType type = (IntelEntityType)x;
					Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(LAST_TYPE_KEY, UuidUtils.uuidToString(type.getUuid()));
					configureAttributePanel(type);
				}else {
					configureAttributePanel(null);
				}
				
			}
		});
		l = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		attributePanel = new Composite(parent, SWT.NONE);
		attributePanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		setTitle(Messages.NewEntityDialog_Title);
		getShell().setText(Messages.NewEntityDialog_Title);
		setMessage(MESSAGE);
		
		loadEntityTypes.setSystem(true);
		loadEntityTypes.schedule(0);
		
		return parent;
	}
	
	private void configureAttributePanel(IntelEntityType type){
		for (Control c : attributePanel.getChildren()){
			c.dispose();
		}
		
		if (type == null) return;
		
		try(Session s = HibernateManager.openSession()){
			s.saveOrUpdate(type);
			for (IntelEntityTypeAttribute a : type.getAttributes()){
				a.getAttribute().getName();
				if (a.getAttribute().getAttributeList() != null){
					for (IntelAttributeListItem l : a.getAttribute().getAttributeList()){
						l.getName();
					}
				}
			}
		}
		
		attributeControls = new ArrayList<AttributeFieldEditor>();
		attributePanel.setLayout(new GridLayout(2, false));
		((GridLayout)attributePanel.getLayout()).horizontalSpacing = 7;
		SelectionListener listener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				modified();
			}
		};
		AttributeFieldEditor editor = new AttributeFieldEditor(attributePanel, type.getIdAttribute());
		editor.addSelectionListener(listener);
		attributeControls.add(editor);
		for (IntelEntityTypeAttribute a : type.getAttributes()) {
			if (a.getAttribute().equals(type.getIdAttribute()) && a.getDuplicateCheck()) {
				addDuplicateIdChecker(editor);
				break;
			}
		}
		
		
		Label ll = new Label(attributePanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		ll.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

		ScrolledComposite sc = new ScrolledComposite(attributePanel, SWT.V_SCROLL);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Composite content = new Composite(sc, SWT.NONE);
		sc.setContent(content);
		content.setLayout(new GridLayout(2, false));
		((GridLayout)content.getLayout()).horizontalSpacing = 7;
		content.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		type.getAttributes().stream()
			.filter(a -> a.getAttributeGroup() != null)
			.map(a -> a.getAttributeGroup())
			.distinct()
			.sorted((a,b)-> ((Integer)a.getOrder()).compareTo(b.getOrder()))
			.forEach(group -> {
				Composite spacer = new Composite(content, SWT.NONE);
				spacer.setLayout(new GridLayout());
				spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				spacer.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
				((GridLayout)spacer.getLayout()).marginWidth = 3;
				((GridLayout)spacer.getLayout()).marginHeight = 3;
				Label l = new Label(spacer, SWT.NONE);
				l.setText(group.getName());
				l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
				l.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
				
				type.getAttributes().stream().filter(a -> group.equals(a.getAttributeGroup()))
				.sorted((a,b)-> ((Integer)a.getOrder()).compareTo(b.getOrder()))
				.forEach(attribute -> {
					if (!attribute.getAttribute().equals(type.getIdAttribute())){
						AttributeFieldEditor leditor = new AttributeFieldEditor(content, attribute.getAttribute());
						leditor.addSelectionListener(listener);
						attributeControls.add(leditor);
						if (leditor.getTextAttributeControl() != null) {
							leditor.getTextAttributeControl().addListener(SWT.Resize, e-> sc.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
						}
						if (attribute.getDuplicateCheck()) addDuplicateIdChecker(leditor);
					}
				});
			}
		);
		//other
		Composite spacer = new Composite(content, SWT.NONE);
		spacer.setLayout(new GridLayout());
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		spacer.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
		((GridLayout)spacer.getLayout()).marginWidth = 3;
		((GridLayout)spacer.getLayout()).marginHeight = 3;
		Label l = new Label(spacer, SWT.NONE);
		l.setText(OtherAttributeGroup.INSTANCE.getName());
		l.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
		l.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		type.getAttributes().stream().filter(a -> a.getAttributeGroup() == null)
		.sorted((a,b)-> ((Integer)a.getOrder()).compareTo(b.getOrder()))
		.forEach(attribute -> {
			if (!attribute.getAttribute().equals(type.getIdAttribute())){
				AttributeFieldEditor leditor = new AttributeFieldEditor(content, attribute.getAttribute());
				leditor.addSelectionListener(listener);
				attributeControls.add(leditor);
				
				if (leditor.getTextAttributeControl() != null) {
					leditor.getTextAttributeControl().addListener(SWT.Resize, e-> sc.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT)));
				}
				if (attribute.getDuplicateCheck()) addDuplicateIdChecker(leditor);
			}
		});
		
		sc.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		attributePanel.layout(true);	
		
		
	}

	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	private void addDuplicateIdChecker(AttributeFieldEditor editor){
		//check for duplicate identifiers
		editor.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Object type = ((IStructuredSelection)cmbEntityType.getSelection()).getFirstElement();
				if (!(type instanceof IntelEntityType)) return;
				
				try(Session session = HibernateManager.openSession()){
					IntelEntityType etype = (IntelEntityType)session.get(IntelEntityType.class, ((IntelEntityType)type).getUuid());
					IntelEntityAttributeValue tmp = new IntelEntityAttributeValue();
					tmp.setAttribute(editor.getAttribute());
					editor.updateValue(tmp);
					if (EntityManager.INSTANCE.isDuplicateId(tmp.getAttributeValue(), editor.getAttribute(), etype, SmartDB.getCurrentConservationArea(), session, null)){
						String warnMessage = Messages.NewEntityDialog_DuplicateIdMsg2; 
						setMessage(warnMessage, IMessageProvider.WARNING);
						editor.setWarningMessage(warnMessage);
					}else{
						setMessage(MESSAGE);
						editor.setWarningMessage(null);
					}
				}
			}
		});
		
	}
	
}