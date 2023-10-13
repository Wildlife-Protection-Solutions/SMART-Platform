/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.incident.pkg.ui;

import java.nio.file.Path;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.EmployeeTeam;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageConfigurator;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageProperty;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.incident.internal.Messages;
import org.wcs.smart.cybertracker.incident.model.IncidentCtPackage;
import org.wcs.smart.cybertracker.incident.model.IncidentMetadataField;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.properties.CtProfileLabelProvider;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesDialog;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.ui.CheckboxSelectorKeyAdapter;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Patrol cybertracker UI configuration 
 * 
 * @author Emily
 *
 */
public class CtIncidentPackageConfigurator implements ICtPackageConfigurator {
	
	private IncidentCtPackage ctpackage;
	
	private ComboViewer modelViewer;
	private ComboViewer profileViewer;
	private Text txtName;
	
	private List<IPackageUiContribution> contributions = null;
	private ConfigurableModel selectedModel = null;
	private CyberTrackerPropertiesProfile cmDefaultProfile = null;

	private Consumer<String> onValidate;
	private Consumer<Boolean> onModified;

	private CheckboxTableViewer lstEmployees;
	private Font boldFont;
	
	private boolean isInit = false;
	
	@Inject
	private IEclipseContext context;
	
	public CtIncidentPackageConfigurator() {
		contributions = new ArrayList<>();
		for ( IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
			if (c.getUiController() != null) contributions.add(c.getUiController());
		}
	}
	
