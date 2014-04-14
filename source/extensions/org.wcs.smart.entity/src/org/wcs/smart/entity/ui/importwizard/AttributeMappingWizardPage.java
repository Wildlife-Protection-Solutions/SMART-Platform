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
package org.wcs.smart.entity.ui.importwizard;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.entity.EntityCsvImporter;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.ProjectionLabelProvider;

/**
 * Wizard page for mapping csv columns to entity attributes.
 * 
 * @author Emily
 *
 */
public class AttributeMappingWizardPage extends WizardPage {

	private EntityCsvImporter importer;
	
	private Button btnSkipHeader;
	
	private ComboViewer idViewer;
	private ComboViewer statusViewer;
	private ComboViewer xViewer;
	private ComboViewer yViewer;
	private ComboViewer projViewer;
	
	private HashMap<EntityAttribute, ComboViewer> viewers;
	
	private ComboViewer cmbColumnSelectorDateFormat;
	private ControlDecoration cdDateFormat ;
	private ControlDecoration cdId;
	private ControlDecoration cdX;
	private ControlDecoration cdY;
	private ControlDecoration cdProj;
	
	private ISelectionChangedListener changeListener = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			validate();
		}
	};
	
	// date formats
	private final static String[] DATE_FORMATS = new String[]{((SimpleDateFormat) SimpleDateFormat.getDateInstance(DateFormat.SHORT)).toLocalizedPattern(),
			((SimpleDateFormat) SimpleDateFormat.getDateInstance(DateFormat.MEDIUM)).toLocalizedPattern(),
			((SimpleDateFormat) SimpleDateFormat.getDateInstance(DateFormat.LONG)).toLocalizedPattern(),
			"d/M/y","d-M-y","M/d/y","M-d-y","y/M/d","y-M-d"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	
	
	protected AttributeMappingWizardPage(EntityCsvImporter importer) {
		super("AttributeMapper"); //$NON-NLS-1$
		this.importer = importer;
	}

	@Override
	public void createControl(Composite parent) {
		setTitle(Messages.AttributeMappingWizardPage_PageTitle);
		setMessage(Messages.AttributeMappingWizardPage_PageMessage);
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Group groupA = new Group(c, SWT.NONE);
		groupA.setLayout(new GridLayout(2, false));
		groupA.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		groupA.setText(Messages.AttributeMappingWizardPage_ConfigurationSectionName);

		Label l = new Label(groupA, SWT.NONE);
		l.setText(Messages.AttributeMappingWizardPage_SkipHeadersLabel);
		
		btnSkipHeader = new Button(groupA, SWT.CHECK);
		btnSkipHeader.setToolTipText(Messages.AttributeMappingWizardPage_SkipHeadersTooltip);
		btnSkipHeader.setSelection(true);
		btnSkipHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//if we have a date attribute we need a date format
		boolean hasDate = false;
		for (EntityAttribute ea : importer.getEntityType().getAttributes()){
			if (ea.getDmAttribute().getType() == AttributeType.DATE){
				hasDate = true;
				break;
			}
		}
		if (hasDate){
			l = new Label(groupA, SWT.NONE);
			l.setText(Messages.AttributeMappingWizardPage_DateFormatLabel);
			l.setToolTipText(Messages.AttributeMappingWizardPage_DateFormatTooltip);
			cmbColumnSelectorDateFormat = new ComboViewer(groupA, SWT.DROP_DOWN );
			cdDateFormat = createDecoration(cmbColumnSelectorDateFormat.getControl());
			cmbColumnSelectorDateFormat.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbColumnSelectorDateFormat.setContentProvider(ArrayContentProvider.getInstance());
			cmbColumnSelectorDateFormat.setInput(DATE_FORMATS);
			cmbColumnSelectorDateFormat.getCombo().setText(Messages.AttributeMappingWizardPage_SelectDateFormat);
			cmbColumnSelectorDateFormat.getCombo().addListener(SWT.Modify, new Listener() {
				@Override
				public void handleEvent(Event event) {
					validate();
				}
			});
		}
		
		
		Group groupB = new Group(c, SWT.NONE);
		groupB.setLayout(new GridLayout(1, false));
		groupB.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		groupB.setText(Messages.AttributeMappingWizardPage_MappingGroupLabel);
		
		ScrolledComposite scroll = new ScrolledComposite(groupB, SWT.V_SCROLL | SWT.H_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)scroll.getLayoutData()).heightHint = 200;
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setShowFocusedControl(true);
		
		Composite inner = new Composite(scroll, SWT.NONE);
		scroll.setContent(inner);
		inner.setLayout(new GridLayout(2, false));

		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.AttributeMappingWizardPage_IDLabel);
		idViewer = createColumnViewer(inner);
		idViewer.addSelectionChangedListener(changeListener);
		cdId = createDecoration(idViewer.getControl());
		
		l = new Label(inner, SWT.NONE);
		l.setText(Messages.AttributeMappingWizardPage_StatusLabel);
		statusViewer = createColumnViewer(inner);
		
		if (importer.getEntityType().getType() == EntityType.Type.FIXED){
			l = new Label(inner, SWT.NONE);
			l.setText(Messages.AttributeMappingWizardPage_XLabel);
			xViewer = createColumnViewer(inner);
			xViewer.addSelectionChangedListener(changeListener);
			cdX = createDecoration(xViewer.getControl());
			
			l = new Label(inner, SWT.NONE);
			l.setText(Messages.AttributeMappingWizardPage_YLabel);
			yViewer = createColumnViewer(inner);
			yViewer.addSelectionChangedListener(changeListener);
			cdY = createDecoration(yViewer.getControl());
			
			l = new Label(inner, SWT.NONE);
			l.setText(Messages.AttributeMappingWizardPage_ProjectionLabel);
			projViewer = new ComboViewer(inner, SWT.DROP_DOWN | SWT.READ_ONLY);
			projViewer.setContentProvider(ArrayContentProvider.getInstance());
			projViewer.setLabelProvider(ProjectionLabelProvider.getInstance());
			projViewer.addSelectionChangedListener(changeListener);
			projViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cdProj = createDecoration(projViewer.getControl());
		}
		
		viewers = new HashMap<EntityAttribute, ComboViewer>();
		for (EntityAttribute ea : importer.getEntityType().getAttributes()){
			l = new Label(inner, SWT.NONE);
			l.setText(ea.getName() + ":"); //$NON-NLS-1$
			ComboViewer viewer = createColumnViewer(inner);
			viewers.put(ea, viewer);
			
		}
		
		scroll.setMinSize(inner.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		setControl(c);
	}
	
	private void validate(){
		boolean valid = true;
	
		if (cmbColumnSelectorDateFormat != null){
			String format = getDateFormat();
			if(format.equals("")){ //$NON-NLS-1$
				cdDateFormat.show();
				cdDateFormat.setDescriptionText(Messages.AttributeMappingWizardPage_DateRequired);
				valid = false;
			}else{
				try{
					new SimpleDateFormat(format);
					cdDateFormat.hide();
				}catch(Exception ex){
					cdDateFormat.setDescriptionText(Messages.AttributeMappingWizardPage_InvalidDate + ex.getLocalizedMessage());
					cdDateFormat.show();
					valid = false;
				}
			}
		}
		
		if (xViewer != null){
			if (getIndex(xViewer) == null || getIndex(xViewer) < 0){
				cdX.show();
				cdX.setDescriptionText(Messages.AttributeMappingWizardPage_XRequired);
				valid = false;
			}else{
				cdX.hide();
			}	
		}
		if (yViewer != null){
			if (getIndex(yViewer) == null || getIndex(yViewer) < 0){
				cdY.show();
				cdY.setDescriptionText(Messages.AttributeMappingWizardPage_YRequired);
				valid = false;
			}else{
				cdY.hide();
			}	
		}
		if (projViewer != null){
			if (projViewer.getSelection().isEmpty()){
				cdProj.show();
				cdProj.setDescriptionText(Messages.AttributeMappingWizardPage_ProjectionRequired);
				valid = false;
			}else{
				cdProj.hide();
			}	
		}
		if (getIndex(idViewer) == null || getIndex(idViewer) < 0){
			cdId.show();
			cdId.setDescriptionText(Messages.AttributeMappingWizardPage_IdRequired);
			valid = false;
		}else{
			cdId.hide();
		}
	
		super.setPageComplete(valid);
	}
	

	public String getDateFormat(){
		ISelection selection = cmbColumnSelectorDateFormat.getSelection();
		if (!selection.isEmpty()) {
			   IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
			   String date = (String)structuredSelection.getFirstElement();
			   return date;
		}
		return cmbColumnSelectorDateFormat.getCombo().getText();
	}
	
	public void initFields(Session session) throws Exception{
		if (projViewer != null){
			List<Projection> projs = HibernateManager.getCaProjectionList(session);
			projViewer.setInput(projs);
			projViewer.setSelection(new StructuredSelection(projs.get(0)));
			
		}
		
		//configure column options
		String[] headers = importer.getFileHeaders();
		ArrayList<ViewerContent> data = new ArrayList<ViewerContent>();
		data.add(new ViewerContent("", null));		 //$NON-NLS-1$
		for (int i = 0; i < headers.length; i ++){
			data.add(new ViewerContent(headers[i] + " [" + i + "]", i)); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		//set column options
		idViewer.setInput(data);
		statusViewer.setInput(data);
		if (xViewer != null) xViewer.setInput(data);
		if (yViewer != null) yViewer.setInput(data);
		for (ComboViewer viewer : viewers.values()){
			viewer.setInput(data);
		}
		

		//layout and validate
		((Composite)getControl()).layout(true);
		validate();
	}
	
	private ComboViewer createColumnViewer(Composite c){
		ComboViewer column = new ComboViewer(c, SWT.READ_ONLY | SWT.DROP_DOWN);
		column.setContentProvider(ArrayContentProvider.getInstance());
		column.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((ViewerContent)element).name;
			}
		});
		column.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		return column;
	}
	
	
	public void updateConfiguration(){
		importer.getConfiguration().setSkipHeader(btnSkipHeader.getSelection());
		if (cmbColumnSelectorDateFormat != null){
			importer.getConfiguration().setDateFormatString(getDateFormat());
		}
		
		importer.getConfiguration().setIdColumn(getIndex(idViewer));
		importer.getConfiguration().setStatusColumn(getIndex(statusViewer));
		importer.getConfiguration().setXColumn(getIndex(xViewer));
		importer.getConfiguration().setYColumn(getIndex(yViewer));
		if (projViewer != null){			
			importer.getConfiguration().setProjection((Projection)((IStructuredSelection)projViewer.getSelection()).getFirstElement());
		}
		for (Entry<EntityAttribute,ComboViewer> data : viewers.entrySet()){
			importer.getConfiguration().setColumn(data.getKey(), getIndex(data.getValue()));
		}
	}
	
	private Integer getIndex(ComboViewer cv){
		if (cv == null){
			return null;
		}
		if (cv.getSelection().isEmpty()){
			return null;
		}
		return ((ViewerContent)(((IStructuredSelection)cv.getSelection()).getFirstElement())).index;
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	class ViewerContent{
		
		public String name;
		public Integer index;
		
		public ViewerContent(String name, Integer index){
			this.name = name;
			this.index = index;
		}
	}
}
