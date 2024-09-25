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
package org.wcs.smart.cybertracker.survey.ui;

import java.nio.file.Path;
import java.text.Collator;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
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
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageConfigurator;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.properties.CtProfileLabelProvider;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesDialog;
import org.wcs.smart.cybertracker.survey.internal.Messages;
import org.wcs.smart.cybertracker.survey.model.SurveyCtPackage;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Patrol cybertracker UI configuration 
 * 
 * @author Emily
 *
 */
public class CtSurveyPackageConfigurator implements ICtPackageConfigurator {
	
	private static final String LANG_KEY = "LANGUAGE"; //$NON-NLS-1$

	private SurveyCtPackage ctpackage;
	
	private ComboViewer designViewer;
	private ComboViewer profileViewer;
	private List<Text> txtNames;
	
	private List<IPackageUiContribution> contributions = null;
	
	private CyberTrackerPropertiesProfile cmDefaultProfile = null;

	private Consumer<String> onValidate;
	private Consumer<Boolean> onModified;

	private boolean isInit = false;
	
	@Inject
	private IEclipseContext context;
	
	public CtSurveyPackageConfigurator() {
		
		contributions = new ArrayList<>();
		contributions.add(new SurveyMetadataPackageContribution());
		for ( IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
			if (c.getUiController() != null) contributions.add(c.getUiController());
		}
	}
	
