package org.wcs.smart.asset.ui.metadata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.TreeDropDown;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifImageDirectory;
import com.drew.metadata.exif.ExifInteropDirectory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class NewMappingExif {

	private NewMappingDialog dialog;
	
	private Text txtExifTag;
	private Text txtExifTagTxt;
	
	private ComboViewer cmbExifMappingField;
	private TableViewer tblExifValueMapping;
	
	private Button btnExifMulti;
	private Button btnExifSingle;

	private List<ExifValueMapping> exifTagValueMappings;
	
	public NewMappingExif(NewMappingDialog dialog) {
		this.dialog = dialog;
		exifTagValueMappings = new ArrayList<>();
		
	}

	private void modified() {
		dialog.modified();
	}
	
	public String validate() {
		String message = null;
//		String directory = cmbExifDirectory.getCombo().getText().trim();
//		if (directory.isEmpty()) {
//			message = "EXIF directory field cannot be empty";
//		}
		String tag = txtExifTag.getText().trim();
		if (tag.isEmpty()) {
			return "EXIF tag field cannot be empty";
		}
		try {
			Integer tagNum = Integer.parseInt(tag);
		}catch (Exception ex) {
			return "EXIF tag is not valid (must be a number)";
		}
		
		if (btnExifSingle.getSelection()) {
			Object mappedTo = cmbExifMappingField.getStructuredSelection().getFirstElement();
			if (!(mappedTo instanceof AssetMetadataMapping.AssetProperty)) {
				message = "EXIF mapping to value must be selected";
			}
		}else if (btnExifMulti.getSelection()) {
			if (exifTagValueMappings.isEmpty()) {
				message = "At least one data model element must be mapped.";
			}
			for (ExifValueMapping mapping : exifTagValueMappings) {
				if (mapping.category == null && mapping.attribute == null && mapping.listItem == null && mapping.treeNode == null) 
					message = "Mapped data model element must be selected";
			}
		}
		return message;
	}
	
	
	public List<AssetMetadataMapping> getMappings() {
		Integer tagNum = Integer.parseInt(txtExifTag.getText());
		
		if (btnExifSingle.getSelection()) {
			Object x = cmbExifMappingField.getStructuredSelection().getFirstElement();
			//TODO:
			if (x instanceof AssetMetadataMapping.AssetProperty) {
				ExifMetadataField field = new ExifMetadataField(tagNum);
				AssetMetadataMapping map = new AssetMetadataMapping();
				map.setConservationArea(SmartDB.getCurrentConservationArea());
				map.setMetadataType(AssetMetadataMapping.MetadataType.EXIF);
				map.setMappedAssetProperty((AssetMetadataMapping.AssetProperty)x);
				map.setMetadataKey(field);
				return Collections.singletonList(map);
			}
		}else if (btnExifMulti.getSelection()) {
			List<AssetMetadataMapping> mappings = new ArrayList<>();
			
			for (ExifValueMapping m : exifTagValueMappings) {
				ExifMetadataField field = new ExifMetadataField(tagNum, m.tagValue);
				AssetMetadataMapping map = new AssetMetadataMapping();
				map.setConservationArea(SmartDB.getCurrentConservationArea());
				map.setMetadataType(AssetMetadataMapping.MetadataType.EXIF);
				map.setMappedAttribute(m.attribute);
				map.setMappedCategory(m.category);
				map.setMappedListItem(m.listItem);
				map.setMappedTreeNode(m.treeNode);
				map.setMetadataKey(field);
				mappings.add(map);
			}
			return mappings;
		}
		return Collections.emptyList();
	}
	
	public Composite createExifPanel(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		
//		Label l = new Label(panel, SWT.NONE);
//		l.setText("EXIF Directory:");
//		
//		cmbExifDirectory = new ComboViewer(panel, SWT.DROP_DOWN | SWT.READ_ONLY);
//		cmbExifDirectory.setLabelProvider(new LabelProvider() {
//			@Override
//			public String getText(Object element) {
//				if (element instanceof Directory) return ((Directory) element).getName();
//				return super.getText(element);
//			}
//		});
//		cmbExifDirectory.setContentProvider(ArrayContentProvider.getInstance());
//		cmbExifDirectory.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//		cmbExifDirectory.setInput(exifDirectories);
//		cmbExifDirectory.getControl().addListener(SWT.Modify, e->modified());
//		cmbExifDirectory.getControl().addListener(SWT.Selection, e->modified());
		
		Label l = new Label(panel, SWT.NONE);
		l.setText("EXIF Tag (Hex):");
		
		txtExifTag = new Text(panel, SWT.BORDER);
		txtExifTag.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtExifTag.addListener(SWT.Modify, e->modified());
		
		l = new Label(panel, SWT.NONE);
		l.setText("EXIF Tag Name):");
		
		txtExifTagTxt = new Text(panel, SWT.BORDER);
		txtExifTagTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtExifTagTxt.setEnabled(false);
		
		
		Link linkSelectFromFile = new Link(panel, SWT.NONE);
		linkSelectFromFile.setText("<a>" + "Select Tag From File ..." + "</a>");
		linkSelectFromFile.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false, 2, 1));
		linkSelectFromFile.addListener(SWT.Selection, e -> selectExifTagFromFile());
		
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

		l = new Label(valuePart, SWT.WRAP);
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
				if (element instanceof ExifValueMapping) return ((ExifValueMapping) element).tagValue;
				return super.getText(element);
			}
		});
		colTag.setEditingSupport(new EditingSupport(tblExifValueMapping) {
			
			private TextCellEditor cellEditor = new TextCellEditor(tblExifValueMapping.getTable());
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof ExifValueMapping) {
					((ExifValueMapping)element).tagValue = (String)value;
					tblExifValueMapping.refresh();
					dialog.modified();
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof ExifValueMapping) {
					return ((ExifValueMapping) element).tagValue;
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
				if (element instanceof ExifValueMapping) {
					ExifValueMapping v = (ExifValueMapping)element;
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
		
		return panel;
	}
	
	private void addTagMapping() {
		ExifValueMapping mm = new ExifValueMapping();
		mm.tagValue = "< TAG >";
		exifTagValueMappings.add(mm);
		tblExifValueMapping.refresh();
		dialog.modified();
	}

	private void removeExifValueMappings() {
		List<ExifValueMapping> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = tblExifValueMapping.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = (Object)iterator.next();
			if (x instanceof ExifValueMapping) toDelete.add((ExifValueMapping) x);
		}
		this.exifTagValueMappings.removeAll(toDelete);
		tblExifValueMapping.refresh();
		dialog.modified();
	}
	
	private void selectExifTagFromFile() {
		FileDialog fd = new FileDialog(dialog.getShell(), SWT.OPEN);
		String f = fd.open();
		if (f == null) return;
		
		Path p = Paths.get(f);
		if (!Files.exists(p)) {
			MessageDialog.openError(dialog.getShell(), "Not Found", MessageFormat.format("File {0} not found.", p.toString()));
			return;
		}
		
		HashMap<Directory, List<Tag>> tags = FileMetadataReader.readExifMetadata(p);
		if (tags == null ||  tags.isEmpty()) {
			MessageDialog.openError(dialog.getShell(), "Metadata Error", MessageFormat.format("Could not read exif metadata from file {0}.", p.toString()));
			return;
		}
				
		ExifTagSelector selectorDialog = new ExifTagSelector(dialog.getShell(), tags);
		if (selectorDialog.open() != ExifTagSelector.OK) return;
		
		txtExifTag.setData(selectorDialog.getDirectoryTag());
		txtExifTag.setText(String.valueOf(selectorDialog.getDirectoryTag().getTagType()));
		txtExifTagTxt.setText(selectorDialog.getDirectoryTag().getTagName() + " [" + selectorDialog.getDirectory().getName() + "]");
		
		modified();
	}
	

	private static Directory[] getDirectories() {
		//TODO: we could add more here
		return new Directory[] {
				new GpsDirectory(),
				new ExifIFD0Directory(),
				new ExifImageDirectory(),
				new ExifInteropDirectory(),
				new ExifSubIFDDirectory(),
				new ExifThumbnailDirectory()	
		};
	}
	
	private class ExifValueMapping{
		String tagValue;
		Category category;
		Attribute attribute;
		AttributeListItem listItem;
		AttributeTreeNode treeNode;
		
		public String toString() {
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
			if (element instanceof ExifValueMapping) return element.toString();
			return null;
		}

		@Override
		protected void setValue(Object element, Object value) {
			if (value == null) return;
			if (element instanceof ExifValueMapping && (
					value instanceof Category ||
					value instanceof Attribute ||
					value instanceof CategoryAttribute ||
					value instanceof AttributeListItem ||
					value instanceof AttributeTreeNode ||
					value instanceof ListNodeWrapper ||
					value instanceof TreeNodeWrapper)) {
				ExifValueMapping mm = (ExifValueMapping)element;
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
		public TreeNodeWrapper(AttributeTreeNode node, Category c) {
			this.node = node;
			this.category = c;
		}
		AttributeTreeNode node;
		Category category;
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
						data[j++] = new TreeNodeWrapper(i, category);
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
					data[j++] = new TreeNodeWrapper(i, w.category);
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
				AttributeTreeNode part = ((TreeNodeWrapper) element).node;
				if (part.getParent() == null) {
					//TODO:
				}else {
					//TODO:
				}
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
