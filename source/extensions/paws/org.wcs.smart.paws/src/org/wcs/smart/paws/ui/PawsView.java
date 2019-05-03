/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.ui.SectionHeader;
import org.wcs.smart.ui.properties.DialogConstants;

import com.ibm.icu.text.Collator;

/**
 * Simple view for listing R scripts and queries
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class PawsView {

	public static final String ID = "org.wcs.smart.paws.view"; //$NON-NLS-1$

	private FormToolkit toolkit;
	
	private Composite content;
	
	private Composite resultsComposite;
	private Composite configComposite;
	
	private TableViewer tblConfigs;
	private TableViewer tblResults;

	/**
	 * Default constructor
	 */
	public PawsView() {
		
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		refresh();
	}

	@PreDestroy
	public void dispose() {		

	}
	
	private void refresh() {
		loadResults.schedule();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());

		
		Composite main = toolkit.createComposite(parent, SWT.BORDER);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;

		Composite headerMain = toolkit.createComposite(main, SWT.NONE);
		headerMain.setLayout(new GridLayout());
		headerMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerMain.getLayout()).marginWidth = 0;
		((GridLayout)headerMain.getLayout()).marginHeight = 0;
		
		SectionHeader header = new SectionHeader(headerMain, SWT.NONE,
				new String[] {"Results", "Configurations"},
				new Listener[] {
						e->{
							((StackLayout)content.getLayout()).topControl = resultsComposite;
							content.layout(true);
						},
						e->{
							((StackLayout)content.getLayout()).topControl = configComposite;
							content.layout(true);
						}
				});
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		content = toolkit.createComposite(main, SWT.NONE);
		content.setLayout(new StackLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		resultsComposite = createResultsPanel(content);
		configComposite = createConfigurationPanel(content);
		
		header.selectPanel(0);
		
		refresh();
	}
	
	
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent){
	}
	
	@Focus
	public void setFocus() {
	}

	private Composite createResultsPanel(Composite parent) {
		Composite part = toolkit.createComposite(parent);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		Composite tpart = toolkit.createComposite(part);
		tpart.setLayout(new TableColumnLayout());
		tpart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblResults = new TableViewer(tpart, SWT.FULL_SELECTION | SWT.MULTI );
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn col = new TableViewerColumn(tblResults, SWT.NONE);
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PawsConfiguration) {
					PawsConfiguration pc = (PawsConfiguration)element;
					return pc.getName();
				}
				return super.getText(element);
			}
		});
		((TableColumnLayout)tpart.getLayout()).setColumnData(col.getColumn(),  new ColumnWeightData(1));
		tblResults.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		return part;
	}
	
	
	private Composite createConfigurationPanel(Composite parent) {
		
		Composite part = toolkit.createComposite(parent);
		part.setLayout(new GridLayout());
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;
		
		Composite tpart = toolkit.createComposite(part);
		tpart.setLayout(new TableColumnLayout());
		tpart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblConfigs = new TableViewer(tpart, SWT.FULL_SELECTION | SWT.MULTI );
		tblConfigs.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn col = new TableViewerColumn(tblConfigs, SWT.NONE);
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PawsConfiguration) {
					PawsConfiguration pc = (PawsConfiguration)element;
					return pc.getName();
				}
				return super.getText(element);
			}
		});
		((TableColumnLayout)tpart.getLayout()).setColumnData(col.getColumn(),  new ColumnWeightData(1));
		tblConfigs.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		return part;
	}
    
	private Job loadResults = new Job("loading PAWS data") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<PawsRun> runs= new ArrayList<>();
			List<PawsConfiguration> configs = new ArrayList<>();
			
			try(Session s = HibernateManager.openSession()){
				runs.addAll(QueryFactory.buildQuery(s, PawsRun.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				configs.addAll(QueryFactory.buildQuery(s, PawsConfiguration.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				
			}
			
			configs.sort((a,b)->Collator.getInstance().compare(a.getName(),  b.getName()));
			runs.sort((a,b)->{
				return a.getRunDate().compareTo(b.getRunDate());
			});
			
			Display.getDefault().syncExec(()->{
				tblConfigs.setInput(configs);
				tblResults.setInput(runs);
			});
			return Status.OK_STATUS;
		}
		
	};
	
	public static class PawsViewWrapper extends DIViewPart<PawsView>{
		public PawsViewWrapper(){
			super(PawsView.class);
		}
	}
	
}