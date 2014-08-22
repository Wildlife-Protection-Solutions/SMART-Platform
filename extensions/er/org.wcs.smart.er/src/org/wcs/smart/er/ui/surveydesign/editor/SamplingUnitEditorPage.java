package org.wcs.smart.er.ui.surveydesign.editor;


import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.ui.samplingunit.wizard.ImportWizard;
import org.wcs.smart.hibernate.HibernateManager;

public class SamplingUnitEditorPage extends EditorPart implements IHyperlinkListener{

	private SurveyDesignEditor editor;
	
	private TableViewer suTable;
	private Form form;
	private Hyperlink btnImport;
	private Hyperlink btnExport;
	private Hyperlink btnAttributes;
	
	public SamplingUnitEditorPage(SurveyDesignEditor editor){
		this.editor = editor;
	}
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);
		form.setText("Sampling Units");
		form.getBody().setLayout(new GridLayout());
		 
		
		Section section = toolkit.createSection(form.getBody(), Section.TWISTIE | Section.EXPANDED | Section.TITLE_BAR);
		section.setText("Sampling Units");
		section.setLayout(new GridLayout());
		section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite suComp = toolkit.createComposite(section, SWT.NONE);
		suComp.setLayout(new GridLayout(2, false));
		suComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		section.setClient(suComp);
		
		createSuTable(suComp);
		
		Composite buttonComp = toolkit.createComposite(suComp);
		buttonComp.setLayout(new GridLayout());
		buttonComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		btnImport = toolkit.createHyperlink(buttonComp, "Import...", SWT.PUSH);
		btnExport = toolkit.createHyperlink(buttonComp, "Export...", SWT.PUSH);
		btnAttributes = toolkit.createHyperlink(buttonComp, "Configure Attributes ...", SWT.PUSH);
		
		btnAttributes.addHyperlinkListener(this);
		btnImport.addHyperlinkListener(this);
		btnExport.addHyperlinkListener(this);
		
		Button btnDelete = new Button(buttonComp, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				//TODO: need to do this in a job so it has it's own session
				Session s = HibernateManager.openSession();
				
				s.beginTransaction();
				try{
					IStructuredSelection selection = (IStructuredSelection) suTable.getSelection();
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object type = (Object) iterator.next();
						if (type instanceof SamplingUnit){
							s.delete(type);
						}
					}
					s.getTransaction().commit();
				}catch (Exception ex){
					//TODO:
					ex.printStackTrace();
				}
				s.close();
				
				SurveyEventHandler.getInstance().fireEvent(EventType.SURVEY_DESIGN_MODIFIED, editor.getSurveyDesign());
				
			}
			
		});
		
		initValues();
		
	}

	public void initValues(){
		SurveyDesign sd = editor.getSurveyDesign();
		Session s = HibernateManager.openSession();
		try{
			sd = (SurveyDesign) s.load(SurveyDesign.class, sd.getUuid());
			form.setText(sd.getName() + ": " + "Sampling Units");
		
			createTableColumns(sd);
			
			List<SamplingUnit> sus = s.createCriteria(SamplingUnit.class).add(Restrictions.eq("surveyDesign", sd)).list();
			suTable.setInput(sus);
			
		}finally{
			s.close();
		}
		
		
	}
	
	@Override
	public void setFocus() {
//		suTable.getControl().setFocus();
	}
	
	private void createSuTable(Composite parent){
		suTable = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		suTable.setContentProvider(ArrayContentProvider.getInstance());

		suTable.getTable().setHeaderVisible(true);
		suTable.getTable().setLinesVisible(true);
		
		suTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
	}
	
	private void createTableColumns(SurveyDesign sd){
		//dispose of existing columns
		for (TableColumn tc : suTable.getTable().getColumns()){
			tc.dispose();
		}
		
		TableViewerColumn stateColumn = new TableViewerColumn(suTable, SWT.NONE);
		stateColumn.setLabelProvider(new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.STATE.name()));
		stateColumn.getColumn().setResizable(true);
		stateColumn.getColumn().setWidth(60);
		stateColumn.getColumn().setText("State");
		
		TableViewerColumn typeColumn = new TableViewerColumn(suTable, SWT.NONE);
		typeColumn.setLabelProvider(new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.TYPE.name()));
		typeColumn.getColumn().setResizable(true);
		typeColumn.getColumn().setWidth(60);
		typeColumn.getColumn().setText("Type");
		
		TableViewerColumn idColumn = new TableViewerColumn(suTable, SWT.NONE);
		idColumn.setLabelProvider(new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.ID.name()));
		idColumn.getColumn().setResizable(true);
		idColumn.getColumn().setWidth(60);
		idColumn.getColumn().setText("ID");
		
		TableViewerColumn bufferColumn = new TableViewerColumn(suTable, SWT.NONE);
		bufferColumn.setLabelProvider(new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.BUFFER.name()));
		bufferColumn.getColumn().setResizable(true);
		bufferColumn.getColumn().setWidth(60);
		bufferColumn.getColumn().setText("Buffer");
		
		TableViewerColumn lengthColumn = new TableViewerColumn(suTable, SWT.NONE);
		lengthColumn.setLabelProvider(new SamplingUnitColumnLabelProvider(SamplingUnitColumnLabelProvider.FixedColumns.LENGTH.name()));
		lengthColumn.getColumn().setResizable(true);
		lengthColumn.getColumn().setWidth(60);
		lengthColumn.getColumn().setText("Length");
		
		for (SurveyDesignSamplingUnitAttribute att : sd.getSamplingUnitAttributes()){
			TableViewerColumn column = new TableViewerColumn(suTable, SWT.NONE);
			column.setLabelProvider(new SamplingUnitColumnLabelProvider(att.getSamplingUnitAttribute().getKeyId()));
			column.getColumn().setResizable(true);
			column.getColumn().setWidth(60);
			column.getColumn().setText(att.getSamplingUnitAttribute().getName());
		}
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
			ImportWizard wizard = new ImportWizard(editor.getSurveyDesign());
			WizardDialog wd = new WizardDialog(getSite().getShell(), wizard);
			wd.open();
		}
	}

}
