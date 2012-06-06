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
package org.wcs.smart.ui.internal.ca.properties;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Composite panel that display and allows users to
 * edit a data model attribute.
 * 
 * @author Emily
 * @since 1.0.0
 */
public abstract class AttributeInfoPanel extends NameKeyComposite {

	private static final Color BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private static final Color GRAY = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

	
	private Text txtMinValue;
	private Text txtMaxValue;
	private Text txtRegex;
	
	private Composite optionComposite;
	private Composite booleanComposite;
	private Composite treeComposite;
	private Composite listComposite;
	private Composite textComposite;
	private Composite numericComposite;

	private ComboViewer cmbType;
	private Button chRequired;
	
	private ControlDecoration cdMinValue;
	private ControlDecoration cdMaxValue;
	private ControlDecoration cdAttList;
	private ControlDecoration cdAttTree;

	private TableViewer lstAttributeList; 
	private Button[] btnAggs;	//list of aggregation options
	
	private ArrayList<IValidationListener> listeners = new ArrayList<IValidationListener>();
	private Button btnDisableListItem;
	private Button btnDeleteListItem;
	
	private WritableList attributeList = new WritableList();
	private Language lang;
	
	private AttributeTree attTree = null;
	private Session currentSession = null;
	/**
	 * Creates a new attribute panel
	 * @param parent parent composite
	 * @param style composite style
	 * @param canEdit <code>true</code> if the panel supports editing of the attributes; <code>false</code> if only viewable 
	 * @param createNew <code>true</code> if a new attribute is being created, <code>false</code> if attribute is being updated
	 * @param lang language being updated
	 * @param currentSession can be null if panel not editable
	 */
	public AttributeInfoPanel(Composite parent, int style, 
			boolean canEdit, boolean createNew, 
			Language lang, Session currentSession) {
		
		super(parent, style);
		
		this.lang = lang;
		this.currentSession = currentSession;
		setLayout(new GridLayout(3, false));
		
		/* Type */
		Label lblNewLabel_2 = new Label(this, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_2.setText("Type:");
		
		cmbType = new ComboViewer(this, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((Attribute.AttributeType)element).name();
			}
		});
		cmbType.setInput(Attribute.AttributeType.values());
		Combo combo = cmbType.getCombo();
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (!canEdit){
			combo.setEnabled(false);
		}
		combo.select(0);
		combo.addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectOption();
				validate();
			}
		});
		
		/* Name & Key */
		createNameKeyFields(this, canEdit, createNew);
		
		/* required */
		Label lblRequired = new Label(this, SWT.NONE);
		lblRequired.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblRequired.setText("Required:");
		
		chRequired = new Button(this, SWT.CHECK);
		chRequired.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		if (!canEdit){
			chRequired.setEnabled(false);
		}
		

		optionComposite = new Composite(this, SWT.NONE);
		optionComposite.setLayout(new StackLayout());
		optionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		/* Numeric Attribute Options */
		numericComposite = new Composite(optionComposite, SWT.NONE);
		numericComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		numericComposite.setBounds(0, 0, 64, 64);
		numericComposite.setLayout(new GridLayout(2, false));
		
		Label lblAggregations = new Label(numericComposite, SWT.NONE);
		lblAggregations.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		lblAggregations.setText("Aggregations:");
		lblAggregations.setToolTipText("Describes how data to be summarized when creating reports.");
		
		Composite compAggs = new Composite(numericComposite, SWT.NONE);
		GridLayout ll = new GridLayout(2, true);
		ll.marginTop = 0;
		compAggs.setLayout(ll);
		compAggs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		List<Aggregation> aggs = DataModel.getAggregations();
		btnAggs = new Button[aggs.size()];
		for (int i = 0; i < aggs.size(); i++){
			btnAggs[i] = new Button(compAggs, SWT.CHECK);
			btnAggs[i].setText(aggs.get(i).getGuiName());
			btnAggs[i].setData(aggs.get(i));
			if (createNew && aggs.get(i).getName().equals("sum")){
				btnAggs[i].setSelection(true);
			}
			btnAggs[i].setEnabled(canEdit);
		}
		
		Label lblNewLabel_3 = new Label(numericComposite, SWT.NONE);
		lblNewLabel_3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_3.setBounds(0, 0, 55, 15);
		lblNewLabel_3.setText("Minimum Value:");
		
		txtMinValue = new Text(numericComposite, SWT.BORDER);
		txtMinValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtMinValue.setBounds(0, 0, 76, 21);
		cdMinValue = createDecoration(txtMinValue);
		txtMinValue.addListener(SWT.Modify, getChangeListener());
		
		Label lblNewLabel_4 = new Label(numericComposite, SWT.NONE);
		lblNewLabel_4.setAlignment(SWT.RIGHT);
		lblNewLabel_4.setBounds(0, 0, 55, 15);
		lblNewLabel_4.setText("Maximum Value:");
		
		txtMaxValue = new Text(numericComposite, SWT.BORDER);
		txtMaxValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtMaxValue.setBounds(0, 0, 76, 21);
		cdMaxValue = createDecoration(txtMaxValue);		
		txtMaxValue.addListener(SWT.Modify,  getChangeListener());
		if (!canEdit){
			txtMinValue.setEditable(false);
			txtMaxValue.setEditable(false);
		}
		
		/*   Text Attribute Options */
		textComposite = new Composite(optionComposite, SWT.NONE);
		textComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textComposite.setBounds(0, 0, 64, 64);
		textComposite.setLayout(new GridLayout(2, false));
		
		Label lblNewLabel_5 = new Label(textComposite, SWT.NONE);
		lblNewLabel_5.setBounds(0, 0, 55, 15);
		lblNewLabel_5.setText("Regular Expression Validation:");
		
		txtRegex = new Text(textComposite, SWT.BORDER);
		txtRegex.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtRegex.setBounds(0, 0, 76, 21);
		if (!canEdit){
			txtRegex.setEditable(false);
		}
		
		
		/*   List Attribute Options */
		listComposite = new Composite(optionComposite, SWT.NONE);
		listComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		listComposite.setBounds(0, 0, 64, 64);
		listComposite.setLayout(new GridLayout(3, false));
		
		Label lblValues = new Label(listComposite, SWT.NONE);
		lblValues.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		lblValues.setBounds(0, 0, 55, 15);
		lblValues.setText("Values:");
		
		lstAttributeList = new TableViewer(listComposite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		Table list = lstAttributeList.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		((GridData)list.getLayoutData()).heightHint = 80;
		((GridData)list.getLayoutData()).widthHint = 100;
		list.setBounds(0, 0, 88, 68);
		lstAttributeList.setContentProvider(new ObservableListContentProvider());
		lstAttributeList.setInput(attributeList);
		cdAttList = createDecoration(lstAttributeList.getControl());
		lstAttributeList.setLabelProvider(new AttributeListLabelProvider());
		
		if (!canEdit){
			lstAttributeList.getTable().setEnabled(false);
		}
		if (canEdit){
			Composite buttonPanel = new Composite(listComposite, SWT.NONE);
			buttonPanel.setLayout(new GridLayout(1, false));
			buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP,false, false));
			
			Button btnAddList = new Button(buttonPanel, SWT.NONE);
			btnAddList.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
			btnAddList.setText("Add");
			btnAddList.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AttributeListItem it = new AttributeListItem();
					AttributeItemDialog dd = new AttributeItemDialog(getShell(), it, attributeList, AttributeInfoPanel.this.lang);
					int ret = dd.open();
					if (ret == Window.CANCEL){
						return;
					}
					it.setIsActive(true);
					it.setListOrder(attributeList.size());
					attributeList.add(it);
					lstAttributeList.refresh();
					validate();
				}
			});
			Button btnEditList = new Button(buttonPanel, SWT.NONE);
			btnEditList.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
			btnEditList.setText(DialogConstants.EDIT_BUTTON_TEXT);
			btnEditList.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					if (it == null) return;
					AttributeItemDialog dd = new AttributeItemDialog(getShell(), it, attributeList, AttributeInfoPanel.this.lang);
					int ret = dd.open();
					if (ret == Window.CANCEL){
						return;
					}
					lstAttributeList.refresh();
					validate();
				}
			});
			btnDisableListItem = new Button(buttonPanel, SWT.NONE);
			btnDisableListItem.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
			btnDisableListItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
			btnDisableListItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					it.setIsActive(!it.getIsActive());
					lstAttributeList.refresh();
					validate();
				}
			});
			
			btnDeleteListItem = new Button(buttonPanel, SWT.NONE);
			btnDeleteListItem.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
			btnDeleteListItem.setText("Delete");
			btnDeleteListItem.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					final AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					boolean ret = MessageDialog.openConfirm(getShell(), "Delete", 
							"Are you sure you want to delete the list item: " + 
							it.findName(AttributeInfoPanel.this.lang) + "?");
					if (!ret){
						return;
					}
					
					runInProgressDialog(new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
							boolean delete = DataModelManager.getInstance().validateDelete(it, monitor, AttributeInfoPanel.this.currentSession);
							if (delete){
								attributeList.remove(it);
								it.setAttribute(null);
								lstAttributeList.refresh();
								validate();
							}
						}
					});
				}
			});
			
			lstAttributeList.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					btnDisableListItem.setEnabled(it != null);
					btnDeleteListItem.setEnabled(it != null);
					if (it != null && it.getIsActive()){
						btnDisableListItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					}else{
						btnDisableListItem.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					}
				}
			});
			
			/* drag and drop support */
			int operations = DND.DROP_MOVE;
			Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
			lstAttributeList.addDragSupport(operations, transferTypes, new DragSourceListener() {
				@Override
				public void dragStart(DragSourceEvent event) {
					LocalSelectionTransfer.getTransfer().setSelection(lstAttributeList.getSelection());
					event.doit = true;
					
				}
				
				@Override
				public void dragSetData(DragSourceEvent event) {
					if (LocalSelectionTransfer.getTransfer()
							.isSupportedType(event.dataType)) {
						event.data = lstAttributeList.getSelection();
					}
				}
				
				@Override
				public void dragFinished(DragSourceEvent event) {
					LocalSelectionTransfer.getTransfer().setSelection(null);
					lstAttributeList.refresh();
				}
			});
			
			ViewerDropAdapter dropAdapter = new ViewerDropAdapter(lstAttributeList) {
				
				@Override
				public boolean validateDrop(Object target, int operation,
						TransferData transferType) {
					if (target instanceof AttributeListItem){
						return true;
					}
					return false;
				}
				
				@Override
				public boolean performDrop(Object data) {
					StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
					if (selection == null){
						return false;
					}
					Object obj = selection.getFirstElement();
					
					int loc = getCurrentLocation();
					AttributeListItem target = (AttributeListItem)getCurrentTarget();
					if (target.equals(obj)){
						return false;
					}
					int index = attributeList.indexOf(obj);
					int toIndex = attributeList.indexOf(target);
					attributeList.move(index, toIndex);
				
					return true;
				}
			};
			lstAttributeList.addDropSupport(operations, transferTypes,dropAdapter);
				
		}
		
		/*   Tree Attribute Options */
		treeComposite = new Composite(optionComposite, SWT.NONE);
		treeComposite.setLayout(new GridLayout(1, false));
		treeComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		treeComposite.setBounds(0, 0, 64, 64);
		
		
		if (canEdit){
			attTree = new AttributeTree();
			Composite tree = attTree.createTree(treeComposite, lang);
			cdAttTree = createDecoration(tree);
			attTree.setListener(new AttributeTreeChangeListener() {
				@Override
				public void treeModified() {
					validate();	
				}
			});
		}

		/*   Boolean Attribute Options */
		booleanComposite = new Composite(optionComposite, SWT.NONE);
		
		selectOption();
		validate();
	}

	/**
	 * Run a taks in a progress monitor
	 * @param runnable
	 */
	private void runInProgressDialog(IRunnableWithProgress runnable){
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(false, true, runnable);		
		} catch (Exception ex) {
			SmartPlugIn.displayLog(getShell(), "Error occurred.", ex);
		}
	}
	
	/**
	 * Validates attribute input
	 * @return <code>true</code> if all fields validate correctly, <code>false</code> if error exists
	 */
	@Override
	public boolean validate(){
		boolean error = super.validate();
		cdMaxValue.hide();
		cdMinValue.hide();
		cdAttList.hide();
		if (cdAttTree != null){
			cdAttTree.hide();
		}
		
		Attribute.AttributeType type = (Attribute.AttributeType)(((IStructuredSelection)cmbType.getSelection()).getFirstElement());
		if (type.equals(AttributeType.BOOLEAN)){
			
		}else if (type.equals(AttributeType.NUMERIC)){
			Double min = null;
			Double max = null;
			if (txtMaxValue.getText().length() > 0){
				try{
					max = Double.valueOf(txtMaxValue.getText());
				}catch (Exception ex){
					error = true;
					cdMaxValue.show();
					cdMaxValue.setDescriptionText("Invalid maximum value");
				}
			}
			if (txtMinValue.getText().length() > 0){
				try{
					min = Double.valueOf(txtMinValue.getText());
				}catch (Exception ex){
					error = true;
					cdMinValue.show();
					cdMinValue.setDescriptionText("Invalid minimum value");
				}
			}
			if (min != null && max != null && min > max){
				cdMaxValue.show();
				cdMaxValue.setDescriptionText("Maximum value cannot be greater than minimum value.");
				error = true;
			}
		}else if (type.equals(AttributeType.TEXT)){
			//TODO: validate regex expression
		}else if (type.equals(AttributeType.LIST)){
			if (this.attributeList.size() == 0){
				cdAttList.setDescriptionText("At least one list element must be defined for an attribute of type list.");
				cdAttList.show();
				error = false;
				
			}
		}else if (type.equals(AttributeType.TREE)){
			if (this.attTree != null){
				if (this.attTree.getAttribute().getTree() == null || this.attTree.getAttribute().getTree().size() == 0){
					cdAttTree.setDescriptionText("At least one tree node must be defined.");
					cdAttTree.show();
					error = false;
				}
			}
		}
		
		for (IValidationListener listener: listeners){
			listener.validated(error);
		}
		return error;
	}
	/**
	 * Add a listener to fire when panel validated.
	 * @param listener
	 */
	public void addValidationListener(IValidationListener listener){
		listeners.add(listener);
	}
	/**
	 * Removes a listener
	 * @param listener
	 */
	public void removeValidationListener(IValidationListener listener){
		listeners.remove(listener);
	}
	
	/*
	 * displays the option panel based
	 * on the attribute type selection
	 */
	private void selectOption(){
		Attribute.AttributeType type = (Attribute.AttributeType)(((IStructuredSelection)cmbType.getSelection()).getFirstElement());
		if (type.equals(AttributeType.BOOLEAN)){
			((StackLayout)optionComposite.getLayout()).topControl = booleanComposite;
		}else if (type.equals(AttributeType.NUMERIC)){
			((StackLayout)optionComposite.getLayout()).topControl = numericComposite;
		}else if (type.equals(AttributeType.TEXT)){
			((StackLayout)optionComposite.getLayout()).topControl = textComposite;
		}else if (type.equals(AttributeType.LIST)){
			((StackLayout)optionComposite.getLayout()).topControl = listComposite;
		}else if (type.equals(AttributeType.TREE)){
			((StackLayout)optionComposite.getLayout()).topControl = treeComposite;
		}
		optionComposite.layout();
	}

	/**
	 * Updates the fields with the values from the associated attribute
	 * @param att attribute
	 */
	public void setAttribute(Attribute att){
		initFields(att, lang);
		chRequired.setSelection(att.getIsRequired());
		
		if (att.getType() != null) {
			cmbType.setSelection(new StructuredSelection(att.getType()));

			if (att.getType().equals(Attribute.AttributeType.NUMERIC)) {
				numericComposite.setVisible(true);
				if (att.getAggregations() != null){
					for (Button btnAgg : btnAggs){
						if (att.getAggregations().contains(btnAgg.getData())){
							btnAgg.setSelection(true);
						}else{
							btnAgg.setSelection(false);
						}
					}
				}
				txtMaxValue.setText("");
				txtMinValue.setText("");
				if (att.getMaxValue() != null) {
					txtMaxValue.setText(String.valueOf(att.getMaxValue()));
				}
				if (att.getMinValue() != null) {
					txtMinValue.setText(String.valueOf(att.getMinValue()));
				}
			} else if (att.getType().equals(Attribute.AttributeType.TEXT)) {
				textComposite.setVisible(true);
				txtRegex.setText("");
				if (att.getRegex() != null) {
					txtRegex.setText(att.getRegex());
				}
			} else if (att.getType().equals(Attribute.AttributeType.LIST)) {
				listComposite.setVisible(true);
				Collections.sort(att.getAttributeList(),
						new Comparator<AttributeListItem>() {
							@Override
							public int compare(AttributeListItem o1,
									AttributeListItem o2) {
								if (o1.getListOrder() == o2.getListOrder()) {
									return 0;
								} else if (o1.getListOrder() > o2
										.getListOrder()) {
									return 1;
								} else {
									return -1;
								}
							}
						});
				attributeList = new WritableList(att.getAttributeList(),
						AttributeListItem.class);
				lstAttributeList.setInput(attributeList);
			} else if (att.getType().equals(Attribute.AttributeType.TREE)) {
				treeComposite.setVisible(false);
				if(attTree != null){
					attTree.setInput(att, currentSession);
				}
			}
		}
		selectOption();
		
		validate();
	}
	
	/**
	 * Updates the given attribute with the contents of the 
	 * gui components.
	 * 
	 * @param att attribute to update
	 */
	public void updateAttribute(Attribute att){
		updateFields(att, lang);
		att.setType(  (Attribute.AttributeType)((IStructuredSelection)cmbType.getSelection()).getFirstElement() );
		att.setIsRequired(chRequired.getSelection());
		
		if (att.getType().equals(Attribute.AttributeType.NUMERIC)){
			if (att.getAggregations() == null){
				att.setAggregations(new ArrayList<Aggregation>());
			}
			for (Button btnAgg: btnAggs){
				Aggregation ag = (Aggregation)btnAgg.getData();
				if (btnAgg.getSelection()){
					if (!att.getAggregations().contains(ag)){
						att.getAggregations().add(ag);
					}
				}else{
					if (att.getAggregations().contains(ag)){
						att.getAggregations().remove(ag);
					}
				}
			}
			
			if (txtMaxValue.getText().length() > 0){
				att.setMaxValue(Double.valueOf(txtMaxValue.getText()));
			}
			if (txtMinValue.getText().length() > 0){
				att.setMinValue(Double.valueOf(txtMinValue.getText()));
			}
		}else if (att.getType().equals(Attribute.AttributeType.TEXT)){
			att.setRegex(txtRegex.getText());
		}else if (att.getType().equals(Attribute.AttributeType.LIST)){
			if (att.getAttributeList() == null){
				att.setAttributeList(new ArrayList<AttributeListItem>());
				att.getAttributeList().addAll(attributeList);
			}
			for (int i = 0; i < attributeList.size(); i ++){
				((AttributeListItem)attributeList.get(i)).setListOrder(i);
			}
			for (AttributeListItem item : att.getAttributeList()){
				item.setAttribute(att);
			}
		}else if (att.getType().equals(Attribute.AttributeType.TREE)){
			if(attTree != null){
				List<AttributeTreeNode> root = attTree.getAttribute().getTree();
				for (AttributeTreeNode n : root){
					setAttribute(att, n);
				}
				att.setTree(root);
			}
		}
	}
	

	private void setAttribute(Attribute newAttribute, AttributeTreeNode node){
		node.setAttribute(newAttribute);
		if (node.getChildren() != null){
			for (AttributeTreeNode child : node.getChildren()){
				setAttribute(newAttribute, child);
			}
		}
		
	}
		
	/**
	 * Validation listener
	 * 
	 */
	interface IValidationListener{
		/**
		 * 
		 * @param hasError if the validation passed or failed
		 */
		void validated(boolean hasError);
	}
	
	/*
	 * Label provided for attribute list
	 */
	class AttributeListLabelProvider extends LabelProvider implements IColorProvider { 

		
		@Override
		public String getText(Object element) {
			AttributeListItem it = (AttributeListItem) element;
			return it.findName(AttributeInfoPanel.this.lang) + " [" + it.getKeyId() + "]";

		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		@Override
		public Color getForeground(Object element) {
			AttributeListItem it = (AttributeListItem)element;
			if (it.getIsActive()){
				return BLACK;
			}else{
				return GRAY;
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
		 */
		@Override
		public Color getBackground(Object element) {
			return null;
		}

	}
}

