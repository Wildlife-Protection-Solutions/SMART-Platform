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
package org.wcs.smart.ui.ca.properties;

import java.lang.reflect.InvocationTargetException;
import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.ITreeNodeVisitor;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.IconPanel;
import org.wcs.smart.ui.ca.properties.NameKeyComposite.IChangeListener;
import org.wcs.smart.ui.internal.ca.properties.AttributeTree;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Composite panel that display and allows users to
 * edit a data model attribute.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeInfoPanel extends Composite {

	private static final Color BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private static final Color GRAY = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

	
	protected NameKeyComposite nameKeyValues;
	
	private Text txtMinValue;
	private Text txtMaxValue;
	private Text txtRegex;
	private Text txtDecimal;
	
	private Composite optionComposite;
	private Composite booleanComposite;
	private Composite dateComposite;
	private Composite treeComposite;
	private Composite listComposite;
	private Composite textComposite;
	private Composite numericComposite;
	private Composite geometryComposite;

	private ComboViewer cmbType;
	private Button chRequired;
	
	private ControlDecoration cdMinValue;
	private ControlDecoration cdMaxValue;
	private ControlDecoration cdAttList;
	private ControlDecoration cdAttTree;
	private ControlDecoration cdDecimal;
	private ControlDecoration cdRegex;

	private Language currentDisplayLang = null;
	
	private TableViewer lstAttributeList; 
	private Button[] btnAggs;	//list of aggregation options
	
	private ArrayList<IValidationListener> listeners = new ArrayList<IValidationListener>();
	private Button btnDisableListItem;
	private Button btnDeleteListItem;
	private Button btnMoveUp;
	private Button btnMoveDown;
	private Button btnSort;
	
	private Button btnConvert;
	
	private List<NamedKeyItem> attributeList = new ArrayList<NamedKeyItem>();
	
	private AttributeTree attTree = null;
	private Session currentSession = null;
	private IconPanel iconPanel = null;
	private boolean canEdit = false;
	
	private ScrolledComposite scroll;
	private Composite main;
	
	/**
	 * Creates a new attribute panel
	 * @param parent parent composite
	 * @param style composite style
	 * @param canEdit <code>true</code> if the panel supports editing of the attributes; <code>false</code> if only viewable 
	 * @param createNew <code>true</code> if a new attribute is being created, <code>false</code> if attribute is being updated
	 * @param currentSession can be null if panel not editable
	 */
	public AttributeInfoPanel(Composite parent, int style, 
			boolean canEdit, boolean createNew, Session currentSession) {
		
		super(parent, style);
		
		this.currentSession = currentSession;
		this.canEdit = canEdit;
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		scroll = new ScrolledComposite(this, SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		main = new Composite(scroll, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		scroll.setContent(main);
		
		
		/* Type */
		Label lblNewLabel_2 = new Label(main, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_2.setText(Messages.AttributeInfoPanel_Type_Label);
		
		
		
		if (!canEdit){
			cmbType = new ComboViewer(main, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbType.getControl().setEnabled(false);
			cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}else {
			Composite tt = new Composite(main, SWT.NONE);
			tt.setLayout(new GridLayout(2, false));
			((GridLayout)tt.getLayout()).marginWidth = 0;
			((GridLayout)tt.getLayout()).marginHeight = 0;
			tt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

			cmbType = new ComboViewer(tt, SWT.SIMPLE | SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			
			btnConvert = new Button(tt, SWT.PUSH);
			btnConvert.setText(Messages.AttributeInfoPanel_ConvertToMultiList);
			btnConvert.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			btnConvert.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			btnConvert.addListener(SWT.Selection, e->{
				cmbType.setSelection(new StructuredSelection(Attribute.AttributeType.MLIST));
				btnConvert.setEnabled(false);
			});
			
			
		}
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element) {
				return ((Attribute.AttributeType)element).getName(Locale.getDefault());
			}
		});
		cmbType.setInput(Attribute.AttributeType.values());
		
		cmbType.setSelection(new StructuredSelection(Attribute.AttributeType.TEXT));
		cmbType.getCombo().addSelectionListener(new SelectionAdapter() {	
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectOption();
				validate();
			}
		});
		
		/* Name & Key */
		nameKeyValues = new NameKeyComposite();
		nameKeyValues.createControls(main, canEdit, createNew, new IChangeListener() {
			@Override
			public void itemModified() {
				validate();	
			}
		});

		/* required */
		Label lblRequired = new Label(main, SWT.NONE);
		lblRequired.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblRequired.setText(Messages.AttributeInfoPanel_Required_Label);
		
		chRequired = new Button(main, SWT.CHECK);
		chRequired.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		if (!canEdit){
			chRequired.setEnabled(false);
		}
		
		// icons
		Label lblIcons= new Label(main, SWT.NONE);
		lblIcons.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		lblIcons.setText(Messages.AttributeInfoPanel_IconLabel);
		
		iconPanel = new IconPanel(main, canEdit);
		iconPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		iconPanel.addListener(SWT.Selection, e->{
			validate();
			resize();
		});
		
		optionComposite = new Composite(main, SWT.NONE);
		optionComposite.setLayout(new StackLayout());
		optionComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		/* Numeric Attribute Options */
		numericComposite = new Composite(optionComposite, SWT.NONE);
		numericComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		numericComposite.setLayout(new GridLayout(2, false));
		
		Label lblAggregations = new Label(numericComposite, SWT.NONE);
		lblAggregations.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1));
		lblAggregations.setText(Messages.AttributeInfoPanel_Aggregations_Label);
		lblAggregations.setToolTipText(Messages.AttributeInfoPanel_Aggregations_ToolTip);
		
		Composite compAggs = new Composite(numericComposite, SWT.NONE);
		GridLayout ll = new GridLayout(2, false);
		ll.marginTop = 0;
		compAggs.setLayout(ll);
		compAggs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		List<Aggregation> aggs = DataModel.getAggregations();
		btnAggs = new Button[aggs.size()];
		for (int i = 0; i < aggs.size(); i++){
			btnAggs[i] = new Button(compAggs, SWT.CHECK);
			btnAggs[i].setText(Aggregation.getGuiName(aggs.get(i), currentSession, Locale.getDefault()));
			btnAggs[i].setData(aggs.get(i));
			if (createNew){
				btnAggs[i].setSelection(true);
			}
			btnAggs[i].setEnabled(canEdit);
		}
		
		Label lblNewLabel_3 = new Label(numericComposite, SWT.NONE);
		lblNewLabel_3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_3.setText(Messages.AttributeInfoPanel_Min_Label);
		
		txtMinValue = new Text(numericComposite, SWT.BORDER);
		txtMinValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		if (canEdit){
			cdMinValue = createDecoration(txtMinValue);
			txtMinValue.addListener(SWT.Modify, new Listener() {
				@Override
				public void handleEvent(Event event) {
					validate();
				}
			});
		}
		
		Label lblNewLabel_4 = new Label(numericComposite, SWT.NONE);
		lblNewLabel_4.setAlignment(SWT.RIGHT);
		lblNewLabel_4.setText(Messages.AttributeInfoPanel_Max_Label);
		
		txtMaxValue = new Text(numericComposite, SWT.BORDER);
		txtMaxValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		if (canEdit){
			cdMaxValue = createDecoration(txtMaxValue);		
			txtMaxValue.addListener(SWT.Modify, new Listener() {
				@Override
				public void handleEvent(Event event) {
					validate();
				}
			});
		}
		
		
		Label lbl = new Label(numericComposite, SWT.NONE);
		lbl.setAlignment(SWT.RIGHT);
		lbl.setText(Messages.AttributeInfoPanel_DecimalPlacesLbl);
		lbl.setToolTipText(Messages.AttributeInfoPanel_DecimalPlacesTooltip);
		
		txtDecimal = new Text(numericComposite, SWT.BORDER);
		txtDecimal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		if (canEdit){
			cdDecimal = createDecoration(txtDecimal);		
			txtDecimal.addListener(SWT.Modify, new Listener() {
				@Override
				public void handleEvent(Event event) {
					validate();
				}
			});
		}
		
		if (!canEdit){
			txtMinValue.setEditable(false);
			txtMaxValue.setEditable(false);
			txtDecimal.setEditable(false);
		}
		
		/*   Text Attribute Options */
		textComposite = new Composite(optionComposite, SWT.NONE);
		textComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		textComposite.setLayout(new GridLayout(2, false));
		
		Label lblNewLabel_5 = new Label(textComposite, SWT.NONE);
		lblNewLabel_5.setText(Messages.AttributeInfoPanel_Regex_Label);
		
		txtRegex = new Text(textComposite, SWT.BORDER);
		txtRegex.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		if (!canEdit){
			txtRegex.setEditable(false);
		}else {
			cdRegex = createDecoration(txtRegex);		
			txtRegex.addListener(SWT.Modify, new Listener() {
				@Override
				public void handleEvent(Event event) {
					validate();
				}
			});
		}
		
		
		/*   List Attribute Options */
		listComposite = new Composite(optionComposite, SWT.NONE);
		listComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		listComposite.setLayout(new GridLayout(3, false));

		
		Label lblValues = new Label(listComposite, SWT.NONE);
		lblValues.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 1, 1));
		lblValues.setText(Messages.AttributeInfoPanel_ValuesLabel);
		
		Composite wrapper = new Composite(listComposite, SWT.NONE);
		TableColumnLayout layout = new TableColumnLayout();
		wrapper.setLayout(layout);
		wrapper.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		((GridData)wrapper.getLayoutData()).heightHint = 100;
		((GridData)wrapper.getLayoutData()).widthHint = 100;
		
		lstAttributeList = new TableViewer(wrapper, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		//Table list = lstAttributeList.getTable();
		
		
		lstAttributeList.setContentProvider(ArrayContentProvider.getInstance());
		lstAttributeList.setInput(attributeList);
		if (canEdit){
			cdAttList = createDecoration(lstAttributeList.getControl());
		}
		
		TableViewerColumn colEmpty = new TableViewerColumn(lstAttributeList, SWT.NONE);
		
		colEmpty.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return null;
			}
		});
		layout.setColumnData(colEmpty.getColumn(), new ColumnWeightData(0, false));
		colEmpty.getColumn().setWidth(0);
		
		
		TableViewerColumn colLabel = new TableViewerColumn(lstAttributeList, SWT.NONE);
		colLabel.setLabelProvider(new AttributeListLabelProvider());
		layout.setColumnData(colLabel.getColumn(), new ColumnWeightData(70, true));
		
		TableViewerColumn imageLabel = new TableViewerColumn(lstAttributeList, SWT.NONE);
		layout.setColumnData(imageLabel.getColumn(), new ColumnWeightData(30, true));
		imageLabel.setLabelProvider(new ColumnLabelProvider() {
					private List<Image> images = new ArrayList<>();
					@Override
					public String getText(Object element) {
						return null;
					}
					
					@Override
					public Image getImage(Object element) {
						if (element instanceof AttributeListItem) {
							AttributeListItem li = (AttributeListItem)element;
						
							if (li.getIcon() == null) return null;
							
							List<IconFile> files = li.getIcon().getFiles();
							if (files.isEmpty()) return null;
							
							//combine all icons into a single image
							Image img = IconManager.INSTANCE.generateImage(li.getIcon(), IconManager.Size.ICON);
							images.add(img);
							return img;
							
						}
						return null;
					}
					
					@Override
					public void dispose() {
						super.dispose();
						images.forEach(i->i.dispose());
					}
					
		});
		
		if (canEdit){
			nameKeyValues.langViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					lstAttributeList.refresh();
				}
			});
			
			Composite buttonPanel = new Composite(listComposite, SWT.NONE);
			buttonPanel.setLayout(new GridLayout(1, false));
			buttonPanel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP,false, false));
			
			Button btnAddList = createButton(buttonPanel, DialogConstants.ADD_BUTTON_TEXT, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			btnAddList.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AttributeListItem it = new AttributeListItem();
					AttributeItemDialog dd = new AttributeItemDialog(getShell(), it, attributeList, 
							SmartDB.getCurrentConservationArea().getDefaultLanguage());
					int ret = dd.open();
					if (ret == Window.CANCEL){
						return;
					}
					it.setIsActive(true);
					it.setListOrder(attributeList.size());
					attributeList.add(it);
					validate();
					lstAttributeList.refresh();
				}
			});
			
			lstAttributeList.addDoubleClickListener(e->editListItem());
				
				
			final Button btnEditList = createButton(buttonPanel, DialogConstants.EDIT_BUTTON_TEXT, 
					SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			btnEditList.setEnabled(false);
			btnEditList.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					editListItem();
				}
			});
			
			btnDisableListItem = createButton(buttonPanel, DialogConstants.DISABLE_BUTTON_TEXT, 
					SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON));
			btnDisableListItem.setEnabled(false);
			btnDisableListItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					it.setIsActive(!it.getIsActive());
					if (it.getIsActive()){
						btnDisableListItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
					}else{
						btnDisableListItem.setText(DialogConstants.ENABLE_BUTTON_TEXT);
					}
					
					lstAttributeList.refresh();
					validate();
				}
			});
			
			
			btnDeleteListItem = createButton(buttonPanel, DialogConstants.DELETE_BUTTON_TEXT, 
					SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			btnDeleteListItem.setEnabled(false);
			btnDeleteListItem.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					final AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					boolean ret = MessageDialog.openConfirm(getShell(), Messages.AttributeInfoPanel_Delete_DialogTitle, 
							MessageFormat.format(Messages.AttributeInfoPanel_Delete_DialogMessage, 
									new Object[]{it.findName(nameKeyValues.langViewer.getCurrentSelection())}));
					if (!ret){
						return;
					}
					
					runInProgressDialog(new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
							try{
								boolean delete = DataModelManager.INSTANCE.validateDelete(it, monitor, AttributeInfoPanel.this.currentSession);
								if (delete){
									it.setAttribute(null);
									Display.getDefault().asyncExec(new Runnable(){
										@Override
										public void run() {
											attributeList.remove(it);
											lstAttributeList.refresh();
											validate();
										}});

								}
							}catch (final Exception ex){
								Display.getDefault().syncExec(new Runnable(){
									@Override
									public void run() {
										MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.AttributeInfoPanel_DeleteErrorDialogTitle, ex.getLocalizedMessage());
									}});
							}
						}
					});
				}
			});
			
			
			btnMoveUp = createButton(buttonPanel, Messages.AttributeInfoPanel_MoveUpBtn, null);
			btnMoveUp.setEnabled(false);
			btnMoveUp.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					final AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					
					int index = attributeList.indexOf(it);
					if (index < 0) return;
					index --;
					if (index < 0) return;
					attributeList.remove(it);
					attributeList.add(index, it);
					for (int i = 0; i < attributeList.size(); i++){
						((AttributeListItem)attributeList.get(i)).setListOrder(i);
					}
					lstAttributeList.refresh();
					
				}
			});
			
			btnMoveDown = createButton(buttonPanel, Messages.AttributeInfoPanel_MoveDownBtn, null);
			btnMoveDown.setEnabled(false);
			btnMoveDown.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					final AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					
					int index = attributeList.indexOf(it);
					if (index < 0) return;
					index ++;
					if (index == attributeList.size()) return;
					
					attributeList.remove(it);
					attributeList.add(index, it);
					for (int i = 0; i < attributeList.size(); i++){
						((AttributeListItem)attributeList.get(i)).setListOrder(i);
					}
					lstAttributeList.refresh();
				}
			});
			
			btnSort = createButton(buttonPanel, Messages.AttributeInfoPanel_SortButton, null);
			btnSort.setToolTipText(Messages.AttributeInfoPanel_SortButtonTooltip);
			btnSort.setEnabled(true);
			btnSort.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e){
					Comparator<NamedKeyItem> c = new Comparator<NamedKeyItem>(){
						@Override
						public int compare(NamedKeyItem arg0, NamedKeyItem arg1) {
							return Collator.getInstance().compare(arg0.findName(currentDisplayLang).toUpperCase(), arg1.findName(currentDisplayLang).toUpperCase());
						}};
					
					Collections.sort(attributeList, c);
					for (int i = 0; i < attributeList.size(); i ++){
						((AttributeListItem)attributeList.get(i)).setListOrder(i);
					}
					lstAttributeList.refresh();
				}
			});
			
			lstAttributeList.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
					btnDisableListItem.setEnabled(it != null);
					btnDeleteListItem.setEnabled(it != null);
					btnEditList.setEnabled(it != null);
					btnMoveDown.setEnabled(it != null);
					btnMoveUp.setEnabled(it != null);
					if (it != null && it.getIsActive()){
						btnDisableListItem.setText(DialogConstants.DISABLE_BUTTON_TEXT);
						btnDisableListItem.setImage( SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLE_ICON) );
					}else{
						btnDisableListItem.setText(DialogConstants.ENABLE_BUTTON_TEXT);
						btnDisableListItem.setImage( SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLE_ICON) );
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
					AttributeListItem obj = (AttributeListItem) selection.getFirstElement();
					
					AttributeListItem target = (AttributeListItem)getCurrentTarget();
					if (target.equals(obj)){
						return false;
					}
					int index = attributeList.indexOf(obj);
					int toIndex = attributeList.indexOf(target);
					
					if (index == -1 || toIndex == -1) return false;
					attributeList.remove(obj);
					attributeList.add(toIndex, obj);
					for (int i = 0; i < attributeList.size(); i++){
						((AttributeListItem)attributeList.get(i)).setListOrder(i);
					}
				
					return true;
				}
			};
			lstAttributeList.addDropSupport(operations, transferTypes,dropAdapter);
				
		}
		
		/*   Tree Attribute Options */
		treeComposite = new Composite(optionComposite, SWT.NONE);
		treeComposite.setLayout(new GridLayout(1, false));
		treeComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));	
		
		if (canEdit){
			attTree = new AttributeTree(AttributeInfoPanel.this, true);
			Composite tree = attTree.createTree(treeComposite);
			cdAttTree = createDecoration(tree);
			attTree.setListener(new AttributeTree.AttributeTreeChangeListener() {
				@Override
				public void treeModified() {
					validate();	
				}
			});

			nameKeyValues.langViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					attTree.refresh(nameKeyValues.langViewer.getCurrentSelection());
				}
			});
			attTree.refresh(nameKeyValues.langViewer.getCurrentSelection());
		}else{
			attTree = new AttributeTree(AttributeInfoPanel.this, false);
			attTree.createTree(treeComposite);
			attTree.refresh(currentDisplayLang);
		}

		/*   Boolean Attribute Options */
		booleanComposite = new Composite(optionComposite, SWT.NONE);
		booleanComposite.setLayout(new GridLayout(1, false));
		booleanComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		
		/*   Date Attribute Options */
		dateComposite = new Composite(optionComposite, SWT.NONE);
		dateComposite.setLayout(new GridLayout(1, false));
		dateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		/* Geometry Options */
		geometryComposite = new Composite(optionComposite, SWT.NONE);
		geometryComposite.setLayout(new GridLayout(1, false));
		geometryComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		selectOption();
		if (canEdit){
			validate();
		}
		
		scroll.setMinSize(main.computeSize(scroll.getClientArea().width, SWT.DEFAULT));
		scroll.addListener(SWT.Resize, ss->{
				resize();
		});
	}

	private void editListItem() {
		AttributeListItem it = (AttributeListItem)((IStructuredSelection)lstAttributeList.getSelection()).getFirstElement();
		if (it == null) return;
		AttributeItemDialog dd = new AttributeItemDialog(getShell(), it, attributeList,   nameKeyValues.langViewer.getCurrentSelection());
		int ret = dd.open();
		if (ret == Window.CANCEL){
			return;
		}
		validate();
		lstAttributeList.refresh();
	}
	
	private Button createButton(Composite parent, String text, Image icon) {
		Button btnMoveDown = new Button(parent, SWT.NONE);
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnMoveDown.setText(text);
		btnMoveDown.setImage(icon);
		btnMoveDown.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		return btnMoveDown;
	}
	
	private void resize() {
		main.layout(true);
		scroll.setMinSize(main.computeSize(scroll.getClientArea().width, SWT.DEFAULT));
		
	}
	public NameKeyComposite getNameKeyComposite(){
		return this.nameKeyValues;
	}
	
	/**
	 * Run a taks in a progress monitor
	 * @param runnable
	 */
	private void runInProgressDialog(IRunnableWithProgress runnable){
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());
		try {
			dialog.run(true, true, runnable);		
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.AttributeInfoPanel_Error_Message, ex);
		}
	}
	
	public Attribute.AttributeType getSelectedType(){
		return (AttributeType) cmbType.getStructuredSelection().getFirstElement();
	}
	/**
	 * Validates attribute input
	 * @return <code>true</code> if all fields validate correctly, <code>false</code> if error exists
	 */
	public boolean validate(){
		boolean error = nameKeyValues.validate();
				
		Attribute.AttributeType type = (Attribute.AttributeType)(((IStructuredSelection)cmbType.getSelection()).getFirstElement());
		if (type.equals(AttributeType.BOOLEAN)){
			
		}else if (type.equals(AttributeType.NUMERIC)){
			if (cdAttTree != null){
				cdAttTree.hide();
			}
			cdAttList.hide();
			Double min = null;
			Double max = null;
			boolean maxer = false;
			boolean miner = false;
			if (txtMaxValue.getText().length() > 0){
				try{
					max = Double.valueOf(txtMaxValue.getText());
				}catch (Exception ex){
					error = true;
					cdMaxValue.show();
					cdMaxValue.setDescriptionText(Messages.AttributeInfoPanel_Error_InvalidMaxValue);
					maxer = true;
				}
			}
			if (txtMinValue.getText().length() > 0){
				try{
					min = Double.valueOf(txtMinValue.getText());
				}catch (Exception ex){
					error = true;
					cdMinValue.show();
					cdMinValue.setDescriptionText(Messages.AttributeInfoPanel_Error_InvalidMinValue);
					miner = true;
				}
			}
			if (min != null && max != null && min > max){
				cdMaxValue.show();
				cdMaxValue.setDescriptionText(Messages.AttributeInfoPanel_Error_MaxValueToBig);
				error = true;
				maxer = true;
			}
			if (!maxer){
				cdMaxValue.hide();
			}
			if (!miner){
				cdMinValue.hide();
			}
			
			String decimal = txtDecimal.getText().trim();
			cdDecimal.hide();
			if (!decimal.isBlank()) {
				try {
					int dd = Integer.parseInt(decimal);
					if (dd < 0 || dd > 30) {
						cdDecimal.show();
						cdDecimal.setDescriptionText(Messages.AttributeInfoPanel_InvalidDecimalPlaces);
						error = true;
					}
				}catch (Exception ex) {
					cdDecimal.show();
					cdDecimal.setDescriptionText(Messages.AttributeInfoPanel_InvalidDecimalPlaces);
					error = true;
				}
			}
			
			
		}else if (type.equals(AttributeType.TEXT)){
			
			String text = txtRegex.getText();
			try {
				Pattern.compile(text);
				cdRegex.hide();
				cdRegex.setDescriptionText(""); //$NON-NLS-1$
			}catch (Exception ex) {
				cdRegex.show();
				cdRegex.setDescriptionText(Messages.AttributeInfoPanel_InvalidRegex);
				error = true;
			}
			
			cdMinValue.hide();
			cdMaxValue.hide();
			if (cdAttTree != null){
				cdAttTree.hide();
			}
			cdAttList.hide();
		}else if (type.isList()){
			if (cdAttTree != null){
				cdAttTree.hide();
			}
			cdMinValue.hide();
			cdMaxValue.hide();
			
			if (this.attributeList.size() == 0){
				cdAttList.setDescriptionText(Messages.AttributeInfoPanel_Error_OneListItemRequired);
				cdAttList.show();
				error = true;
			}else{
				cdAttList.hide();
			}
		}else if (type.equals(AttributeType.TREE)){
			cdMinValue.hide();
			cdMaxValue.hide();
			cdAttList.hide();
			if (this.attTree != null){
				if (this.attTree.getRootNodes() == null || this.attTree.getRootNodes().size() == 0){
					cdAttTree.setDescriptionText(Messages.AttributeInfoPanel_Error_OneTreeNodeRequired);
					cdAttTree.show();
					error = true;
				}else{
					cdAttTree.hide();
				}
			}
		}else if (type.isGeometry()) {
			cdMaxValue.hide();
			cdMaxValue.hide();
			cdAttList.hide();
			if (cdAttTree != null) cdAttTree.hide();
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
		}else if (type.isList()){
			((StackLayout)optionComposite.getLayout()).topControl = listComposite;
		}else if (type.equals(AttributeType.TREE)){
			((StackLayout)optionComposite.getLayout()).topControl = treeComposite;
		}else if (type.equals(AttributeType.DATE)){
			((StackLayout)optionComposite.getLayout()).topControl = dateComposite;
		}else if (type.isGeometry()){
			((StackLayout)optionComposite.getLayout()).topControl = geometryComposite;
		}
		optionComposite.layout();
	}

	/*
	 * Creates a control decoration for a wizard page field.
	 */
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	/**
	 * Updates the fields with the values from the associated attribute
	 * @param att attribute
	 * @param language current display language
	 */
	public void setAttribute(Attribute att, Collection<? extends NamedKeyItem> siblings, Language language){
		nameKeyValues.initFields(att, siblings, language);
		
		this.currentDisplayLang = language;
	
		chRequired.setSelection(att.getIsRequired());
		
		boolean canconvert = false;
		
		if (att.getUuid() != null){
			cmbType.getControl().setEnabled(false);
			if (att.getType() == Attribute.AttributeType.LIST) canconvert = true;
		}
		if (btnConvert != null) {
			if (canconvert) {
				btnConvert.setVisible(true);
				((GridData)btnConvert.getLayoutData()).widthHint = btnConvert.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				btnConvert.getParent().layout(true,true);
			}else {
				btnConvert.setVisible(false);
				((GridData)btnConvert.getLayoutData()).widthHint = 0;
				btnConvert.getParent().layout(true,true);
			}
		}
			
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
				txtMaxValue.setText(""); //$NON-NLS-1$
				txtMinValue.setText(""); //$NON-NLS-1$
				if (att.getMaxValue() != null) {
					txtMaxValue.setText(String.valueOf(att.getMaxValue()));
				}
				if (att.getMinValue() != null) {
					txtMinValue.setText(String.valueOf(att.getMinValue()));
				}
				if (att.getRegex() != null) {
					txtDecimal.setText(att.getRegex());
				}else {
					txtDecimal.setText(""); //$NON-NLS-1$
				}
			} else if (att.getType().equals(Attribute.AttributeType.TEXT)) {
				textComposite.setVisible(true);
				txtRegex.setText(""); //$NON-NLS-1$
				if (att.getRegex() != null) {
					txtRegex.setText(att.getRegex());
				}
			} else if (att.getType().isList()) {
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

				//create a cloned copy for working with
				ArrayList<AttributeListItem> items = new ArrayList<AttributeListItem>();
				for (AttributeListItem item : att.getAttributeList()){
					AttributeListItem clone = item.clone(att, att.getConservationArea(), null, att.getConservationArea().getDefaultLanguage().getCode());
					clone.setUuid(item.getUuid());
					clone.setIcon(item.getIcon());
					items.add(clone);
				}
				
				attributeList = new ArrayList<NamedKeyItem>(items);
				lstAttributeList.setInput(attributeList);

			} else if (att.getType().equals(Attribute.AttributeType.TREE)) {
				treeComposite.setVisible(false);
				
			}
		}
		
		if(attTree != null){
			attTree.setInput(att, currentSession);
			attTree.refresh(currentDisplayLang);
		}
		selectOption();
		
		if (canEdit){
			//can edit
			validate();
		}
		iconPanel.setIcon(att.getIcon());
		resize();
	}
	
	/*
	 * clears attribute list
	 */
	private void clearAttributeList(Attribute att){
		if (att.getAttributeList()== null){
			return;
		}
		for (AttributeListItem it : att.getAttributeList()){
			it.setAttribute(null);
		}
		att.getAttributeList().clear();
	}
	
	/*
	 * clears all tree nodes
	 */
	private void clearAttributeTree(Attribute att){
		if (att.getTree() == null){
			return;
		}

		List<AttributeTreeNode> toprocess = new ArrayList<AttributeTreeNode>();
		for(AttributeTreeNode node : att.getTree()){
			toprocess.add(node);
		}
		while(toprocess.size() > 0){
			AttributeTreeNode node = toprocess.remove(0);
			node.setAttribute(null);
			node.setParent(null);
			if (node.getChildren() != null){
				toprocess.addAll(node.getChildren());
				node.setChildren(null);
			}
			node.getChildren().clear();
		}
		att.getTree().clear();
	}
	
	/**
	 * Updates the given attribute with the contents of the 
	 * gui components.
	 * @param <T>
	 * 
	 * @param att attribute to update
	 */
	public <T> void updateAttribute(Attribute att, final Session session){
		try {
			boolean convert = false;
			nameKeyValues.updateFields(att);
			
			if (att.getType() == Attribute.AttributeType.LIST &&
				((Attribute.AttributeType)cmbType.getStructuredSelection().getFirstElement() == Attribute.AttributeType.MLIST)){
				convert = true;
			}
			
			att.setType(  (Attribute.AttributeType)((IStructuredSelection)cmbType.getSelection()).getFirstElement() );
			att.setIsRequired(chRequired.getSelection());
			att.setIcon(iconPanel.getIcon());
						
			if (att.getUuid() == null)session.persist(att);
			HibernateManager.saveOrMerge(session, att.getIcon());
						
			session.flush();
						
			if (att.getType().equals(Attribute.AttributeType.NUMERIC)){
				att.setMaxValue(null);
				att.setMinValue(null);
				att.setRegex(null);
				clearAttributeTree(att);
				clearAttributeList(att);
				
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
				if (!txtDecimal.getText().isBlank()) {
					att.setRegex(txtDecimal.getText());
				}
			}else if (att.getType().equals(Attribute.AttributeType.BOOLEAN)){
				att.setAggregations(null);
				att.setMaxValue(null);
				att.setMinValue(null);
				att.setRegex(null);
				clearAttributeTree(att);
				clearAttributeList(att);
			}else if (att.getType().equals(Attribute.AttributeType.TEXT)){
				att.setAggregations(null);
				att.setMaxValue(null);
				att.setMinValue(null);
				att.setRegex(null);
				clearAttributeTree(att);
				clearAttributeList(att);
				att.setRegex(txtRegex.getText());
			}else if (att.getType().isList()){
				att.setAggregations(null);
				att.setMaxValue(null);
				att.setMinValue(null);
				att.setRegex(null);
				clearAttributeTree(att);
				
				if (att.getAttributeList() == null){
					att.setAttributeList(new ArrayList<AttributeListItem>());
				}else{
					for (Iterator<AttributeListItem> iterator = att.getAttributeList().iterator(); iterator.hasNext();) {
						AttributeListItem oldItem = (AttributeListItem) iterator.next();
						
						if (!attributeList.contains(oldItem)){
							
							//item deleted
							try{
								DataModelManager.INSTANCE.fireDeleteListener(session, oldItem);
							}catch (Exception ex){
								SmartPlugIn.displayLog(Messages.AttributeInfoPanel_ListModificationError + ex.getMessage(), ex); 
								return;
							}
							oldItem.setAttribute(null);
							iterator.remove();
						}
					}
					session.flush();
				}
				for (int i = 0; i < attributeList.size(); i ++){
					AttributeListItem item = (AttributeListItem) attributeList.get(i);
					HibernateManager.saveOrMerge(session,  item.getIcon());
					
					item.setListOrder(i);
					item.setAttribute(att);
					
					if (item.getUuid() != null){
						item = (AttributeListItem) session.merge(item);	
					}else{
						//new item
						session.persist(item);
						DataModelManager.INSTANCE.fireAddListener(session, item);
						att.getAttributeList().add(item);
					}
					for ( org.wcs.smart.ca.Label l : item.getNames()){
						l.setElement(item);
					}
				}
				Collections.sort(att.getAttributeList(), new Comparator<AttributeListItem>() {
	
					@Override
					public int compare(AttributeListItem o1, AttributeListItem o2) {
						return ((Integer)o1.getListOrder()).compareTo(o2.getListOrder());
					}
				});
				
				session.flush();
			}else if (att.getType().equals(Attribute.AttributeType.TREE)){
				att.setAggregations(null);
				att.setMaxValue(null);
				att.setMinValue(null);
				att.setRegex(null);
				clearAttributeList(att);
				if(attTree != null){
					final Attribute thisAttribute = att;
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
					try {
						pmd.run(true, false, new IRunnableWithProgress() {
							
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException,
									InterruptedException {
								monitor.setTaskName(Messages.AttributeInfoPanel_SavingProgressMessage);
								
								for(AttributeTreeNode toDelete : attTree.getDeletedNodes()){
									try{
										if (toDelete.getUuid() != null){
											
											AttributeTreeNode item = session.get(toDelete.getClass(), toDelete.getUuid());
											if (item.getParent() != null) {
												item.getParent().getChildren().remove(item);
												item.getParent().getActiveChildren().remove(item);
											}else {
												item.getAttribute().getTree().remove(item);
												item.getAttribute().getActiveTreeNodes().remove(item);
											}
											
											session.remove(item);
											
											DataModelManager.INSTANCE.fireDeleteListener(currentSession, toDelete);
										}
									}catch (Exception ex){
										throw new InvocationTargetException(ex);
									}
								}
								attTree.clearDeletedNodes();
								
								if (thisAttribute.getTree() == null){
									thisAttribute.setTree(new ArrayList<AttributeTreeNode>());
								}else{
									thisAttribute.getTree().clear();
								}
								List<AttributeTreeNode> root = attTree.getRootNodes();
								if (root != null){
									for (AttributeTreeNode n : root){
										AttributeTreeNode mergedNode = updateAttributeTreeNode(thisAttribute, n, session);
										thisAttribute.getTree().add(mergedNode);
									}
								}
								session.flush();
								//icons
								ITreeNodeVisitor v = node-> {
									if (node.getIcon() != null) {
										if (node.getIcon().getUuid() != null) {
											Icon merged = (Icon) session.merge(node.getIcon());
											node.setIcon(merged);
										}
									}
									return true;
								};
								thisAttribute.getTree().forEach(n->n.accept(v));
							
								
								session.flush();
							}
						});
					} catch (Exception ex) {
						SmartPlugIn.displayLog(Messages.AttributeInfoPanel_SaveErrorMessage, ex);
					}
					
				}
			}
			session.flush();
			
			if (convert) {
				try {
					DataModelManager.INSTANCE.fireSingleToMulti(session, att);
				}catch (Exception ex) {
					att.setType(Attribute.AttributeType.LIST);
					throw ex;
				}
			}
		}catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.AttributeInfoPanel_SaveError, ex);

		}
	}
	

	private AttributeTreeNode updateAttributeTreeNode(Attribute newAttribute, AttributeTreeNode node, Session session){
		
		HibernateManager.saveOrMerge(session, node.getIcon());
		
		node.setAttribute(newAttribute);
		if (node.getUuid() == null) {
			DataModelManager.INSTANCE.fireAddListener(currentSession, node);
			session.persist(node);
		}
		
		for ( org.wcs.smart.ca.Label l : node.getNames()){
			l.setElement(node);
		}
		
		List<AttributeTreeNode> kids = new ArrayList<AttributeTreeNode>();
		if (node.getChildren() != null){
			for (AttributeTreeNode child : node.getChildren()){
				AttributeTreeNode n = updateAttributeTreeNode(newAttribute, child, session);
				kids.add(n);
			}
		}

		node = (AttributeTreeNode) session.merge(node);
		node.getChildren().clear();
		node.getChildren().addAll(kids);
		
		return node;
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
	class AttributeListLabelProvider extends ColumnLabelProvider {
		@Override
		public String getText(Object element) {
			AttributeListItem it = (AttributeListItem) element;
			Language lang = currentDisplayLang;
			if (nameKeyValues.langViewer != null){
				lang = nameKeyValues.langViewer.getCurrentSelection();
			}
			String name = it.findNameNull(lang);
			if (name == null){
				name = it.getName();
			}
			return name + " [" + it.getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			

		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
		 */
		@Override
		public Color getForeground(Object element) {
			if (!lstAttributeList.getControl().isEnabled()) return null;
			
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

