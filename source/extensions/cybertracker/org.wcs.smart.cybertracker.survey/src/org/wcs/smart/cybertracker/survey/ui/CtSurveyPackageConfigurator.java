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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
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
import org.wcs.smart.cybertracker.properties.CtProfileLabelProvider;
import org.wcs.smart.cybertracker.survey.model.SurveyCtPackage;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

import com.ibm.icu.text.DateFormat;

/**
 * Patrol cybertracker UI configuration 
 * 
 * @author Emily
 *
 */
public class CtSurveyPackageConfigurator implements ICtPackageConfigurator {
	
	private SurveyCtPackage ctpackage;
	
	private ComboViewer designViewer;
	private ComboViewer profileViewer;
	private Text txtName;
	
	private List<IPackageUiContribution> contributions = null;
	private ConfigurableModel selectedModel = null;
	private CyberTrackerPropertiesProfile cmDefaultProfile = null;

	private Consumer<String> onValidate;
	
	private boolean isInit = false;
	
	@Inject
	private IEclipseContext context;
	
	public CtSurveyPackageConfigurator() {
		contributions = new ArrayList<>();
		for ( IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
			if (c.getUiController() != null) contributions.add(c.getUiController());
		}
	}
	
	@Override
	public void createGui(Composite parent, ICtPackage ctitem, Consumer<String> onValidate) {
		contributions.forEach(e->ContextInjectionFactory.inject(e, context));
		
		this.onValidate = onValidate;
		if (!(ctitem instanceof SurveyCtPackage)) throw new IllegalStateException("Incorrect package type for cybertracker patrol editor.");
		this.ctpackage = (SurveyCtPackage) ctitem;
	
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Group g = new Group(main, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		g.setText("Survey Configuration");
		
		Label nameLabel = new Label(g, SWT.NONE);
		nameLabel.setText("Package Name:");
		
		txtName = new Text(g, SWT.BORDER);
		txtName.setText(ctitem.getName() == null ? (ctitem.getTypeIdentifier() + " Package") : ctitem.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->{ if (!isInit) validate();});
		
		Label modelLabel = new Label(g, SWT.NONE);
		modelLabel.setText("Survey Design");
		
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
				selectedModel = null;
				Object profile = ((IStructuredSelection)designViewer.getSelection()).getFirstElement();
				if (profile instanceof ConfigurableModel) {
					selectedModel = (ConfigurableModel)profile;
					cmDefaultProfile = getAssciatedProfile(selectedModel);
					profileViewer.setSelection(new StructuredSelection(cmDefaultProfile));
				}else if (profile instanceof DataModelWrapper) {
					selectedModel = null;					
				}
				{ if (!isInit) validate();}
			}
		});

		
		Label lblProfile = new Label(g, SWT.NONE);
		lblProfile.setText("CyberTracker Properties");

		profileViewer = new ComboViewer(g, SWT.READ_ONLY);
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
		
		if (contributions != null) {
			for (IPackageUiContribution cc : contributions) {
				Composite part = cc.createUi(main, ctpackage, e->validate());
				if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
		}
		
		loadData();
		
	}

	@Override
	public void save() throws Exception {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				ctpackage.setSurveyDesign((SurveyDesign)designViewer.getStructuredSelection().getFirstElement());
				ctpackage.setCtProfile((CyberTrackerPropertiesProfile) profileViewer.getStructuredSelection().getFirstElement());
				ctpackage.setName(txtName.getText());
				for (IPackageUiContribution cc : contributions) {
					cc.updatePackage(ctpackage);
				}
				session.saveOrUpdate(ctpackage);
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
				throw new Exception("A package name is required");
			}
		
			if (designViewer.getSelection().isEmpty() || !(designViewer.getStructuredSelection().getFirstElement() instanceof SurveyDesign)) {
				throw new Exception("A survey design must be selected");
			}
		
			if (profileViewer.getSelection().isEmpty()) {
				throw new Exception("A profile must be selected");
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
			SmartPlugIn.displayLog("Error loading cybertracker profiles.", ex);
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
					
					profiles.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
					
					if (ctpackage != null && ctpackage.getUuid() != null) {
						init = session.get(SurveyCtPackage.class, ctpackage.getUuid());
						if (init.getSurveyDesign() != null) init.getSurveyDesign().getUuid();
						if (init.getCtProfile() != null) init.getCtProfile().getUuid();
						if (init.getIncidentModel() != null) init.getIncidentModel().getUuid();
					}
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
			l.setText("Survey Design");
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l = new Label(inner, SWT.NONE);
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			if (local.getSurveyDesign() != null) {
				l.setText(local.getSurveyDesign().getName());
			}else {
				l.setText( "Unknown" );
			}
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l= new Label(inner, SWT.NONE);
			l.setText("Cybertracker Profile");
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
			l.setText("Local Package Details");
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
				l.setText("Unknown");
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