	@Override
	public void createGui(Composite parent, ICtPackage ctitem, Consumer<String> onValidate,
			Consumer<Boolean> onModified) {
		contributions.forEach(e->ContextInjectionFactory.inject(e, context));
		
		this.onValidate = onValidate;
		this.onModified = onModified;
		if (!(ctitem instanceof IncidentCtPackage)) throw new IllegalStateException(Messages.CtIncidentPackageConfigurator_InvalidType);
		this.ctpackage = (IncidentCtPackage) ctitem;
	
		
		CTabFolder tabs = new CTabFolder(parent, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tabs.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		CTabItem mainTab = new CTabItem(tabs, SWT.NONE);
		mainTab.setText(Messages.CtIncidentPackageConfigurator_SettingsTabName);
		
		ScrolledComposite scroll = new ScrolledComposite(tabs,  SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		
		Composite main = new Composite(scroll, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		scroll.setContent(main);
		mainTab.setControl(scroll);
		
		Composite outer = new Composite(main, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartUiUtils.createHeaderLabel(outer, Messages.CtIncidentPackageConfigurator_ConfigurationSectionHeader);
		
		Composite g = new Composite(outer, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label nameLabel = new Label(g, SWT.NONE);
		nameLabel.setText(Messages.CtIncidentPackageConfigurator_NameLabel);
		
		txtName = new Text(g, SWT.BORDER);
		txtName.setText(ctitem.getName() == null ? (ctitem.getTypeIdentifier() + Messages.CtIncidentPackageConfigurator_DefaultName) : ctitem.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->{ if (!isInit) validate();});
		
		Label modelLabel = new Label(g, SWT.NONE);
		modelLabel.setText(Messages.CtIncidentPackageConfigurator_ConfigLabel);
		
		modelViewer = new ComboViewer(g, SWT.READ_ONLY);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)modelViewer.getControl().getLayoutData()).widthHint = 100;
		modelViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelViewer.setLabelProvider(new ConfigurableModelLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DataModelWrapper) {
					return Messages.CtIncidentPackageConfigurator_OriginalDmOption;
				}
				return super.getText(element);
			}
		});
		modelViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		modelViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		modelViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedModel = null;
				Object profile = ((IStructuredSelection)modelViewer.getSelection()).getFirstElement();
				if (profile instanceof ConfigurableModel) {
					selectedModel = (ConfigurableModel)profile;
					cmDefaultProfile = getAssciatedProfile(selectedModel);
					profileViewer.setSelection(new StructuredSelection(cmDefaultProfile));
				}else if (profile instanceof DataModelWrapper) {
					selectedModel = null;					
				}
				if (selectedModel == null) {
					context.set(ConfigurableModel.class, new ConfigurableModel());
				}else {
					context.set(ConfigurableModel.class, selectedModel);
				}
				{ if (!isInit) validate();}
			}
		});

		
		Label lblProfile = new Label(g, SWT.NONE);
		lblProfile.setText(Messages.CtIncidentPackageConfigurator_SettingLabel);

		Composite c = new Composite(g, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		profileViewer = new ComboViewer(c, SWT.READ_ONLY);
		profileViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		profileViewer.setContentProvider(ArrayContentProvider.getInstance());
		profileViewer.setLabelProvider(new CtProfileLabelProvider());
		profileViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		profileViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				{ if (!isInit) validate();}
			}
		});
		
		ToolBar tb = new ToolBar(c, SWT.FLAT);
		ToolItem tiEdit = new ToolItem(tb,SWT.PUSH);
		tiEdit.setToolTipText(Messages.CtIncidentPackageConfigurator_Settingstooltip);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.addListener(SWT.Selection, e->{
			Object x = profileViewer.getStructuredSelection().getFirstElement();
			if (!(x instanceof CyberTrackerPropertiesProfile)) return;
			
			Dialog dialog = new CyberTrackerPropertiesDialog(c.getShell(), (CyberTrackerPropertiesProfile)x);
			dialog.open();
		});
		
		ObservationOptions ops = null;
		try(Session session = HibernateManager.openSession()){
			ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
		}
		
		if (ops != null && ops.getTrackObserver()) {
			
			SmartUiUtils.createHeaderLabel(outer, Messages.CtIncidentPackageConfigurator_ObserverHeader);
			
			Composite obs = new Composite(outer, SWT.NONE);
			obs.setLayout(new GridLayout());
			obs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			Label l = new Label(obs, SWT.WRAP);
			l.setText(Messages.CtIncidentPackageConfigurator_EmployeeListDetails);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 200;
			
			ColumnLabelProvider employeeLblProvider = new ColumnLabelProvider() {

				@Override
				public String getText(Object element) {
					if (element instanceof EmployeeTeam) return ((EmployeeTeam)element).getName();
					if (element instanceof Employee) return SmartLabelProvider.getShortLabel((Employee)element);
					return super.getText(element);
				}
				
				@Override
				public Font getFont(Object element) {
					if (element instanceof EmployeeTeam || element instanceof Employee) return null;
					return boldFont;
				}
			};
			
			lstEmployees = CheckboxTableViewer.newCheckList(obs, SWT.BORDER | SWT.MULTI);
			lstEmployees.getTable().addKeyListener(new CheckboxSelectorKeyAdapter(lstEmployees));
			lstEmployees.setContentProvider(ArrayContentProvider.getInstance());
			lstEmployees.setLabelProvider(employeeLblProvider);
			lstEmployees.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridData)lstEmployees.getControl().getLayoutData()).heightHint = 80;
			FontData fd = lstEmployees.getControl().getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			boldFont = new Font(lstEmployees.getControl().getDisplay(), fd);
			lstEmployees.getControl().addListener(SWT.Dispose,e->boldFont.dispose());
			ViewerFilter filter = new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					if (element instanceof Employee || element instanceof EmployeeTeam) return lstEmployees.getChecked(element);
					return true;
				}
			};
			lstEmployees.addCheckStateListener(new ICheckStateListener() {
				
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					validate(true);
				}
			});
			Button btnChecked = new Button(obs, SWT.CHECK);
			btnChecked.setText(Messages.CtIncidentPackageConfigurator_ShowOnlyChecked);
			btnChecked.addListener(SWT.Selection, e->{
				lstEmployees.getControl().setVisible(false);
				if (btnChecked.getSelection()) lstEmployees.addFilter(filter);
				else lstEmployees.removeFilter(filter);
				lstEmployees.getControl().setVisible(true);
			});
			
			
		}
		
		//main page contributions
		if (contributions != null) {
			for (IPackageUiContribution cc : contributions) {
				if (!cc.isTab()) {
					Composite part = cc.createUi(main, ctpackage, e->validate(), ()->validate(false));
					if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));	
				}
			}
		}
		
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		//tab contributaions
		if (contributions != null) {
			for (IPackageUiContribution cc : contributions) {
				if (cc.isTab()) {
					CTabItem item = new CTabItem(tabs, SWT.NONE);
					item.setText(cc.getTabName());
					Composite all = new Composite(tabs, SWT.NONE);
					all.setLayout(new GridLayout());
					item.setControl(all);
					Composite part = cc.createUi(all, ctpackage, e->validate(), ()->validate(false));
					if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				}
			}
		}
		
		loadData();
		
	}

	@Override
	public void save() throws Exception {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				if (ctpackage.getUuid() != null) {
					ctpackage = session.get(ctpackage.getClass(), ctpackage.getUuid());
				}else {
					session.persist(ctpackage);
				}
				
				Object first = modelViewer.getStructuredSelection().getFirstElement();
				if (first instanceof ConfigurableModel) {
					ctpackage.setConfigurableModel((ConfigurableModel) first);
				}else {
					ctpackage.setConfigurableModel(null);
				}
				ctpackage.setCtProfile((CyberTrackerPropertiesProfile) profileViewer.getStructuredSelection().getFirstElement());
				ctpackage.setName(txtName.getText());
				
				MetadataFieldValue mdObserver = findMetadataValue(IncidentMetadataField.MEMBERS, ctpackage);
				if (lstEmployees != null) {
					mdObserver.setVisible(true);
					
					if (mdObserver.getUuidList() == null) mdObserver.setUuidList(new ArrayList<>());
					List<MetadataFieldUuidValue> items = new ArrayList<>();
					
					for (Object x : lstEmployees.getCheckedElements()) {
						UUID uuid = null;
						if (x instanceof Employee) {
							uuid = ((Employee)x).getUuid();
						}else if (x instanceof EmployeeTeam) {
							uuid = ((EmployeeTeam)x).getUuid();
						}
						if (uuid == null) continue;
						
						MetadataFieldUuidValue value = null;
						for (MetadataFieldUuidValue mdvalue: mdObserver.getUuidList()) {
							if (mdvalue.getUuidValue().equals(uuid)) {
								value = mdvalue;
								break;
							}
						}
						if (value == null) {
							value = new MetadataFieldUuidValue();
							value.setUuidValue(uuid);
							value.setMetadata(mdObserver);
						}
						items.add(value);
					}
					mdObserver.getUuidList().clear();
					mdObserver.getUuidList().addAll(items);
					
				}else {
					mdObserver.setVisible(false);
				}

				for (IPackageUiContribution cc : contributions) {
					cc.updatePackage(ctpackage, session);
				}				
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
	}
	
	private MetadataFieldValue findMetadataValue(IncidentMetadataField field, IncidentCtPackage ppackage) {
		MetadataFieldValue v = null;
		for (MetadataFieldValue md : ppackage.getMetadataValues()) {
			if (md.getMetadataKey().equalsIgnoreCase(field.name())) {
				v = md;
				break;
			}
		}
		
		if (v == null) {
			v = new MetadataFieldValue();
			v.setCtPackage((AbstractCtPackage)ppackage);
			v.setConservationArea(ppackage.getConservationArea());
			v.setMetadataKey(field.name());
			v.setRequired(false);
			ppackage.getMetadataValues().add(v);
		}
		v.setVisible(true);
		return v;
	}
	
	private void validate() {
		validate(true);
	}
	private void validate(boolean modified) {
		if (modified) onModified.accept(true);
		
		try {
			if (txtName.getText().isBlank()) {
				throw new Exception(Messages.CtIncidentPackageConfigurator_PackageRequired);
			}
		
			if (modelViewer.getSelection().isEmpty()) {
				throw new Exception(Messages.CtIncidentPackageConfigurator_ModelRequired);
			}
		
			if (profileViewer.getSelection().isEmpty()) {
				throw new Exception(Messages.CtIncidentPackageConfigurator_SettingsRequired);
			}
			
			if (lstEmployees != null) {
				boolean ok = false;
				for (Object x : lstEmployees.getCheckedElements()) {
					if (x instanceof Employee || x instanceof EmployeeTeam) {
						ok = true;
						break;
					}
				}
				if (!ok) {
					throw new Exception(Messages.CtIncidentPackageConfigurator_ObserverRequired);
				}
			}
			
			for (IPackageUiContribution cc : contributions) {
				String x = cc.isValid();
				if (x != null) throw new Exception(x);
			}
		}catch (Exception ex) {
			onValidate.accept(ex.getMessage());
			return;
		}
		onValidate.accept(null);
	}
	
	private CyberTrackerPropertiesProfile getAssciatedProfile(Object src) {
		try (Session session = HibernateManager.openSession()){
			if (src instanceof ConfigurableModel) {
				ConfigurableModel cm = (ConfigurableModel) src;
				ConfigurableModelCtPropertiesProfile cmctp = CyberTrackerHibernateManager.getAssociatedCmProfile(session, cm);
				CyberTrackerPropertiesProfile  profile = cmctp.getProfile();
				profile.equals(profile);
				return profile;
			} else {
				CyberTrackerPropertiesProfile profile = CyberTrackerHibernateManager.getDefaultProfile(session);
				profile.equals(profile);
				return profile;
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.CtIncidentPackageConfigurator_ErrorLoadingSettings, ex);
			return null;
		}
	}
	
	private void loadData() {		
    	
		Job j = new Job("loading data") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> modelList = new ArrayList<>();
				List<CyberTrackerPropertiesProfile>  profiles = new ArrayList<>();
				DataModelWrapper dm = new DataModelWrapper();
				
				IncidentCtPackage init = null;
				List<EmployeeTeam> eteams ;
				List<Employee> es;
				
				try(Session session = HibernateManager.openSession()){
					
					eteams = QueryFactory.buildQuery(session, EmployeeTeam.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
					
					es = QueryFactory.buildQuery(session, Employee.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
							new Object[] {"endEmploymentDate", null}).list(); //$NON-NLS-1$
							
					
					List<ConfigurableModel> models = DataentryHibernateManager.getConfigurableModels(session);
					models.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(),  b.getName().toLowerCase()));
					modelList.addAll(models);
					modelList.add(dm);
					
					profiles.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
					
					if (ctpackage != null && ctpackage.getUuid() != null) {
						init = session.get(IncidentCtPackage.class, ctpackage.getUuid());
						if (init.getConfigurableModel() != null) init.getConfigurableModel().getUuid();
						if (init.getCtProfile() != null) init.getCtProfile().getUuid();
					}else if (ctpackage != null) {
						init = ctpackage;
						if (init.getConfigurableModel() != null) {
							init.setConfigurableModel(session.get(ConfigurableModel.class, init.getConfigurableModel().getUuid()));
							init.getConfigurableModel().getUuid();
						}
						if (init.getCtProfile() != null) {
							init.setCtProfile(session.get(CyberTrackerPropertiesProfile.class, init.getCtProfile().getUuid()));
							init.getCtProfile().getUuid();
						}
					}
					init.getMetadataValues().forEach(e->{
						e.getMetadataKey();
						if (e.getUuidList() != null) {
							e.getUuidList().forEach(md->md.getUuidValue());
						}
					});
				}
				eteams.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
				es.sort((a,b)->Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)));
				
				profiles.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));

				List<Object> observerOptions = new ArrayList<>();
				if (!eteams.isEmpty()) {
					observerOptions.add(Messages.CtIncidentPackageConfigurator_Teams);
					observerOptions.addAll(eteams);
					observerOptions.add(Messages.CtIncidentPackageConfigurator_Employees);
				}
				observerOptions.addAll(es);
				
				List<Object> checked = new ArrayList<>();
				MetadataFieldValue mv = findMetadataValue(IncidentMetadataField.MEMBERS, init);
				if (mv != null && mv.getUuidList() != null) {
					Set<UUID> uuids = mv.getUuidList().stream().map(z->z.getUuidValue()).collect(Collectors.toSet());
					for (EmployeeTeam e : eteams) {
						if (uuids.contains(e.getUuid())) checked.add(e);
					}
					for (Employee e : es) {
						if (uuids.contains(e.getUuid())) checked.add(e);
					}
				}
				
				if (init.isDataModel()) {
					context.set(ConfigurableModel.class, new ConfigurableModel());
				}else {
					context.set(ConfigurableModel.class, init.getConfigurableModel());
				}
				IncidentCtPackage finit = init;
				
				Display.getDefault().syncExec(()->{
					try {
						isInit = true;
						profileViewer.setInput(profiles);
						modelViewer.setInput(modelList);
						
						if (finit.getConfigurableModel() != null) {
							modelViewer.setSelection(new StructuredSelection(finit.getConfigurableModel()));
						}else {
							modelViewer.setSelection(new StructuredSelection(dm));
						}
						
						if (finit.getCtProfile() != null) {
							profileViewer.setSelection(new StructuredSelection(finit.getCtProfile()));
						}else {
							if (!profiles.isEmpty()) profileViewer.setSelection(new StructuredSelection(profiles.get(0)));
						}
						
						if (lstEmployees !=  null) {
							lstEmployees.setInput(observerOptions);
							lstEmployees.setCheckedElements(checked.toArray());
						}
						validate(false);
					}finally {
						isInit = false;
					}
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}

	@Override
	public Composite createDetails(Composite parent, ICtPackage ctpackage, List<ICtPackagePropertyProvider> properties) {
		Composite all = new Composite(parent, SWT.NONE);
		all.setLayout(new GridLayout());
		((GridLayout)all.getLayout()).marginWidth = 0;
		((GridLayout)all.getLayout()).marginHeight = 0;
		all.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		
		
		try(Session session = HibernateManager.openSession()){
			IncidentCtPackage local = session.get(IncidentCtPackage.class, ((IncidentCtPackage)ctpackage).getUuid());
			if (local == null) return all;
			
			Label header = new Label(all, SWT.NONE);
			header.setText(local.getName());
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			header.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			FontData fd = header.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			fd.setHeight(fd.getHeight() + 1);
			Font f = new Font(parent.getDisplay(), fd);
			header.addListener(SWT.Dispose, e->f.dispose());
			header.setFont(f);
			
			ScrolledComposite scroll = new ScrolledComposite(all, SWT.V_SCROLL | SWT.H_SCROLL );
			scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			scroll.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			Composite inner = new Composite(scroll, SWT.NONE);
			scroll.setContent(inner);
			inner.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			inner.setLayout(new GridLayout());
			((GridLayout)inner.getLayout()).marginWidth = 0;
			((GridLayout)inner.getLayout()).marginHeight = 0;
			
			Label l= new Label(inner, SWT.NONE);
			l.setText(Messages.CtIncidentPackageConfigurator_ConfigurableModelLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l = new Label(inner, SWT.NONE);
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			if (local.getConfigurableModel() != null) {
				l.setText(local.getConfigurableModel().getName());
			}else {
				l.setText( Messages.CtIncidentPackageConfigurator_OriginalDmOption );
			}
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l= new Label(inner, SWT.NONE);
			l.setText(Messages.CtIncidentPackageConfigurator_DeviceSettingsLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l= new Label(inner, SWT.NONE);
			l.setText(local.getCtProfile().getName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			for ( IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
				c.createDetails(inner, local, session);
			}
			
			l= new Label(inner, SWT.NONE);
			l.setText(Messages.CtIncidentPackageConfigurator_DetailsLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			Path p = null;
			try {
				p = local.getLocalFile();
				if (p != null) {
					String fname = p.getFileName().toString();
					String revision = fname.substring(fname.indexOf('.')+1,fname.lastIndexOf('.'));
					int index = revision.indexOf('.');
					String date = revision.substring(index+1);
					revision = revision.substring(0,index);
					DateTimeFormatter sdf = DateTimeFormatter.ofPattern(ICtPackage.PACKAGE_DATE_FORMAT);
					
					Composite temp = new Composite(inner, SWT.NONE);
					temp.setLayout(new GridLayout(2, false));
					temp.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					((GridLayout)(temp.getLayout())).marginWidth = 0;
					((GridLayout)(temp.getLayout())).marginHeight = 0;
					temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					
					l = new Label(temp, SWT.NONE);
					l.setText(Messages.CtIncidentPackageConfigurator_DateLabel);
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText( DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDate.parse(date,sdf)) );
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					
					l = new Label(temp, SWT.NONE);
					l.setText(Messages.CtIncidentPackageConfigurator_VersionLabel);
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText(revision);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}else{
					l= new Label(inner, SWT.NONE);
					l.setText(Messages.CtIncidentPackageConfigurator_NoPackageOp);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}
			}catch (Exception ex) {
				l= new Label(inner, SWT.NONE);
				l.setText(Messages.CtIncidentPackageConfigurator_UnknownOp);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			}
			
			for (ICtPackagePropertyProvider pp : properties) {
				pp.getProperties();
			
				l= new Label(inner, SWT.NONE);
				l.setText(pp.getName());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).verticalIndent = 5;
				
				Composite temp = new Composite(inner, SWT.NONE);
				temp.setLayout(new GridLayout(2, false));
				temp.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				((GridLayout)(temp.getLayout())).marginWidth = 0;
				((GridLayout)(temp.getLayout())).marginHeight = 0;
				temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				for (ICtPackageProperty pprop : pp.getProperties()) {
					l = new Label(temp, SWT.NONE);
					l.setText(pprop.getShortName() + ":"); //$NON-NLS-1$
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText( pprop.getValue(ctpackage) );
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}
			}
			
			
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			scroll.setMinSize(inner.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
		return all;
	}
}
