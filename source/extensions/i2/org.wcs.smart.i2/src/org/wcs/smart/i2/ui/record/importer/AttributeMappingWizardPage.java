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
package org.wcs.smart.i2.ui.record.importer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
import org.wcs.smart.i2.record.importer.RecordImportConfig;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Wizard page for collecting attribute to column mappings.
 * 
 * @author Emily
 *
 */
public class AttributeMappingWizardPage extends WizardPage implements ISelectionChangedListener{

	private static final String ATTRIBUTE_DATA_KEY = "ATTRIBUTE"; //$NON-NLS-1$

	public static final String FILE_PAGE = "org.wcs.smart.i2.ui.record.importer.mapping"; //$NON-NLS-1$
		
	private Composite mappingPanel;
	private ScrolledComposite sc ;
	private List<ComboViewer> mappings = null;
	
	private IntelEntityType lastType = null;
	private Path lastFile = null;
	
	protected AttributeMappingWizardPage() {
		super(FILE_PAGE);
	}

	@Override
	public boolean isPageComplete() {
		setErrorMessage(null);
		if (mappings == null) return super.isPageComplete();
		for (ComboViewer c : mappings){
			if (!c.getSelection().isEmpty()){
				Object x = ((StructuredSelection)c.getSelection()).getFirstElement();
				if (x.equals(RecordImportConfig.Column.TITLE)){
					return super.isPageComplete();
				}
			}
		}
		setErrorMessage(Messages.AttributeMappingWizardPage1_TitleMappingRequired);
		return false;
	}

