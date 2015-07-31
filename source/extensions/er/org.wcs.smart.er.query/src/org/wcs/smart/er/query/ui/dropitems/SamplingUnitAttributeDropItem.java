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
package org.wcs.smart.er.query.ui.dropitems;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignSamplingUnitAttribute;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;

/**
 * Sampling unit attribute drop item that supports text, list
 * and numeric attributes.
 * 
 * @author Emily
 *
 */
public class SamplingUnitAttributeDropItem extends DropItem implements IFilterDropItem, ISurveyDesignDropItem {
	
	protected String text;
	protected String key;
	
	private String currentValue = null;
	private String currentOp = null;	
	private Label lblAttribute;
	private Text value;
	private Combo operators;
	private AttributeType type = null;
	private SamplingUnitAttribute ma;
	private Label lblError ;
	
	private ComboViewer listViewer;
	private ListItem currentSelection = null;
				
	private Font smallerFont;
	private SamplingUnitFilter.Source source;
	
	private SurveyDesign sd;
	private boolean isAttributeSd = true;
	private Composite parent;
	private Color redColor;
	private Color defaultColor;
	
	private Composite defaultComp = null;
	private Composite listComp = null;
	private Composite errorComp = null;
	private Composite outer = null;
	
	/*
	 * Job to load the attribute list options
	 */
	private Job loadItemsJobs = new Job(Messages.SamplingUnitAttributeDropItem_loadJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			final ArrayList<ListItem> items = new ArrayList<ListItem>();
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				SamplingUnitAttribute attribute = (SamplingUnitAttribute) s.load(SamplingUnitAttribute.class, ma.getUuid());
				for (SamplingUnitAttributeListItem i : attribute.getAttributeList()){
					items.add(new ListItem(i.getUuid(), i.getName(), i.getKeyId()));
				}
				//add the any item
				items.add(0, BasicDropItemFactory.ANY_OPTION);				
				if (currentSelection != null && !items.contains(currentSelection)){
					//item is not longer active; but still in query
					items.add(currentSelection);
				}

			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer == null || listViewer.getCombo().isDisposed()){
						return;
					}
					if (currentSelection != null && !items.contains(currentSelection)){
						items.add(currentSelection);
					}
					listViewer.setInput(items.toArray(new ListItem[items.size()]));
					if (currentSelection != null){
						listViewer.setSelection(new StructuredSelection(currentSelection));
					}else{
						listViewer.setSelection(new StructuredSelection(BasicDropItemFactory.ANY_OPTION));
					}
					getTargetPanel().redraw();
				}});
			return Status.OK_STATUS;
		}};
	/**
	 * Creates a new attribute drop item
	 * 
	 * @param parent parent composite
	 * @param target drop target
	 * @param att the category attribute to make up the drop item
	 */
	public SamplingUnitAttributeDropItem(SamplingUnitAttribute attribute, SamplingUnitFilter.Source source) {
		this.source = source;
		this.ma = attribute;
		this.type = attribute.getType();
		this.text = attribute.getName();
		this.key = "s:suattribute:" + type.typeKey + ":" + attribute.getKeyId(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Creates a new attribute drop item
	 * 
	 * @param parent parent composite
	 * @param target drop target
	 * @param att the category attribute to make up the drop item
	 */
	public SamplingUnitAttributeDropItem(SamplingUnitAttribute attribute) {
		this(attribute, null);
	}
	
	/**
	 * Sets the sampling unit filter source
	 * @param source
	 */
	public void setSource(SamplingUnitFilter.Source source){
		this.source = source;
	}
	
	/**
	 * <p>
	 * For String or Numeric Attribute: String array {operator, value}
	 * </p>
	 * @param data data to initialize drop item
	 */
	public void initializeData(Object data){
		if (type == AttributeType.NUMERIC || 
				type == AttributeType.TEXT){
			String[] initd = (String[])data;
			this.currentOp = initd[0];
			this.currentValue = initd[1];
		}else if (type == AttributeType.LIST){
			this.currentSelection = (ListItem) data;
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		String error = getErrorMessage();
		if (error != null){
			return null;
		}
		if (operators != null){
			return this.text + " " + operators.getItem(operators.getSelectionIndex()) + " ";//+ value.getText() ; //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			return this.text; 
		}	
	}
	
	private String getErrorMessage(){
		if (!isAttributeSd){
			return Messages.SamplingUnitAttributeDropItem_SuError;
		}
		return null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		String error = getErrorMessage();
		if (error != null){
			return null;
		}
		StringBuilder querypart = new StringBuilder();
		if (type == AttributeType.NUMERIC){
			querypart.append (this.key);
			querypart.append(":"); //$NON-NLS-1$
			querypart.append(source.queryKey);
			querypart.append( " "); //$NON-NLS-1$
			querypart.append(Operator.NUMERIC_OPS[operators.getSelectionIndex()].asSmartValue());
			querypart.append(" "); //$NON-NLS-1$
			querypart.append(value.getText());
			
		}else if (type == AttributeType.TEXT){
			querypart.append (this.key);
			querypart.append(":"); //$NON-NLS-1$
			querypart.append(source.queryKey);
			querypart.append( " "); //$NON-NLS-1$
			querypart.append(Operator.STRING_OPS[operators.getSelectionIndex()].asSmartValue());
			querypart.append(" \""); //$NON-NLS-1$
			querypart.append(value.getText());
			querypart.append("\""); //$NON-NLS-1$
		}else if (type == AttributeType.LIST){
			querypart.append (this.key);
			querypart.append(":"); //$NON-NLS-1$
			querypart.append(source.queryKey);
			querypart.append(" = "); //$NON-NLS-1$
			
			ListItem it = null;
			if (currentSelection != null){
				it = currentSelection;
			}else{
				IStructuredSelection sel = (IStructuredSelection) listViewer.getSelection();
				if (sel != null && !sel.isEmpty()){
					it = (ListItem) sel.getFirstElement();
				}
			}
			if (it != null && (it.getUuid() != null || it == BasicDropItemFactory.ANY_OPTION)){			
				querypart.append(it.getKey());
			}
		}
		return querypart.toString();
	}

	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		if (redColor != null){
			redColor.dispose();
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		this.parent = parent;
		this.defaultColor = parent.getBackground();
		
		outer = new Composite(parent, SWT.NONE);
		StackLayout layout = new StackLayout();
		outer.setLayout(layout);
		
		createErrorComposite(outer);
		if (type == AttributeType.NUMERIC || type == AttributeType.TEXT){
			createDefaultComposite(outer);
			layout.topControl = defaultComp;
		}else if (type == AttributeType.LIST){
			createListComposite(outer);
			layout.topControl = listComp;
		}
		if (getErrorMessage() != null){
			layout.topControl = errorComp;
			parent.setBackground(redColor);
		}
	}

	
	private void createDefaultComposite(Composite parent){
		Composite main = new Composite(parent, SWT.NONE);
		defaultComp = main;
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		operators = new Combo(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		operators.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (currentOp != null
						&& currentOp.equals(operators.getText())) {
					// no change
				} else {
					currentOp = operators.getText();
					queryChanged();
				}
			}
		});
		FontData fd = (operators.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		operators.setFont(smallerFont);

		value = new Text(main, SWT.BORDER);
		value.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				if (currentValue != null
						&& currentValue.equals(value.getText())) {
					// nothing changed
				} else {
					queryChanged();
					value.setToolTipText(value.getText());
					currentValue = value.getText();
				}
			}
		});
		GridData gd = new GridData();
		gd.minimumWidth = 50;
		gd.widthHint = 100;
		value.setLayoutData(gd);

		Operator[] options = null;
		if (type == AttributeType.NUMERIC) {
			options = Operator.NUMERIC_OPS;
		} else if (type == AttributeType.TEXT) {
			options = Operator.STRING_OPS;
		}
		if (options != null) {
			int index = 0;
			for (int i = 0; i < options.length; i++) {
				operators.add(options[i].getGuiValue());
				if (currentOp != null
						&& currentOp.equals(options[i].getGuiValue())) {
					index = i;
				}
			}
			operators.select(index);
		}
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(MessageFormat.format("({0}) {1}", new Object[]{source.guiName, formatStringForLabel(this.text)})); //$NON-NLS-1$
		if (currentValue != null){
			if (value != null){
				value.setText(currentValue);
			}
		}
	}
	
	protected void createListComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		listComp = main;
		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);

		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.setLabelProvider(ListItem.createLabelProvider());
		
		listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ListItem newSelection = (ListItem) ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
				ListItem lastSelection = currentSelection;
				
				currentSelection = newSelection;
				if (! (lastSelection != null && lastSelection.equals(newSelection))){
					queryChanged();	
				}
			}
		});
		listViewer.setInput(new ListItem[]{new ListItem(Messages.SamplingUnitAttributeDropItem_LoadingLabel)});
		
		initDrag(main);
		initDrag(lblAttribute);
		
		lblAttribute.setText(MessageFormat.format("({0}) {1} = ", new Object[]{source.guiName, formatStringForLabel(this.text)})); //$NON-NLS-1$
		loadItemsJobs.schedule();
	}

	protected void createErrorComposite(Composite parent) {
		redColor =  new Color(Display.getDefault(),new RGB(255, 210,210) );
		
		Composite c = new Composite(parent, SWT.NONE);
		errorComp = c;
		c.setBackground(redColor);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 0;
		c.setLayout(gl);
		
		Label lblImage = new Label(c, SWT.NONE);
		lblImage.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.EXCLAMATION_ICON));
		lblImage.setBackground(redColor);
		lblError = new Label(c, SWT.NONE);
		lblError.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (getErrorMessage() != null){
			lblError.setText(getErrorMessage());
		}
		lblError.setBackground(redColor);
	}
	
	@Override
	public void setSurveyDesign(SurveyDesign design) {
		this.sd = design;
		if (sd != null){
			isAttributeSd = false;
			Session s = HibernateManager.openSession();
			try{
				SurveyDesign temp = (SurveyDesign) s.load(SurveyDesign.class, design.getUuid());
				for (SurveyDesignSamplingUnitAttribute att : temp.getSamplingUnitAttributes()){
					if (att.getSamplingUnitAttribute().equals(ma)){
						isAttributeSd = true;
						break;
					}
				}
			}finally{
				s.close();
			}
		}else{
			isAttributeSd = true;
		}
		updateComposite();
	}
	
	private void updateComposite(){
		if (getErrorMessage() != null){
			lblError.setText(getErrorMessage());
			((StackLayout)outer.getLayout()).topControl = errorComp;
			parent.setBackground(redColor);
		}else if (type == AttributeType.NUMERIC || type == AttributeType.TEXT){
			((StackLayout)outer.getLayout()).topControl = defaultComp;
			parent.setBackground(defaultColor);
		}else if (type == AttributeType.LIST){
			((StackLayout)outer.getLayout()).topControl = listComp;
			parent.setBackground(defaultColor);
		}
		outer.layout();
		getTargetPanel().redraw();
	}
}
