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
package org.wcs.smart.asset.ui.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;
import org.wcs.smart.asset.model.mapping.XmpMetadataField;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.TreeDropDown;

/**
 * Composite for mapping metadata fields to data model elements. 
 * 
 * @author Emily
 *
 */
public abstract class AbstractNewMappingComposite {

	protected NewMappingDialog dialog;
	
	protected ComboViewer cmbExifMappingField;
	protected TableViewer tblExifValueMapping;
	
	protected Button btnExifMulti;
	protected Button btnExifSingle;

	protected List<MetadataValueMapping> exifTagValueMappings;
	
	public AbstractNewMappingComposite(NewMappingDialog dialog) {
		this.dialog = dialog;	
		exifTagValueMappings = new ArrayList<>();
	}

	protected void modified() {
		dialog.modified();
	}
	
	public String validate() {
		String message = null;
		if (btnExifSingle.getSelection()) {
			Object mappedTo = cmbExifMappingField.getStructuredSelection().getFirstElement();
			if (!(mappedTo instanceof AssetMetadataMapping.AssetProperty)) {
				message = "EXIF mapping to value must be selected";
			}
		}else if (btnExifMulti.getSelection()) {
			if (exifTagValueMappings.isEmpty()) {
				message = "At least one data model element must be mapped.";
			}
			for (MetadataValueMapping mapping : exifTagValueMappings) {
				if (mapping.category == null && mapping.attribute == null && mapping.listItem == null && mapping.treeNode == null) 
					message = "Mapped data model element must be selected";
			}
		}
		return message;
	}
	
	
	public abstract List<AssetMetadataMapping> getMappings();
	
	public abstract Composite createPanel(Composite parent);
	
	protected Composite createMappingPanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		btnExifSingle = new Button(panel, SWT.RADIO);
		btnExifSingle.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnExifSingle.setText("Single Mapping");
		btnExifSingle.setText("Map tag to field");
		btnExifSingle.addListener(SWT.Selection, e->dialog.modified());
		