	@Override
	public void createControl(Composite parent) {
		sc = new ScrolledComposite(parent, SWT.V_SCROLL);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		mappingPanel = new Composite(sc, SWT.NONE);
		mappingPanel.setLayout(new GridLayout(2, false));
		
		sc.setContent(mappingPanel);
		sc.setMinSize(mappingPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setTitle(Messages.AttributeMappingWizardPage1_Title);
		setMessage(Messages.AttributeMappingWizardPage1_Message);
		setControl(sc);
	}
	
	public void initPage(){
		j.schedule();
	}
	
	public void updateConfiguration(RecordImportConfig configuration){
		configuration.clearColumnMappings();
		
		for (ComboViewer viewer : mappings){
			int index = (Integer) viewer.getData(ATTRIBUTE_DATA_KEY);
			
			if (!viewer.getSelection().isEmpty()){
				Object selection = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
				
				if (selection instanceof IntelRecordSourceAttribute){
					configuration.addColumnMapping(index,  (IntelRecordSourceAttribute)selection);
				}else if (selection instanceof RecordImportConfig.Column){
					configuration.addColumnMapping(index,  (RecordImportConfig.Column)selection);
				}else if (selection instanceof IntelAttribute){
					configuration.addColumnMapping(index, (IntelAttribute)selection);
				}
				
			}
		}
	}
	
	Job j = new Job(Messages.AttributeMappingWizardPage1_loadingAttributeJobName){

		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (lastType != null && lastFile != null){
				if (lastFile.equals(((ImportRecordWizard)getWizard()).getImportConfiguration().getFile())){
					//same type & same file; lets not update details 
					return Status.OK_STATUS;
				}
			}
			HashSet<IntelRecordSourceAttribute> recordattributes = new HashSet<IntelRecordSourceAttribute>();
			
			
			Session s = HibernateManager.openSession();
			try{
				List<IntelRecordSourceAttribute> atts = s.createCriteria(IntelRecordSourceAttribute.class, "rs") //$NON-NLS-1$
						.createAlias("rs.source", "src") //$NON-NLS-1$ //$NON-NLS-2$
						.add(Restrictions.eq("src.conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
						.list();
				recordattributes.addAll(atts);
			}finally{
				s.close();
			}
			
			//create the column headers from the csv file
			Path file = ((ImportRecordWizard)getWizard()).getImportConfiguration().getFile();
			lastFile = file;
			String[] headers = null;
			try(CSVReader reader = new CSVReader(Files.newBufferedReader(file))){
				headers = reader.readNext();
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.AttributeMappingWizardPage1_FileReadError, file.toString(), ex.getMessage()), ex);
				return Status.OK_STATUS;
			}
			final String[] fheaders = headers;
			
			List<Object> columnOptions= new ArrayList<Object>();
			HashSet<IntelAttribute> attributes = new HashSet<IntelAttribute>();
			
			for (IntelRecordSourceAttribute x : recordattributes){
				columnOptions.add(x);
				if (x.getAttribute() != null) attributes.add(x.getAttribute());
				if (x.getAttribute() != null && x.getAttribute().getType() == AttributeType.POSITION){
					
					String name = x.getName();
					if (name == null){
						if (x.getAttribute() != null) name = x.getAttribute().getName();
						if (x.getEntityType() != null) name = x.getEntityType().getName();
					}
					x.setName(name + Messages.AttributeMappingWizardPage1_XpositionValue);
					
					IntelAttribute y = new IntelAttribute();
					y.setKeyId(x.getAttribute().getKeyId());
					y.setType(x.getAttribute().getType());
					
					IntelRecordSourceAttribute src = new IntelRecordSourceAttribute();
					src.setAttribute(y);
					src.setSource(x.getSource());
					src.setName(x.getName());
					src.setName(name + Messages.AttributeMappingWizardPage1_YPositionValue);
					columnOptions.add(src);
				}
			}
			columnOptions.sort((a,b)-> {
				IntelRecordSourceAttribute aa = (IntelRecordSourceAttribute)a;
				IntelRecordSourceAttribute bb = (IntelRecordSourceAttribute)b;
				if (aa.getSource().equals(bb.getSource())){
					String v1 = aa.getName();
					String v2 = bb.getName();
					if (v1 == null){
						if (aa.getAttribute() != null) v1 = aa.getAttribute().getName();
						if (aa.getEntityType() != null) v1 = aa.getEntityType().getName();
					}
					if (v2 == null){
						if (bb.getAttribute() != null) v2 = bb.getAttribute().getName();
						if (bb.getEntityType() != null) v2 = bb.getEntityType().getName();
					}
					return Collator.getInstance().compare(v1, v2);
				}else{
					return Collator.getInstance().compare(aa.getSource().getName(), bb.getSource().getName());		
				}
			});
			
			
			columnOptions.add(0, ""); //$NON-NLS-1$
			columnOptions.add(1, Messages.AttributeMappingWizardPage1_RecordAttributes);
			columnOptions.add(2, RecordImportConfig.Column.TITLE);
			columnOptions.add(3, RecordImportConfig.Column.SOURCE);
			columnOptions.add(4, RecordImportConfig.Column.NARRATIVE);
			columnOptions.add(5, RecordImportConfig.Column.SCRATCHPAD);
			columnOptions.add(6, Messages.AttributeMappingWizardPage1_IntelAttribute);
			columnOptions.add(7, Messages.AttributeMappingWizardPage1_SourceSpecificAttributes);
			columnOptions.addAll(7, attributes);
			
			Display.getDefault().syncExec(()->{
				for (Control c : mappingPanel.getChildren()){
					c.dispose();
				}
				mappings = new ArrayList<>();
				
				for (int i = 0; i < fheaders.length; i ++){
	
					Label l = new Label(mappingPanel, SWT.NONE);
					l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
					l.setText(fheaders[i] + ":"); //$NON-NLS-1$
					
					ComboViewer viewer = new ComboViewer(mappingPanel, SWT.READ_ONLY | SWT.DROP_DOWN);
					viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					viewer.setContentProvider(ArrayContentProvider.getInstance());
					viewer.setLabelProvider(new LabelProvider(){
							@Override
							public String getText(Object element){
								if (element instanceof RecordImportConfig.Column){
									return ((RecordImportConfig.Column)element).getGuiName();
								}else if (element instanceof IntelAttribute){
									return ((IntelAttribute) element).getName();
								}else if (element instanceof IntelRecordSourceAttribute){
									IntelRecordSourceAttribute e = (IntelRecordSourceAttribute)element;
									String name = e.getName();
									
									if (e.getAttribute() != null){
										if (name == null){
											name = e.getAttribute().getName();
										}
									}else if (e.getEntityType() != null){
										if (name == null){
											name = e.getEntityType().getName();
										}
										name += " (" + e.getEntityType().getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
									}
									
									return e.getSource().getName() + ": " + name; //$NON-NLS-1$
								}
								return super.getText(element);
							}
					});
					viewer.setInput(columnOptions);
					viewer.setData(ATTRIBUTE_DATA_KEY, i);
					viewer.addSelectionChangedListener(AttributeMappingWizardPage.this);
					mappings.add(viewer);
					
				}
				mappingPanel.layout(true, true);
				sc.setMinSize(mappingPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				getWizard().getContainer().updateButtons();
			});
			return Status.OK_STATUS;
		}
		
	};

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		getWizard().getContainer().updateButtons();
	}
}
