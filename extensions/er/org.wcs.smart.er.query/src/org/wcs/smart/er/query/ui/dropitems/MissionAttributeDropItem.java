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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.MissionAttribute;
import org.wcs.smart.er.model.MissionAttributeListItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.filter.AttributeFilter;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Attribute drop item for numeric, text, and boolean attributes in observation
 * filters
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MissionAttributeDropItem extends DropItem implements IFilterDropItem {

	private DateTime dtime1;
	private DateTime dtime2;
	
	protected String text;
	protected String key;
	
	private String currentValue2 = null;
	private String currentValue = null;
	private String currentOp = null;	
	private Label lblAttribute;
	private Text value;
	private Combo operators;
	private AttributeType type = null;
	
	private MissionAttribute ma;
	private ComboViewer listViewer;
	private ListItem currentSelection = null;
	
//	private Attribute attribute = null;
		
	private Font smallerFont;
	
	/*
	 * Job to load the attribute list options
	 */
	private Job loadItemsJobs = new Job("Load list items"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			final ArrayList<ListItem> items = new ArrayList<ListItem>();
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				MissionAttribute attribute = (MissionAttribute) s.load(MissionAttribute.class, ma.getUuid());
				for (MissionAttributeListItem i : attribute.getAttributeList()){
					items.add(new ListItem(i.getUuid(), i.getName(), i.getKeyId()));
				}
				//add the any item
				items.add(0, AttributeFilter.ANY_OPTION);				
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
						listViewer.setSelection(new StructuredSelection(AttributeFilter.ANY_OPTION));
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
	public MissionAttributeDropItem(MissionAttribute attribute) {
		//super(parent, target);
		this.ma = attribute;
		this.type = attribute.getType();
		this.text = attribute.getName();
		this.key = "s:missionproperty:" + type.typeKey + ":" + attribute.getKeyId();
	}

	
	/**
	 * <p>
	 * For String or Numberic Attribute:
	 * data is a string array of length two where the first 
	 * value represents the operator and second the associated value
	 * </p><p>
	 * For Boolean Attributes:
	 * data should be null
	 * </p>
	 * @param data - a string array of the operator and value
	 */
	public void initializeData(Object data){
		if (data != null && data instanceof String[]){
			String[] initd = (String[])data;
			if (type == AttributeType.DATE){
				this.currentValue = initd[0];
				this.currentValue2 = initd[1];
				this.currentOp = initd[2];
			}else{
				this.currentOp = initd[0];
				this.currentValue = initd[1];
			}
		}else if (data != null && data instanceof ListItem){
			currentSelection = (ListItem)data;
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		if (operators != null){
			return this.text + " " + operators.getItem(operators.getSelectionIndex()) + " " ;//+ value.getText() ; //$NON-NLS-1$ //$NON-NLS-2$
		}else{
			return this.text;
		}
		
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder querypart = new StringBuilder();
		if (type == AttributeType.NUMERIC){
			querypart.append (this.key);
			querypart.append( " "); //$NON-NLS-1$
			querypart.append(Operator.NUMERIC_OPS[operators.getSelectionIndex()].asSmartValue());
			querypart.append(" "); //$NON-NLS-1$
			querypart.append(value.getText());
			
		}else if (type == AttributeType.TEXT){
			querypart.append (this.key);
			querypart.append( " "); //$NON-NLS-1$
			
			querypart.append(Operator.STRING_OPS[operators.getSelectionIndex()].asSmartValue());
			querypart.append(" \""); //$NON-NLS-1$
			querypart.append(value.getText());
			querypart.append("\""); //$NON-NLS-1$
		}else if (type == AttributeType.LIST){
			StringBuilder query = new StringBuilder(this.key);
			query.append(" = "); //$NON-NLS-1$
			
			ListItem it = null;
			if (currentSelection != null){
				it = currentSelection;
			}else{
				IStructuredSelection sel = (IStructuredSelection) listViewer.getSelection();
				if (sel != null && !sel.isEmpty()){
					it = (ListItem) sel.getFirstElement();
				}
			}
			if (it != null && (it.getUuid() != null || it == AttributeFilter.ANY_OPTION)){			
				query.append(it.getKey());
			}
			return query.toString();
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
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		if (type == AttributeType.NUMERIC ||
				type == AttributeType.TEXT){
			createDefaultComposite(parent);
		}else if (type == AttributeType.LIST){
			createListComposite(parent);
		}
	}
	private void createDefaultComposite(Composite parent){
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lblAttribute = new Label(main, SWT.NONE);
		if (type == AttributeType.TEXT || type == AttributeType.NUMERIC ) {
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
		}else if (type == AttributeType.DATE){
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
			
			dtime1 = new DateTime(main, SWT.DROP_DOWN | SWT.DATE | SWT.MEDIUM);
			dtime1.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					String newValue = (new java.sql.Date(SmartUtils.getDate(dtime1).getTime())).toString();
					if (!newValue.equals(currentValue)){
						queryChanged();
						currentValue = newValue;
					}
				}});
			
			dtime2 = new DateTime(main, SWT.DROP_DOWN | SWT.DATE | SWT.MEDIUM);
			dtime2.addListener(SWT.Selection, new Listener(){
				@Override
				public void handleEvent(Event event) {
					String newValue = (new java.sql.Date(SmartUtils.getDate(dtime2).getTime())).toString();
					if (!newValue.equals(currentValue2)){
						queryChanged();
						currentValue2 = newValue;
					}
				}});
			
			Operator[] options = Operator.DATE_OPS;
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
		
		lblAttribute.setText(formatStringForLabel(this.text));
		if (currentValue != null){
			if (value != null){
				value.setText(currentValue);
			}
			if (dtime1 != null && currentValue != null){
				SmartUtils.initDateDateTimeWidget(dtime1, java.sql.Date.valueOf(currentValue));	
			}
			if (dtime2 != null && currentValue2 != null){
				SmartUtils.initDateDateTimeWidget(dtime2, java.sql.Date.valueOf(currentValue2));	
			}
		}
	}
	
	protected void createListComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
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
		listViewer.setInput(new ListItem[]{new ListItem("Loading...")});
		
		initDrag(main);
		initDrag(lblAttribute);
		
		
		lblAttribute.setText(formatStringForLabel(this.text + " = ")); //$NON-NLS-1$
		loadItemsJobs.schedule();
	}

}