		cmbExifMappingField = new ComboViewer(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbExifMappingField.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)cmbExifMappingField.getControl().getLayoutData()).horizontalIndent = 30;
		cmbExifMappingField.setContentProvider(ArrayContentProvider.getInstance());
		cmbExifMappingField.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping.AssetProperty) return ((AssetMetadataMapping.AssetProperty) element).name();
				return super.getText(element);
			}
		});
		cmbExifMappingField.addSelectionChangedListener(e->modified());
		cmbExifMappingField.setInput( AssetMetadataMapping.AssetProperty.values());
		
		
		btnExifMulti = new Button(panel, SWT.RADIO);
		btnExifMulti.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		btnExifMulti.setText("Map to data model");
		btnExifMulti.setToolTipText("Map individual tag values to specific data model elements");
		btnExifMulti.addListener(SWT.Selection, e->dialog.modified());
		
		Composite valuePart = new Composite(panel, SWT.NONE);
		valuePart.setLayout(new GridLayout(2, false));
		valuePart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		Label l = new Label(valuePart, SWT.WRAP);
		l.setText("For numeric, text, boolean, and date attributes leave the tag value blank, the value will be interpretted as the observation value.");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridData)l.getLayoutData()).horizontalIndent = 30;
		((GridData)l.getLayoutData()).widthHint = 200;
		
		tblExifValueMapping = new TableViewer(valuePart, SWT.BORDER | SWT.FULL_SELECTION);
		
		TableViewerEditor.create(tblExifValueMapping , new ColumnViewerEditorActivationStrategy( tblExifValueMapping ) {
            protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
                return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL ||
                		event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION ||
                		event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION;
            }
        }, ColumnViewerEditor.TABBING_MOVE_TO_ROW_NEIGHBOR );
		
		tblExifValueMapping.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblExifValueMapping.getControl().getLayoutData()).horizontalIndent = 30;
		((GridData)tblExifValueMapping.getControl().getLayoutData()).heightHint = 150;
		((GridData)tblExifValueMapping.getControl().getLayoutData()).widthHint = 400;
		tblExifValueMapping.setContentProvider(ArrayContentProvider.getInstance());
		tblExifValueMapping.getTable().setLinesVisible(true);
		tblExifValueMapping.getTable().setHeaderVisible(true);
		
		TableViewerColumn colTag = new TableViewerColumn(tblExifValueMapping, SWT.NONE);
		colTag.getColumn().setText("Tag Value");
		colTag.getColumn().setWidth(150);
		colTag.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MetadataValueMapping) return ((MetadataValueMapping) element).tagValue;
				return super.getText(element);
			}
		});
		colTag.setEditingSupport(new EditingSupport(tblExifValueMapping) {
			
			private TextCellEditor cellEditor = new TextCellEditor(tblExifValueMapping.getTable());
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof MetadataValueMapping) {
					((MetadataValueMapping)element).tagValue = (String)value;
					tblExifValueMapping.refresh();
					dialog.modified();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof MetadataValueMapping) {
					return ((MetadataValueMapping) element).tagValue;
				}
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				return cellEditor;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		TableViewerColumn colMapping = new TableViewerColumn(tblExifValueMapping, SWT.NONE);
		colMapping.getColumn().setText("Data Model Mapping");
		colMapping.getColumn().setWidth(400);
		colMapping.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MetadataValueMapping) {
					MetadataValueMapping v = (MetadataValueMapping)element;
					return v.toString();
				}
				return super.getText(element);
			}
		});
		colMapping.setEditingSupport(new DataModelCellEditor(tblExifValueMapping));
		
		tblExifValueMapping.setInput(exifTagValueMappings);
		tblExifValueMapping.getTable().setEnabled(false);
		
		Composite btnPanel = new Composite(valuePart, SWT.NONE);
		btnPanel.setLayout(new GridLayout());
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		Button btnAdd = new Button(btnPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnAdd.addListener(SWT.Selection,  e->addTagMapping());
		btnAdd.setEnabled(false);
		
		Button btnRemove = new Button(btnPanel, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.addListener(SWT.Selection, e->removeExifValueMappings());
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRemove.setEnabled(false);
		
		Menu mm = new Menu(tblExifValueMapping.getTable());
		
		MenuItem miAdd = new MenuItem(mm, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->addTagMapping());
		
		MenuItem miDelete = new MenuItem(mm, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->removeExifValueMappings());
		
		tblExifValueMapping.getControl().setMenu(mm);
		mm.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				miDelete.setEnabled(!tblExifValueMapping.getSelection().isEmpty());
			}
			@Override
			public void menuHidden(MenuEvent e) { }
		});
		
		Listener listener = e->{
			cmbExifMappingField.getControl().setEnabled(btnExifSingle.getSelection());
			tblExifValueMapping.getControl().setEnabled(!btnExifSingle.getSelection());
			btnAdd.setEnabled(!btnExifSingle.getSelection());
			btnRemove.setEnabled(!btnExifSingle.getSelection());
		};
		btnExifSingle.addListener(SWT.Selection, listener);
		btnExifMulti.addListener(SWT.Selection, listener);

		btnExifSingle.setSelection(true);
		
		if (dialog.getEditItem() != null) {
			btnExifSingle.setEnabled(false);
			btnExifMulti.setEnabled(false);
			if (dialog.getEditItem().getMappedAssetProperty() != null) {
				btnExifSingle.setSelection(true);
				btnExifMulti.setSelection(false);
				
				cmbExifMappingField.setSelection(new StructuredSelection(dialog.getEditItem().getMappedAssetProperty()));
				tblExifValueMapping.getControl().setEnabled(false);
				cmbExifMappingField.getControl().setEnabled(true);
			}else if (dialog.getEditItem().getMappedAssetProperty() == null) {
				btnExifSingle.setSelection(false);
				btnExifMulti.setSelection(true);
				
				MetadataValueMapping mapping = new MetadataValueMapping();
				if (dialog.getEditItem().getMetadataField() instanceof ExifMetadataField) {
					mapping.tagValue = ((ExifMetadataField)dialog.getEditItem().getMetadataField()).getTagValue();
				}else if (dialog.getEditItem().getMetadataField() instanceof XmpMetadataField) {
					mapping.tagValue = ((XmpMetadataField)dialog.getEditItem().getMetadataField()).getValue();
				}
				mapping.category = dialog.getEditItem().getMappedCategory();
				mapping.attribute = dialog.getEditItem().getMappedAttribute();
				mapping.listItem = dialog.getEditItem().getMappedListItem();
				mapping.treeNode = dialog.getEditItem().getMappedTreeNode();
				
				exifTagValueMappings.add(mapping);
				tblExifValueMapping.refresh();
				tblExifValueMapping.getControl().setEnabled(true);
				cmbExifMappingField.getControl().setEnabled(false);
				
				btnAdd.setEnabled(true);
				btnRemove.setEnabled(true);
				
			}
		}
		
		return panel;
	}
	
	private void addTagMapping() {
		MetadataValueMapping mm = new MetadataValueMapping();
		mm.tagValue = null;
		exifTagValueMappings.add(mm);
		tblExifValueMapping.refresh();
		dialog.modified();
	}

	private void removeExifValueMappings() {
		List<MetadataValueMapping> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = tblExifValueMapping.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof MetadataValueMapping) toDelete.add((MetadataValueMapping) x);
		}
		this.exifTagValueMappings.removeAll(toDelete);
		tblExifValueMapping.refresh();
		dialog.modified();
	}
	

	protected class MetadataValueMapping{
		String tagValue;
		Category category;
		Attribute attribute;
		AttributeListItem listItem;
		AttributeTreeNode treeNode;
		
		public String toString() {
			if (attribute == null && category == null) return "Select Data Model Element";
			
			StringBuilder sb = new StringBuilder();
			if (treeNode != null) sb.append(treeNode.getName());
			if (listItem != null) sb.append(listItem.getName());
			if (attribute != null) {
				if (sb.length() != 0) sb.append( " (" + attribute.getName() + ")");
				else sb.append(attribute.getName());
				
			}
			if (category != null) {
				if (sb.length() != 0) sb.append( " [" + category.getFullCategoryName() + "]");
				else sb.append(category.getFullCategoryName());
			}
			return sb.toString();
		}
	}
	
	private void runEventLoop(Shell loopShell) {

		//Use the display provided by the shell if possible
		Display display = loopShell.getDisplay();
		while (loopShell != null && !loopShell.isDisposed()) {
			try {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		if (!display.isDisposed()) display.update();
	}
	
	private class DataModelCellEditor extends EditingSupport{

		
		DialogCellEditor ce = new DialogCellEditor(tblExifValueMapping.getTable()) {
			
			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
				TreeDropDown dd = new TreeDropDown(dialog.getShell());
				dd.getTreeViewer().setContentProvider(new DmContentProvider());
				dd.getTreeViewer().setLabelProvider(new DataModelLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ListNodeWrapper) return super.getText(((ListNodeWrapper) element).list);
						if (element instanceof TreeNodeWrapper) return super.getText(((TreeNodeWrapper)element).node);
						return super.getText(element);
					}
				});
				dd.getTreeViewer().setInput(dialog.getCachedDataModel());
				final Object[] data = {null};
				dd.positionAndShow(cellEditorWindow,400, 400, new ISelectionListener() {
					@Override
					public void selectionChanged(IWorkbenchPart part, ISelection selection) {
						data[0] = ((IStructuredSelection)selection).getFirstElement();
						dd.dispose();
					}
				});
				runEventLoop(dd.getTreeViewer().getControl().getShell());
				return data[0];
			}
		};
		
		public DataModelCellEditor(ColumnViewer viewer) {
			super(viewer);
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return ce;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
			if (element instanceof MetadataValueMapping) return element.toString();
			return null;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (value == null) return;
			if (element instanceof MetadataValueMapping && (
					value instanceof Category ||
					value instanceof Attribute ||
					value instanceof CategoryAttribute ||
					value instanceof AttributeListItem ||
					value instanceof AttributeTreeNode ||
					value instanceof ListNodeWrapper ||
					value instanceof TreeNodeWrapper)) {
				MetadataValueMapping mm = (MetadataValueMapping)element;
				mm.attribute = null;
				mm.listItem = null;
				mm.treeNode = null;
				mm.category = null;
				
				if (value instanceof Category) {
					mm.category = (Category) value;
				}else if (value instanceof Attribute) {
					mm.attribute= (Attribute) value;
				}else if (value instanceof CategoryAttribute) {
					mm.category = ((CategoryAttribute)value).getCategory();
					mm.attribute = ((CategoryAttribute)value).getAttribute();
				}else if (value instanceof AttributeListItem) {
					mm.attribute = ((AttributeListItem) value).getAttribute();
					mm.listItem = (AttributeListItem) value;
				}else if (value instanceof AttributeTreeNode) {
					mm.attribute = ((AttributeTreeNode) value).getAttribute();
					mm.treeNode = (AttributeTreeNode) value;
				}else if (value instanceof ListNodeWrapper) {
					mm.category = ((ListNodeWrapper) value).category;
					mm.attribute = ((ListNodeWrapper) value).list.getAttribute();
					mm.listItem = ((ListNodeWrapper) value).list;
				}else if (value instanceof TreeNodeWrapper) {
					mm.category = ((TreeNodeWrapper) value).category;
					mm.attribute = ((TreeNodeWrapper) value).node.getAttribute();
					mm.treeNode = ((TreeNodeWrapper) value).node;
				}
				tblExifValueMapping.refresh();
				dialog.modified();
			}
		}
	}
	
	
	private class ListNodeWrapper {
		public ListNodeWrapper(AttributeListItem list, Category c) {
			this.list = list;
			this.category = c;
		}
		AttributeListItem list;
		Category category;
	}
	private class TreeNodeWrapper {
		
		public TreeNodeWrapper(AttributeTreeNode node, Category c, Object parent) {
			this.node = node;
			this.category = c;
			this.parent = parent;
		}
		
		AttributeTreeNode node;
		Category category;
		Object parent;
	}
	
	private class DmContentProvider extends DataModelContentProvider{
		public DmContentProvider() {
			super(false, false, true);
		}
		
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof DataModel) {
				List<Object> items = new ArrayList<Object>();
				items.addAll(((DataModel)inputElement).getCategories());
				items.addAll(((DataModel)inputElement).getAttributes());
				return items.toArray();
			}
			return super.getElements(inputElement);
		}
		
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof CategoryAttribute) {
				AttributeType type = ((CategoryAttribute)parentElement).getAttribute().getType(); 
				Category category = ((CategoryAttribute)parentElement).getCategory();
				if (type == AttributeType.LIST) {
					List<AttributeListItem> items = ((CategoryAttribute) parentElement).getAttribute().getAttributeList();
					Object[] data = new Object[items.size()];
					int j = 0;
					for (AttributeListItem i : items) {
						data[j++] = new ListNodeWrapper(i, category);
					}
					return data;
				}else if (type == AttributeType.TREE) {
					List<AttributeTreeNode> items = ((CategoryAttribute) parentElement).getAttribute().getTree();
					Object[] data = new Object[items.size()];
					int j = 0;
					for (AttributeTreeNode i : items) {
						data[j++] = new TreeNodeWrapper(i, category, parentElement);
					}
					return data;
				}
			}else if (parentElement instanceof Attribute) {
				AttributeType type = ((Attribute)parentElement).getType();
				if (type == AttributeType.LIST) {
					return ((Attribute)parentElement).getAttributeList().toArray();
				}else if (type == AttributeType.LIST) {
					return ((Attribute)parentElement).getTree().toArray();
				}
			}else if (parentElement instanceof AttributeTreeNode) {
				((AttributeTreeNode)parentElement).getChildren().toArray();
			}else if (parentElement instanceof TreeNodeWrapper) {
				TreeNodeWrapper w = (TreeNodeWrapper)parentElement;
				List<AttributeTreeNode> items = w.node.getChildren();
				Object[] data = new Object[items.size()];
				int j = 0;
				for (AttributeTreeNode i : items) {
					data[j++] = new TreeNodeWrapper(i, w.category, parentElement);
				}
				return data;
			}
			return super.getChildren(parentElement);
		}
		
		public Object getParent(Object element) {
			if (element instanceof ListNodeWrapper) {
				return ((ListNodeWrapper) element).list.getAttribute();
			}else if (element instanceof AttributeListItem) {
				return ((AttributeListItem) element).getAttribute();
			}else if (element instanceof AttributeTreeNode) {
				if (((AttributeTreeNode) element).getParent() == null) {
					return ((AttributeTreeNode) element).getAttribute();
				}else {
					return ((AttributeTreeNode) element).getParent();
				}
			}else if (element instanceof TreeNodeWrapper) {
				return ((TreeNodeWrapper)element).parent;
			}
			return super.getParent(element);
		}
		
		@Override
		public boolean hasChildren(Object element) {
			Attribute.AttributeType type = null;
			if (element instanceof CategoryAttribute) {
				type = ((CategoryAttribute)element).getAttribute().getType(); 
			}else if (element instanceof Attribute) {
				type = ((Attribute)element).getType();
			}
			if (type != null) {
				return type == AttributeType.LIST || type == AttributeType.TREE;
			}
			if (element instanceof AttributeTreeNode) {
				return !((AttributeTreeNode) element).getChildren().isEmpty();
			}
			if (element instanceof TreeNodeWrapper) {
				return !((TreeNodeWrapper) element).node.getChildren().isEmpty();
			}
			return super.hasChildren(element);
		}
	}
}
