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
package org.wcs.smart.er.ui.surveydesign.editor;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.command.navigation.ZoomCommand;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.selection.SelectCommand;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.factory.CommonFactoryFinder;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SamplingUnitAttributeValue;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.samplingunit.EditSamplingUnitDialog;
import org.wcs.smart.er.ui.samplingunit.SamplingUnitStateDialog;
import org.wcs.smart.er.ui.samplingunit.export.wizard.ExportWizard;
import org.wcs.smart.er.ui.samplingunit.load.wizard.ImportAttributeWizard;
import org.wcs.smart.er.ui.samplingunit.load.wizard.ImportOptionDialog;
import org.wcs.smart.er.ui.samplingunit.load.wizard.ImportWizard;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.SmartUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Sampling unit editor page
 * @author Emily
 *
 */
public class SamplingUnitEditorPage extends SmartMapEditorPart implements IHyperlinkListener {

	private FilterFactory ff = CommonFactoryFinder.getFilterFactory();
	
	private SurveyDesignEditor editor;
	
	private TableViewer suTable;
	private Form form;
	private Hyperlink btnImport;
	private Hyperlink btnExport;
	private Hyperlink btnAttributes;
	
	private LoadDefaultLayersJob loadDefaultLayers;
	
	private SamplingUnitService suService;
	private IViewportModelListener initListener;
	
	private List<Layer> suLayers = null;
		
	private ToolItem editItem;
	private ToolItem stateItem;
	private ToolItem deleteItem;
	private ToolItem clearItem;
	private ToolItem zoomItem;
	
	private SamplingUnitColumnLabelProvider sortColumn = null;
	private int sortDirection = SWT.DOWN;
	
	private ViewerSorter viewerComparator = new ViewerSorter(){
		@Override
		public int compare(Viewer viewer, Object o1, Object o2){
			if (sortColumn == null) return 0;
			if (sortDirection == SWT.DOWN){
				return -sortColumn.compare(o1, o2);
			}else{
				return sortColumn.compare(o1, o2);
			}
			
		}
	};
	
