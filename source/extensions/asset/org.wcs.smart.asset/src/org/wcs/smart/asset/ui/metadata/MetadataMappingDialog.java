package org.wcs.smart.asset.ui.metadata;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

public class MetadataMappingDialog extends TitleAreaDialog{

	
	private TableViewer tblMappings;
	
	private List<AssetMetadataMapping> mappings;
	private List<AssetMetadataMapping> mappingsToDelete;
	
	public MetadataMappingDialog(Shell parentShell) {
		super(parentShell);
		mappingsToDelete = new ArrayList<>();
	}


	@Override
	protected void okPressed() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				mappingsToDelete.forEach(m->{
					if (m.getUuid() != null) session.delete(m);
				});
				for (int i = 0; i < mappings.size(); i ++) mappings.get(i).setSearchOrder(i);
				mappings.forEach(m->session.saveOrUpdate(m));
				session.getTransaction().commit();
				
			}catch (Exception ex) {
				AssetPlugIn.displayLog("Unable to save changes to asset mappings: " + ex.getMessage(), ex);
				session.getTransaction().rollback();
				return;
			}
		}
		mappingsToDelete.clear();
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	protected void cancelPressed(){
		super.cancelPressed();
	}

	protected void createButtonsForButtonBar(Composite parent) {
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		btnOk.setEnabled(false);
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		Composite cmp = new Composite(parent, SWT.NONE);
		cmp.setLayout(new GridLayout(2, false));
		cmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		tblMappings = new TableViewer(cmp, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblMappings.getTable().setLinesVisible(true);
		tblMappings.getTable().setHeaderVisible(true);
		tblMappings.setContentProvider(ArrayContentProvider.getInstance());
		tblMappings.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblMappings.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewerColumn colType = new TableViewerColumn(tblMappings, SWT.NONE);
		colType.getColumn().setText("Type");
		colType.getColumn().setWidth(50);
		colType.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping) {
					return ((AssetMetadataMapping) element).getMetadataType().name();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn colMetadata = new TableViewerColumn(tblMappings, SWT.NONE);
		colMetadata.getColumn().setText("Metadata");
		colMetadata.getColumn().setWidth(250);
		colMetadata.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping) {
					AssetMetadataMapping mm = (AssetMetadataMapping)element;
					if (mm.getMetadataField() == null) {
						return "ERROR PARSING METADATA MAPPING";
					}
					return mm.getMetadataField().asUserString();
				}
				return super.getText(element);
			}
		});
		
		TableViewerColumn colMapTo = new TableViewerColumn(tblMappings, SWT.NONE);
		colMapTo.getColumn().setText("Mapped To");
		colMapTo.getColumn().setWidth(250);
		colMapTo.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping) {
					AssetMetadataMapping mm = (AssetMetadataMapping)element;
					if (mm.getMappedAssetProperty() != null) return mm.getMappedAssetProperty().name();
					StringBuilder sb = new StringBuilder();
					if ( ((ExifMetadataField)mm.getMetadataField() ).getTagValue() != null) sb.append( ((ExifMetadataField)mm.getMetadataField() ).getTagValue()  + ": ");
					if (mm.getMappedListItem() != null) sb.append(mm.getMappedListItem().getName());
					if (mm.getMappedTreeNode() != null) sb.append(mm.getMappedTreeNode().getName());
					
					if (mm.getMappedAttribute() != null) {
						if (sb.length() != 0) sb.append(" (" + mm.getMappedAttribute().getName() + ")");
						else sb.append(mm.getMappedAttribute().getName());
					}
					
					if (mm.getMappedCategory() != null) {
						if (sb.length() != 0) sb.append(" [" + mm.getMappedCategory().getFullCategoryName() + "]");
						else sb.append(mm.getMappedCategory().getFullCategoryName());
					}
					
					return sb.toString();
				}
				return super.getText(element);
			}
		});
		
		
		
		Composite buttonPanel = new Composite(cmp, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, true));

		Button btnNew = createButton(buttonPanel, DialogConstants.ADD_BUTTON_TEXT, ()->addMapping());
		Button btnEdit = createButton(buttonPanel, DialogConstants.EDIT_BUTTON_TEXT, ()->editMapping());
		Button btnDelete = createButton(buttonPanel, DialogConstants.DELETE_BUTTON_TEXT, ()->deleteMappings());
		Label l = new Label(buttonPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Button btnMoveUp = createButton(buttonPanel, "Move Up", ()->moveUp());
		Button btnMoveDown = createButton(buttonPanel, "Move Down", ()->moveDown());
		
		btnNew.setEnabled(true);
		tblMappings.getControl().addListener(SWT.Selection, e->{
			boolean hasSelection = !tblMappings.getSelection().isEmpty();
			btnEdit.setEnabled(hasSelection);
			btnDelete.setEnabled(hasSelection);
			btnMoveUp.setEnabled(hasSelection);
			btnMoveDown.setEnabled(hasSelection);
		});
		
		Menu tblMenu = new Menu(tblMappings.getControl());
		
		MenuItem addItem = createMenuItem(tblMenu, DialogConstants.ADD_BUTTON_TEXT, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON), ()->addMapping());
		MenuItem editItem = createMenuItem(tblMenu, DialogConstants.EDIT_BUTTON_TEXT, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON), ()->editMapping());
		MenuItem deleteItem = createMenuItem(tblMenu, DialogConstants.DELETE_BUTTON_TEXT, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON), ()->deleteMappings());
		new MenuItem(tblMenu, SWT.SEPARATOR);
		MenuItem moveUpItem = createMenuItem(tblMenu, "Move Up", null, ()->moveUp());
		MenuItem moveDownItem = createMenuItem(tblMenu, "Move Down", null, ()->moveDown());
		addItem.setEnabled(true);
		tblMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = !tblMappings.getSelection().isEmpty();
				editItem.setEnabled(hasSelection);
				deleteItem.setEnabled(hasSelection);
				moveUpItem.setEnabled(hasSelection);
				moveDownItem.setEnabled(hasSelection);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
		tblMappings.getControl().setMenu(tblMenu);
		
		setTitle("File Metadata Mappings");
		setMessage("Configure file metadata mappings");
		getShell().setText("File Metadata Mappings");
		
		int width = cmp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		width = width / tblMappings.getTable().getColumnCount();
		int cnt = 0;
		for (TableColumn c : tblMappings.getTable().getColumns()) {
			if (cnt != 0) c.setWidth(width);
			cnt ++;
		}
		
		loadMappings.schedule();
		
		return parent;
	}
	
	private Button createButton(Composite parent, String text, Callable<Void> event) {
		Button button = new Button(parent, SWT.PUSH);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		button.addListener(SWT.Selection, e->{
			try {
				event.call();
			}catch (Exception ex) {
				//TODO:
				ex.printStackTrace();
			}
		});
		button.setEnabled(false);
		return button;
	}
	
	private MenuItem createMenuItem(Menu parent, String text, Image img, Callable<Void> event) {
		MenuItem button = new MenuItem(parent, SWT.PUSH);
		button.setText(text);
		button.setImage(img);
		button.addListener(SWT.Selection, e->{
			try {
				event.call();
			}catch (Exception ex) {
				ex.printStackTrace();
				//TODO:
			}
		});
		button.setEnabled(false);
		return button;
	}
	
	private Void addMapping() {
		NewMappingDialog dialog = new NewMappingDialog(getShell());
		if (dialog.open() != NewMappingDialog.OK) return null;
		
		mappings.addAll(dialog.getMappings());
		
		tblMappings.refresh();
		modified();
		return null;
	}
	
	private Void editMapping() {
		return null;
	}
	
	
	private Void deleteMappings() {
		List<AssetMetadataMapping> toDelete = getSelection();
		if (toDelete.isEmpty()) return null;
		if (!MessageDialog.openQuestion(getShell(), "Delete", MessageFormat.format("Are you sure you want to delete the {0} selected mappings?", toDelete.size()))) return null;

		mappings.removeAll(toDelete);
		mappingsToDelete.addAll(toDelete);
		tblMappings.refresh();
		modified();
		
		return null;
	}
	
	private Void moveUp() {
		moveMapping(SWT.DOWN);
		return null;
	}
	
	private Void moveDown() {
		moveMapping(SWT.UP);
		return null;
		
	}
	public List<AssetMetadataMapping> getSelection(){
		 List<AssetMetadataMapping> selection = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection) tblMappings.getSelection()).iterator(); iterator.hasNext();) {
			Object toMove = iterator.next();
			if (toMove instanceof AssetMetadataMapping) {
				selection.add((AssetMetadataMapping) toMove);
			}
		}
		return selection;
	}
	
	private void moveMapping(int direction){
		for (AssetMetadataMapping toMove : getSelection()) {
			int index = mappings.indexOf(toMove);
			if (direction == SWT.UP){
				index ++;
				if(index >= mappings.size()){
					index = mappings.size() - 1;
				}
			}else if (direction == SWT.DOWN){
				index --;
				if(index < 0) index = 0;
			}
			mappings.remove(toMove);
			mappings.add(index, toMove);
		}
		modified();
		tblMappings.refresh();
	}
	
	public void modified() {
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		//TODO:
	}
	@Override
	public boolean isResizable(){
		return true;
	}	
	
	private Job loadMappings = new Job("load metadata mappings") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<AssetMetadataMapping> items = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				items.addAll(QueryFactory.buildQuery(session, AssetMetadataMapping.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list());
				
				items.forEach(i->{
					if (i.getMappedAttribute() != null) i.getMappedAttribute().getName();
					if (i.getMappedCategory() != null) i.getMappedCategory().getFullCategoryName();
					if (i.getMappedListItem() != null) i.getMappedListItem().getName();
					if (i.getMappedTreeNode() != null) i.getMappedTreeNode().getName();
				});
			}
			items.sort((a,b)->a.getSearchOrder().compareTo(b.getSearchOrder()));
			mappings = items;
			Display.getDefault().syncExec(()->{
				if (tblMappings.getControl().isDisposed()) return;
				tblMappings.setInput(mappings);
				tblMappings.refresh();
			});
			return Status.OK_STATUS;
		}
		
	};
}