	@Override
	public void createGui(Composite parent, ICtPackage ctitem, Consumer<String> onValidate,
			Consumer<Boolean> onModified) {
		contributions.forEach(e->ContextInjectionFactory.inject(e, context));
		
		this.onModified = onModified;
		this.onValidate = onValidate;
		if (!(ctitem instanceof SurveyCtPackage)) throw new IllegalStateException(Messages.CtSurveyPackageConfigurator_InvalidType);
		this.ctpackage = (SurveyCtPackage) ctitem;
	
		CTabFolder tabs = new CTabFolder(parent, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tabs.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		CTabItem mainTab = new CTabItem(tabs, SWT.NONE);
		mainTab.setText(Messages.CtSurveyPackageConfigurator_SettingsTab);
		
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
		
		SmartUiUtils.createHeaderLabel(g,  Messages.CtSurveyPackageConfigurator_ConfigurationHeader);
		
		g = new Composite(g, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label nameLabel = new Label(g, SWT.NONE);
		nameLabel.setText("Package Name(s):");
		nameLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP,false, false));
		
		Composite nameComp = new Composite(g, SWT.NONE);
		nameComp.setLayout(new GridLayout(2, false));		
		nameComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtNames = new ArrayList<>();
		
		for (Language l : SmartDB.getCurrentConservationArea().getLanguages()) {
			Label lbl = new Label(nameComp, SWT.NONE);
			lbl.setText(l.getDisplayName() + ":"); //$NON-NLS-1$
			lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			
			Text txtName = new Text(nameComp, SWT.BORDER);
			txtName.setText( ((NamedItem)ctitem).findName(l));
			txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			txtName.addListener(SWT.Modify, e->validate());
			txtName.setData(LANG_KEY, l);
			txtNames.add(txtName);			
		}
		
		Label modelLabel = new Label(g, SWT.NONE);
		modelLabel.setText(Messages.CtSurveyPackageConfigurator_ModelLabel);
		
		designViewer = new ComboViewer(g, SWT.READ_ONLY);
		designViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)designViewer.getControl().getLayoutData()).widthHint = 100;
		designViewer.setContentProvider(ArrayContentProvider.getInstance());
		designViewer.setLabelProvider(new ConfigurableModelLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof SurveyDesign) {
					return ((SurveyDesign)element).getName();
				}
				return super.getText(element);
			}
		});
		designViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		designViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		designViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				SurveyDesign design = null;
				Object profile = ((IStructuredSelection)designViewer.getSelection()).getFirstElement();
				if (profile instanceof SurveyDesign) {
					design = (SurveyDesign)profile;
				};
				
				if (design == null) {
					context.set(ConfigurableModel.class, new ConfigurableModel());
					context.set(SurveyDesign.class, null);
				}else {
					ConfigurableModel cm = design.getConfigurableModel();
					cmDefaultProfile = getDefaultProfile();
					profileViewer.setSelection(new StructuredSelection(cmDefaultProfile));
					
					context.set(ConfigurableModel.class, cm != null ? cm : new ConfigurableModel());
					context.set(SurveyDesign.class, design);
				}
				
				if (!isInit) validate();
			}
		});

		
		Label lblProfile = new Label(g, SWT.NONE);
		lblProfile.setText(Messages.CtSurveyPackageConfigurator_CtPropertiesLabel);

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
		tiEdit.setToolTipText(Messages.CtSurveyPackageConfigurator_vieweditdevicesettings);
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
				
				ctpackage.setSurveyDesign((SurveyDesign)designViewer.getStructuredSelection().getFirstElement());
				ctpackage.setCtProfile((CyberTrackerPropertiesProfile) profileViewer.getStructuredSelection().getFirstElement());
				
				for (Text txt : txtNames) {
					ctpackage.updateName(getLanguage(txt), txt.getText().strip());
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

	private Language getLanguage(Text txt) {
		return (Language) txt.getData(LANG_KEY);
	}
	
	private void validate() {
		validate(true);
	}
	
	private void validate(boolean isModified) {
		if (isModified) onModified.accept(true);
		
		try {
			for (Text txt : txtNames) {
				if (getLanguage(txt).isDefault() && txt.getText().isBlank()) {
					throw new Exception(MessageFormat.format("A package name is required for the default language ({0})", getLanguage(txt).getDisplayName()));
				}
			}
		
			if (designViewer.getSelection().isEmpty() || !(designViewer.getStructuredSelection().getFirstElement() instanceof SurveyDesign)) {
				throw new Exception(Messages.CtSurveyPackageConfigurator_DesignRequired);
			}
		
			if (profileViewer.getSelection().isEmpty()) {
				throw new Exception(Messages.CtSurveyPackageConfigurator_ProfileRequired);
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
	
	private CyberTrackerPropertiesProfile getDefaultProfile() {
		try (Session session = HibernateManager.openSession()){
			CyberTrackerPropertiesProfile profile = CyberTrackerHibernateManager.getDefaultProfile(session);
			Hibernate.initialize(profile);
			return profile;
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.CtSurveyPackageConfigurator_LoadingError, ex);
			return null;
		}
	}
	
	private void loadData() {		
    	
		Job j = new Job("loading data") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<SurveyDesign> designs = new ArrayList<>();
				List<CyberTrackerPropertiesProfile>  profiles = new ArrayList<>();
				
				SurveyCtPackage init = null;
				try(Session session = HibernateManager.openSession()){
					
					designs.addAll(QueryFactory.buildQuery(session,  SurveyDesign.class, 
							new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
							new Object[] {"state", SurveyDesign.State.ACTIVE}).getResultList()); //$NON-NLS-1$
					
					designs.forEach(e->{
						if (e.getConfigurableModel() != null) e.getConfigurableModel().hashCode();
					});
					profiles.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
					
					if (ctpackage != null && ctpackage.getUuid() != null) {
						init = session.get(SurveyCtPackage.class, ctpackage.getUuid());
						if (init.getSurveyDesign() != null) init.getSurveyDesign().getUuid();
						if (init.getCtProfile() != null) init.getCtProfile().getUuid();
						if (init.getIncidentModel() != null) init.getIncidentModel().getUuid();
					}else if (ctpackage != null) {
						init = ctpackage;
						if (ctpackage.getSurveyDesign() != null) {
							init.setSurveyDesign(session.get(SurveyDesign.class, ctpackage.getSurveyDesign().getUuid()));
							init.getSurveyDesign().getUuid();
						}
						if (ctpackage.getCtProfile() != null) {
							init.setCtProfile(session.get(CyberTrackerPropertiesProfile.class, ctpackage.getCtProfile().getUuid()));
							init.getCtProfile().getUuid();
						}
						if (ctpackage.getIncidentModel() != null) {
							init.setIncidentModel(session.get(ConfigurableModel.class, ctpackage.getIncidentModel().getUuid()));
							init.getIncidentModel().getUuid();
						}
					}
				}
				designs.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
				profiles.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));

				if (init.getSurveyDesign() != null) {
					context.set(ConfigurableModel.class, init.getConfigurableModel());
				}
				SurveyCtPackage finit = init;
				Display.getDefault().syncExec(()->{
					try {
						isInit = true;
						profileViewer.setInput(profiles);
						designViewer.setInput(designs);
						
						//to do fix this incase any of these have been deleted
						SurveyDesign selection = null;
						if (!designs.isEmpty()) designs.get(0);
						designViewer.setSelection(new StructuredSelection());
						if (finit != null && finit.getSurveyDesign() != null) selection = finit.getSurveyDesign();
						
						if (selection != null) designViewer.setSelection(new StructuredSelection(selection));
							
						CyberTrackerPropertiesProfile pselection = null;
						if (!profiles.isEmpty()) pselection = profiles.get(0);
						if (finit != null && finit.getCtProfile() != null) pselection = finit.getCtProfile();
						if (pselection != null) profileViewer.setSelection(new StructuredSelection(pselection));						
					}finally {
						isInit = false;
					}
					validate(false);
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
			SurveyCtPackage local = session.get(SurveyCtPackage.class, ((SurveyCtPackage)ctpackage).getUuid());
			
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
			l.setText(Messages.CtSurveyPackageConfigurator_DesignLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l = new Label(inner, SWT.NONE);
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			if (local.getSurveyDesign() != null) {
				l.setText(local.getSurveyDesign().getName());
			}else {
				l.setText( Messages.CtSurveyPackageConfigurator_UnknownLabel );
			}
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l= new Label(inner, SWT.NONE);
			l.setText(Messages.CtSurveyPackageConfigurator_ProfileLabel);
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
			l.setText(Messages.CtSurveyPackageConfigurator_DetailsLabel);
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
					l.setText(Messages.CtSurveyPackageConfigurator_DateProperty);
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText( DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(LocalDate.parse(date,sdf)) );
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					
					l = new Label(temp, SWT.NONE);
					l.setText(Messages.CtSurveyPackageConfigurator_VersionProperty);
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText(revision);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}else{
					l= new Label(inner, SWT.NONE);
					l.setText(Messages.CtSurveyPackageConfigurator_NoPackageValue);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}
			}catch (Exception ex) {
				l= new Label(inner, SWT.NONE);
				l.setText(Messages.CtSurveyPackageConfigurator_UnknownLabel);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			}
			
			for (ICtPackagePropertyProvider pp : properties) {
			
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
				
				createPropertiesComposite(temp, ctpackage, pp, session);

			}
			
			
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			scroll.setMinSize(inner.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
		return all;
	}
}