	private Job addLayerJob = new Job(Messages.SamplingUnitEditorPage_addLayerJobName) {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			suService = new SamplingUnitService(editor.getSurveyDesign());
	    	try {
	    		List<IGeoResource> layers = (List<IGeoResource>) suService.resources(monitor);
	    		
	    		AddLayersCommand command = new AddLayersCommand(layers, 0);
	    		getMap().sendCommandSync(command);
	    		
	    		suLayers = command.getLayers();
	    		
	    		initListener = new IViewportModelListener() {
					@Override
					public void changed(ViewportModelEvent event) {
						if (getMap() != null){
							getMap().getViewportModel().removeViewportModelListener(initListener);
							getMap().sendCommandASync(new ZoomExtentCommand());
						}
						
					}
				};
				if (monitor.isCanceled() || getMap() == null){
					return Status.OK_STATUS;
				}
	    		getMap().getViewportModel().addViewportModelListener(initListener);
				
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Messages.SamplingUnitEditorPage_UnknownError, IStatus.ERROR, Messages.SamplingUnitEditorPage_UnknownErrorDescription, e);
			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job(Messages.SamplingUnitEditorPage_RefreshJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (suService != null){
				try {
					suService.refresh(editor.getSurveyDesign(), null);
				} catch (IOException e) {
					EcologicalRecordsPlugIn.log("Error refreshing sampling unit layers", e); //$NON-NLS-1$
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
    };
    
    private Job loadValues = new Job(Messages.SamplingUnitEditorPage_loadingValuesJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			SurveyDesign sd = editor.getSurveyDesign();
			
			List<SamplingUnit> units = null;
			List<SurveyDesignSamplingUnitAttribute> attributes = null;
		
			Session s = HibernateManager.openSession();
			try{
				sd = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
				sd.getName();

				//load attributes
				attributes = new ArrayList<SurveyDesignSamplingUnitAttribute>();
				attributes.addAll(sd.getSamplingUnitAttributes());
				Collections.sort(attributes, new Comparator<SurveyDesignSamplingUnitAttribute>() {
					@Override
					public int compare(SurveyDesignSamplingUnitAttribute arg0,
							SurveyDesignSamplingUnitAttribute arg1) {
						return Collator.getInstance().compare(arg0.getSamplingUnitAttribute().getName(),
								arg1.getSamplingUnitAttribute().getName());
					}
				});
				//ensure fields are loaded
				for(SurveyDesignSamplingUnitAttribute a: attributes){
					a.getSamplingUnitAttribute().getName();
					a.getSamplingUnitAttribute().getKeyId();
					a.getSamplingUnitAttribute().getType();
				}
				
				//load units
				units = s.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", sd)).list(); //$NON-NLS-1$
				for(SamplingUnit u : units){
					for (SamplingUnitAttributeValue v:  u.getAttributes()){
						v.getSamplingUnitAttribute().getKeyId();
					}
				}
				
			}finally{
				s.close();
			}
			
			//update ui
			final List<SamplingUnit> sus = units;
			final String name = sd.getName() + ": " + Messages.SamplingUnitEditorPage_SamplingUnitLabel; //$NON-NLS-1$
			final List<SurveyDesignSamplingUnitAttribute> lattributes = attributes;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					form.setText(name);
					createTableColumns(lattributes);
					suTable.setInput(sus);
				}});
			return Status.OK_STATUS;
		}
		
	};
    
	public SamplingUnitEditorPage(SurveyDesignEditor editor){
		super();
		this.editor = editor;
	}
	
	public void dispose(){
		super.dispose();
		if (loadDefaultLayers != null) {
			loadDefaultLayers.cancel();
			loadDefaultLayers = null;
		}
		addLayerJob.cancel();
		
	    // dispose of patrol service
		if (suService != null){
			CatalogPlugin.getDefault().getLocalCatalog().remove(suService);
			suService.dispose(null);
			suService = null;
		}

		refreshJob.cancel();
		refreshJob = null;
	}
	
	
	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Composite container = toolkit.createComposite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		form = toolkit.createForm(container);
		form.setText(Messages.SamplingUnitEditorPage_FormName);
		form.getBody().setLayout(new GridLayout());
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm sash = new SashForm(form.getBody(), SWT.HORIZONTAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createUnitsSection(sash, toolkit);
		createMapsSection(sash,toolkit);
		
		initValues();
	}

	private void createUnitsSection(Composite parent, FormToolkit toolkit) {
		Composite suComp = toolkit.createComposite(parent, SWT.NONE);
		suComp.setLayout(new GridLayout());
		suComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		ToolBar tb = new ToolBar(suComp, SWT.HORIZONTAL );
		
		editItem = new ToolItem(tb, SWT.PUSH );
		editItem.setToolTipText(Messages.SamplingUnitEditorPage_editTooltip);
		editItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.EDIT_SU_ICON));
		editItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editSuItem();
			}
		});
		
		stateItem = new ToolItem(tb, SWT.PUSH );
		stateItem.setToolTipText(Messages.SamplingUnitEditorPage_editStateTooltip);
		stateItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.CHANGE_STATE_ICON));
		stateItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				editSuState();
			}
		});
		
		deleteItem = new ToolItem(tb, SWT.PUSH );
		deleteItem.setToolTipText(Messages.SamplingUnitEditorPage_deleteTooltip);
		deleteItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.DELETE_ICON));
		deleteItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				deleteSuItems();
			}
		});
		
		new ToolItem(tb, SWT.SEPARATOR);
		
		zoomItem = new ToolItem(tb, SWT.PUSH );
		zoomItem.setToolTipText(Messages.SamplingUnitEditorPage_zoomTooltip);
		zoomItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.ZOOM_SU_ICON));
		zoomItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				zoomToItems();
			}
		});
		
		clearItem = new ToolItem(tb, SWT.PUSH );
		clearItem.setToolTipText(Messages.SamplingUnitEditorPage_clearTooltip);
		clearItem.setImage(EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.CLEAR_SELECTION_ICON));
		clearItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				suTable.setSelection(null);
			}
		});
		
		toolkit.adapt(tb);
		
		createSuTable(suComp);
		
		Composite buttonComp = toolkit.createComposite(suComp);
		buttonComp.setLayout(new GridLayout(3, false));
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 2, 1));
		((GridLayout)buttonComp.getLayout()).marginWidth = 0;
		((GridLayout)buttonComp.getLayout()).marginHeight = 0;
		
		btnImport = toolkit.createHyperlink(buttonComp, Messages.SamplingUnitEditorPage_importButton, SWT.PUSH);
		btnExport = toolkit.createHyperlink(buttonComp, Messages.SamplingUnitEditorPage_exportButton, SWT.PUSH);
		btnAttributes = toolkit.createHyperlink(buttonComp, Messages.SamplingUnitEditorPage_configAttributesButton, SWT.PUSH);
		
		btnAttributes.addHyperlinkListener(this);
		btnImport.addHyperlinkListener(this);
		btnExport.addHyperlinkListener(this);
		
		updateSelection();
	}

	private void createMapsSection(Composite parent, FormToolkit toolkit) {
		Composite mapPart = toolkit.createComposite(parent, SWT.BORDER);
		mapPart.setLayout(new GridLayout(2, false));
		mapPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		super.createPartControl(mapPart);
	}
		
	private void editSuState(){
		if (suTable.getSelection().isEmpty()) return;
		 SamplingUnitStateDialog d = new SamplingUnitStateDialog(getSite().getShell());
		 if (d.open() == SamplingUnitStateDialog.OK){
			 Session s = HibernateManager.openSession();
			 s.beginTransaction();
			 try{
				 IStructuredSelection selection = (IStructuredSelection) suTable.getSelection();
				 for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					 Object item = (Object) iterator.next();
					 if (item instanceof SamplingUnit){
						 ((SamplingUnit) item).setState(d.getState());
						 s.saveOrUpdate(item);
					 }
				 }
				 s.getTransaction().commit();
			 }catch (Exception ex) {
				EcologicalRecordsPlugIn.displayLog(Messages.SamplingUnitEditorPage_SaveErrorMsg + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return;
			 }finally{
				 s.close();
			 }					 
			 SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, editor.getSurveyDesign());
		 }	
	}
	
	private void editSuItem(){
		Object x = ((IStructuredSelection)suTable.getSelection()).getFirstElement();
		if (x == null) return;
		if (x instanceof SamplingUnit){
			EditSamplingUnitDialog d = new EditSamplingUnitDialog(getSite().getShell(), (SamplingUnit)x);
			d.open();
		}
	}
	
	private void deleteSuItems(){
		if (suTable.getSelection().isEmpty()) return;
		
		IStructuredSelection selection = (IStructuredSelection) suTable.getSelection();
		final List<SamplingUnit> toDelete = new ArrayList<SamplingUnit>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof SamplingUnit){
				toDelete.add((SamplingUnit)type);
			}
		}
		if (!MessageDialog.openQuestion(getSite().getShell(), Messages.SamplingUnitEditorPage_DeleteDialogTitle, 
				MessageFormat.format(Messages.SamplingUnitEditorPage_DeleteMessage, new Object[]{toDelete.size()}))){
			return ;
		}
		
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite().getShell());
		try {
			pmd.run(true,  false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.SamplingUnitEditorPage_DeleteProgress1, toDelete.size());
					
					Session s = HibernateManager.openSession();
					s.beginTransaction();
					try{
						for (final SamplingUnit delete : toDelete){
							monitor.subTask(MessageFormat.format(Messages.SamplingUnitEditorPage_DeleteProgress2, new Object[]{delete.getId()}));
							try{
								DeleteManager.canDelete(delete, s);
								s.delete(delete);
							}catch (final Exception ex){
								Display.getDefault().syncExec(new Runnable(){
									@Override
									public void run() {
										EcologicalRecordsPlugIn.log(null, ex);
										MessageDialog.openError(pmd.getShell(), Messages.SamplingUnitEditorPage_DeleteDialogTitle, MessageFormat.format(Messages.SamplingUnitEditorPage_ErrorDeletingSu, new Object[]{delete.getId()}) + "\n\n" + ex.getMessage()); //$NON-NLS-1$
									}
									
								});
							}
							monitor.worked(1);
						}
						s.getTransaction().commit();
					}catch (Exception ex){
						EcologicalRecordsPlugIn.log(Messages.SamplingUnitEditorPage_ErrorDeletingSu2 + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
						return;
					}finally{
						s.close();
					}
					
					//fire events
					SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, editor.getSurveyDesign());
				}
			});
		} catch (Exception ex) {
			EcologicalRecordsPlugIn.displayLog(Messages.SamplingUnitEditorPage_ErrorDeletingSu2 + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
	}
	
	private void zoomToItems(){
		if (suTable.getSelection().isEmpty()) return;
		
		Envelope env = null;
		IStructuredSelection selection = (IStructuredSelection) suTable.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			if (type instanceof SamplingUnit){
				if (env == null){
					env = ((SamplingUnit) type).getGeometry().getEnvelopeInternal();
				}else{
					env.expandToInclude(((SamplingUnit) type).getGeometry().getEnvelopeInternal());
				}
			}
		}
		
		ZoomCommand z = new ZoomCommand(env);
		getMap().sendCommandASync(z);
	}
	
	
	
	/**
	 * Loads the sampling units from the database and updates 
	 * associated table.
	 */
	public void initValues(){
		loadValues.schedule();
		addLayers();
	}
	
	@Override
	public void setFocus() {
		suTable.getControl().setFocus();
	}
	
	private void createSuTable(Composite parent){
		suTable = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		suTable.setContentProvider(ArrayContentProvider.getInstance());

		suTable.getTable().setHeaderVisible(true);
		suTable.getTable().setLinesVisible(true);
		
		suTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		suTable.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateSelection();
			}
		});
		
	}
		
	private void createTableColumns(List<SurveyDesignSamplingUnitAttribute> attributes){
		//dispose of existing columns
		for (TableColumn tc : suTable.getTable().getColumns()){
			tc.dispose();
		}
		sortColumn = null;
		suTable.getTable().setSortColumn(null);
		suTable.getTable().setSortDirection(SWT.NONE);
		
		TableViewerColumn stateColumn = new TableViewerColumn(suTable, SWT.NONE);
		SamplingUnitColumnLabelProvider labelProvider = new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.STATE.name());
		stateColumn.setLabelProvider(labelProvider);
		stateColumn.getColumn().setResizable(true);
		stateColumn.getColumn().setWidth(60);
		stateColumn.getColumn().setText(Messages.SamplingUnitEditorPage_StateColumnName);
		stateColumn.getColumn().addSelectionListener(createTableColumnSelectionListener(labelProvider, stateColumn));
		
		TableViewerColumn typeColumn = new TableViewerColumn(suTable, SWT.NONE);
		labelProvider = new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.TYPE.name());
		typeColumn.setLabelProvider(labelProvider);
		typeColumn.getColumn().setResizable(true);
		typeColumn.getColumn().setWidth(100);
		typeColumn.getColumn().setText(Messages.SamplingUnitEditorPage_TypeColumnName);
		typeColumn.getColumn().addSelectionListener(createTableColumnSelectionListener(labelProvider, typeColumn));
		
		TableViewerColumn idColumn = new TableViewerColumn(suTable, SWT.NONE);
		labelProvider = new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.ID.name());
		idColumn.setLabelProvider(labelProvider);
		idColumn.getColumn().setResizable(true);
		idColumn.getColumn().setWidth(60);
		idColumn.getColumn().setText(Messages.SamplingUnitEditorPage_IdColumnName);
		idColumn.getColumn().addSelectionListener(createTableColumnSelectionListener(labelProvider, idColumn));
		
		TableViewerColumn bufferColumn = new TableViewerColumn(suTable, SWT.NONE);
		labelProvider = new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.BUFFER.name());
		bufferColumn.setLabelProvider(labelProvider);
		bufferColumn.getColumn().setResizable(true);
		bufferColumn.getColumn().setWidth(60);
		bufferColumn.getColumn().setText(Messages.SamplingUnitEditorPage_BufferColumnName);
		bufferColumn.getColumn().addSelectionListener(createTableColumnSelectionListener(labelProvider, bufferColumn));
		
		TableViewerColumn lengthColumn = new TableViewerColumn(suTable, SWT.NONE);
		labelProvider = new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.LENGTH.name());
		lengthColumn.setLabelProvider(labelProvider);
		lengthColumn.getColumn().setResizable(true);
		lengthColumn.getColumn().setWidth(100);
		lengthColumn.getColumn().setText(Messages.SamplingUnitEditorPage_LengthColumnName);
		lengthColumn.getColumn().addSelectionListener(createTableColumnSelectionListener(labelProvider, lengthColumn));
		
		GC gc = new GC(suTable.getTable());
		try{
			for (SurveyDesignSamplingUnitAttribute att : attributes){
				TableViewerColumn column = new TableViewerColumn(suTable, SWT.NONE);
				labelProvider = new SamplingUnitColumnLabelProvider(att.getSamplingUnitAttribute().getKeyId(), att.getSamplingUnitAttribute().getType());
				column.setLabelProvider(labelProvider);
				column.getColumn().setResizable(true);
				column.getColumn().setWidth(gc.stringExtent(att.getSamplingUnitAttribute().getName()).x + 20);
				column.getColumn().setText(att.getSamplingUnitAttribute().getName());
				column.getColumn().addSelectionListener(createTableColumnSelectionListener(labelProvider, column));
			}
		}finally{
			gc.dispose();
		}
		
		MenuManager mgr = new MenuManager();
		Menu menu = mgr.createContextMenu(suTable.getTable());
		suTable.getTable().setMenu(menu);
		mgr.add(new Action(Messages.SamplingUnitEditorPage_EditMenuLabel, EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.EDIT_SU_ICON)) {
			@Override
			public void run(){
				editSuItem();
			}
		});
		
		mgr.add(new Action(Messages.SamplingUnitEditorPage_ZoomToMenuLabel, EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.ZOOM_SU_ICON)) {
			@Override
			public void run(){
				zoomToItems();
			}
		});
		
		mgr.add(new Action(Messages.SamplingUnitEditorPage_DeleteMenuLabel, EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.DELETE_ICON)) {
			@Override
			public void run(){
				deleteSuItems();
			}
		});
	}


	/*
	 * Creates a selection listener for table columns
	 */
	private SelectionListener createTableColumnSelectionListener(final SamplingUnitColumnLabelProvider labelProvider, final TableViewerColumn column){
		return new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (sortColumn == labelProvider){
					if (sortDirection == SWT.UP){
						sortDirection = SWT.DOWN;
					}else{
						sortDirection = SWT.UP;
					}
				}
				suTable.getTable().setSortColumn(column.getColumn());
				suTable.getTable().setSortDirection(sortDirection);
				sortColumn = labelProvider;
				suTable.setSorter(viewerComparator);		
				suTable.refresh();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		};
	}
	
	@Override
	public void linkEntered(HyperlinkEvent e) {
	}


	@Override
	public void linkExited(HyperlinkEvent e) {
	}


	@Override
	public void linkActivated(HyperlinkEvent e) {
		if (e.widget == btnAttributes){
			SurveyDesignSamplingUnitAttributeDialog d = new SurveyDesignSamplingUnitAttributeDialog(getSite().getShell(), editor.getSurveyDesign());
			d.open();
		}else if (e.widget == btnImport){
			
			ImportOptionDialog dialog = new ImportOptionDialog(getSite().getShell());
			if (dialog.open() != ImportOptionDialog.OK){
				return;
			}
			
			IWizard wizard = null;
			if (dialog.importNew()){
				wizard = new ImportWizard(editor.getSurveyDesign());
			}else{
				wizard = new ImportAttributeWizard(editor.getSurveyDesign());
			}
			WizardDialog wd = new WizardDialog(getSite().getShell(), wizard);
			wd.open();
			
		}else if (e.widget == btnExport){
			ExportWizard wizard = new ExportWizard(editor.getSurveyDesign());
			WizardDialog wd = new WizardDialog(getSite().getShell(), wizard);
			wd.open();
		}
	}

	private void addLayers(){
		
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayers.schedule();
		
		if (suService == null){
			addLayerJob.schedule();
		}else{
			refreshJob.schedule();
		}
	}
	
	private void updateSelection(){
		boolean isEmpty = suTable.getSelection().isEmpty();
		
		editItem.setEnabled(!isEmpty);
		stateItem.setEnabled(!isEmpty);
		deleteItem.setEnabled(!isEmpty);
		clearItem.setEnabled(!isEmpty);
		zoomItem.setEnabled(!isEmpty);
		
		if (suLayers == null) return;
		
		//update map language
		IStructuredSelection selection = (IStructuredSelection) suTable.getSelection();
		List<Filter> allFilters = new ArrayList<Filter>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object su = (Object) iterator.next();
			if (su instanceof SamplingUnit){
				SamplingUnit s = (SamplingUnit)su;
				allFilters.add(ff.equals(ff.property("fid"),  //$NON-NLS-1$
							ff.literal(s.getId() + "." + SmartUtils.encodeHex(s.getUuid())))); //$NON-NLS-1$
			}
		}
		for (Layer l : suLayers){
			if (allFilters.size() == 0){
				SelectCommand sc = new SelectCommand(l, Filter.EXCLUDE);
				getMap().sendCommandASync(sc);
			}else{
				SelectCommand sc = new SelectCommand(l, ff.or(allFilters));
				getMap().sendCommandASync(sc);
			}
		}
	}


	@Override
	public MultiPageEditorPart getParentEditor() {
		return editor;
	}

}
