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
package org.wcs.smart.cybertracker.patrol.ui;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageConfigurator;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageProperty;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.PatrolCtPackage;
import org.wcs.smart.cybertracker.properties.CtProfileLabelProvider;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesDialog;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

import com.ibm.icu.text.DateFormat;

/**
 * Patrol cybertracker UI configuration 
 * 
 * @author Emily
 *
 */
public class CtPatrolPackageConfigurator implements ICtPackageConfigurator {
	
	private PatrolCtPackage ctpackage;
	
	private ComboViewer modelViewer;
	private ComboViewer profileViewer;
	private Text txtName;
	
	private List<IPackageUiContribution> contributions = null;
	private ConfigurableModel selectedModel = null;
	private CyberTrackerPropertiesProfile cmDefaultProfile = null;

	private Consumer<String> onValidate;
	
	private boolean isInit = false;
	
	@Inject
	private IEclipseContext context;
	
	public CtPatrolPackageConfigurator() {
		contributions = new ArrayList<>();
		contributions.add(new PatrolMetadataPackageContribution());
		for ( IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
			if (c.getUiController() != null) contributions.add(c.getUiController());
		}
	}
	
	@Override
	public void createGui(Composite parent, ICtPackage ctitem, Consumer<String> onValidate) {
		contributions.forEach(e->ContextInjectionFactory.inject(e, context));
		
		this.onValidate = onValidate;
		if (!(ctitem instanceof PatrolCtPackage)) throw new IllegalStateException(Messages.CtPatrolPackageConfigurator_InvalidPackageType);
		this.ctpackage = (PatrolCtPackage) ctitem;
	
		
		CTabFolder tabs = new CTabFolder(parent, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tabs.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		CTabItem mainTab = new CTabItem(tabs, SWT.NONE);
		mainTab.setText(Messages.CtPatrolPackageConfigurator_SettingsLabel);
		
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
		
		Composite g = new Composite(main, SWT.NONE);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite header = new Composite(g, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label headerLabel = new Label(header, SWT.NONE);
		headerLabel.setText(Messages.PatrolCTPackageDialog_PatrolConfigurationLabel);
		WidgetElement.setCSSClass(header, SmartUiUtils.HEADER_CLASS);
		
		g = new Composite(g, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label nameLabel = new Label(g, SWT.NONE);
		nameLabel.setText(Messages.CtPatrolPackageConfigurator_NameLabel);
		
		txtName = new Text(g, SWT.BORDER);
		txtName.setText(ctitem.getName() == null ? (ctitem.getTypeIdentifier() + Messages.CtPatrolPackageConfigurator_DefaultName) : ctitem.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->{ if (!isInit) validate();});
		
		Label modelLabel = new Label(g, SWT.NONE);
		modelLabel.setText(Messages.PatrolCTPackageDialog_CmLbl);
		
		modelViewer = new ComboViewer(g, SWT.READ_ONLY);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)modelViewer.getControl().getLayoutData()).widthHint = 100;
		modelViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelViewer.setLabelProvider(new ConfigurableModelLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DataModelWrapper) {
					return Messages.PatrolCTPackageDialog_DmLbl;
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
		lblProfile.setText(Messages.PatrolCTPackageDialog_CtProfileLbl);

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
		tiEdit.setToolTipText(Messages.CtPatrolPackageConfigurator_viewedittooltip);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.addListener(SWT.Selection, e->{
			Object x = profileViewer.getStructuredSelection().getFirstElement();
			if (!(x instanceof CyberTrackerPropertiesProfile)) return;
			
			Dialog dialog = new CyberTrackerPropertiesDialog(c.getShell(), (CyberTrackerPropertiesProfile)x);
			dialog.open();
		});
		
		
		//main page contributions
		if (contributions != null) {
			for (IPackageUiContribution cc : contributions) {
				if (!cc.isTab()) {
					Composite part = cc.createUi(main, ctpackage, e->validate());
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
					Composite part = cc.createUi(all, ctpackage, e->validate());
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
				Object first = modelViewer.getStructuredSelection().getFirstElement();
				if (first instanceof ConfigurableModel) {
					ctpackage.setConfigurableModel((ConfigurableModel) first);
				}else {
					ctpackage.setConfigurableModel(null);
				}
				ctpackage.setCtProfile((CyberTrackerPropertiesProfile) profileViewer.getStructuredSelection().getFirstElement());
				ctpackage.setName(txtName.getText());
				session.saveOrUpdate(ctpackage);
				session.flush();
				for (IPackageUiContribution cc : contributions) {
					cc.updatePackage(ctpackage);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
	}

	private void validate() {
		
		try {
			if (txtName.getText().isBlank()) {
				throw new Exception(Messages.CtPatrolPackageConfigurator_NameRequired);
			}
		
			if (modelViewer.getSelection().isEmpty()) {
				throw new Exception(Messages.CtPatrolPackageConfigurator_CmRequired);
			}
		
			if (profileViewer.getSelection().isEmpty()) {
				throw new Exception(Messages.CtPatrolPackageConfigurator_ProfileRequired);
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
			SmartPlugIn.displayLog(Messages.PatrolCTPackageDialog_ProfileLoadError, ex);
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
				
				PatrolCtPackage init = null;
				try(Session session = HibernateManager.openSession()){
					modelList.addAll(DataentryHibernateManager.getConfigurableModels(session));
					modelList.add(dm);
					
					profiles.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
					
					if (ctpackage != null && ctpackage.getUuid() != null) {
						init = session.get(PatrolCtPackage.class, ctpackage.getUuid());
						if (init.getConfigurableModel() != null) init.getConfigurableModel().getUuid();
						if (init.getCtProfile() != null) init.getCtProfile().getUuid();
						if (init.getIncidentModel() != null) init.getIncidentModel().getUuid();
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
						if (ctpackage.getIncidentModel() != null) {
							init.setIncidentModel(session.get(ConfigurableModel.class, init.getIncidentModel().getUuid()));
							init.getIncidentModel().getUuid();
						}
					}
				}
				if (init.isDataModel()) {
					context.set(ConfigurableModel.class, new ConfigurableModel());
				}else {
					context.set(ConfigurableModel.class, init.getConfigurableModel());
				}
				PatrolCtPackage finit = init;
				
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
			PatrolCtPackage local = session.get(PatrolCtPackage.class, ((PatrolCtPackage)ctpackage).getUuid());
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
			l.setText(Messages.CtPatrolPackageConfigurator_CmLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l = new Label(inner, SWT.NONE);
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			if (local.getConfigurableModel() != null) {
				l.setText(local.getConfigurableModel().getName());
			}else {
				l.setText( Messages.PatrolCTPackageDialog_DmLbl );
			}
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l= new Label(inner, SWT.NONE);
			l.setText(Messages.CtPatrolPackageConfigurator_ProfileLabel);
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
			l.setText(Messages.CtPatrolPackageConfigurator_DetailsLabel);
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
					SimpleDateFormat sdf = new SimpleDateFormat(ICtPackage.PACKAGE_DATE_FORMAT);
					
					l= new Label(inner, SWT.NONE);
					l.setText(DateFormat.getDateTimeInstance().format( sdf.parse(date)) );
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					l= new Label(inner, SWT.NONE);
					l.setText(revision);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}
			}catch (Exception ex) {
				l= new Label(inner, SWT.NONE);
				l.setText(Messages.CtPatrolPackageConfigurator_Unknown);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			}
			
			for (ICtPackagePropertyProvider pp : properties) {
				pp.getProperties();
			
				l= new Label(inner, SWT.NONE);
				l.setText(pp.getName());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).verticalIndent = 5;
				
				for (ICtPackageProperty pprop : pp.getProperties()) {
					l= new Label(inner, SWT.NONE);
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
