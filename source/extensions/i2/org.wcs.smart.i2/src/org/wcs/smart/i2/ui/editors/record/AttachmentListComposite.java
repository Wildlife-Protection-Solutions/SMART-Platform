package org.wcs.smart.i2.ui.editors.record;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.editors.AttachmentTable;
import org.wcs.smart.i2.ui.editors.IMenuCreator;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.ui.properties.DialogConstants;

public class AttachmentListComposite extends Composite{

	private AttachmentTable attachmentTable;
	private Composite compAttachmentEdit;
	private RecordEditor editor;
	
	private List<IntelEntityAttachment> newEntityAttachments = new ArrayList<IntelEntityAttachment>();
	private List<IntelRecordAttachment> attachmentsToDelete = new ArrayList<IntelRecordAttachment>();
	
	public AttachmentListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		toolkit.adapt(this);
		this.editor = editor;
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
	
		compAttachmentEdit= toolkit.createComposite(this, SWT.NONE);
		compAttachmentEdit.setLayout(new GridLayout(1, false));
		((GridLayout)compAttachmentEdit.getLayout()).marginHeight = 0;
		((GridLayout)compAttachmentEdit.getLayout()).marginWidth = 0;
		((GridLayout)compAttachmentEdit.getLayout()).horizontalSpacing = 0;
		((GridLayout)compAttachmentEdit.getLayout()).verticalSpacing = 0;
		compAttachmentEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnAddAttachment = toolkit.createButton(compAttachmentEdit, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
		btnAddAttachment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {	
				addAttachment();
			}
		});
		updateEditMode();
		IMenuCreator thumbMenu = new IMenuCreator() {
			private MenuItem mnuAdd;
			private MenuItem mnuOpen;
			private MenuItem mnuDelete;
			private MenuItem mnulinkTo;
			private Menu thumbMenu;
			
			
			@Override
			public Menu createMenu(Composite parent) {
				thumbMenu = new Menu(parent);
				thumbMenu.addMenuListener(new MenuListener() {
					
					@Override
					public void menuShown(MenuEvent e) {
						createMenu();
					}
					
					@Override
					public void menuHidden(MenuEvent e) {
					}
				});
				parent.setMenu(thumbMenu);
				return thumbMenu;
			}
			
			private void createMenu(){		
				if (mnuOpen == null){
					mnuOpen = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuOpen.setText("Open");
					mnuOpen.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (!attachmentTable.getSelection().isEmpty()){
								AttachmentUtil.openAttachment(attachmentTable.getSelection().get(0));
							}
						}
					});
				}
				if (editor.getEditMode()){
					if (mnuAdd == null){
						mnuAdd = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
						mnuAdd.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								addAttachment();
							}
						});
					}
					if (mnuDelete == null){
						mnuDelete = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!attachmentTable.getSelection().isEmpty()){
									IntelAttachment toDelete = attachmentTable.getSelection().get(0);
									
									
									for (IntelRecordAttachment ea : editor.getRecord().getAttachments()){
										if (ea.getAttachment().equals(toDelete)){
											attachmentsToDelete.add(ea);
											editor.getRecord().getAttachments().remove(ea);
											break;
										}
									}
									refreshAttachmentTable();
									editor.setDirty(true);
								}
							}	
						});
					}
					if (mnulinkTo == null){
						mnulinkTo = new MenuItem(this.thumbMenu, SWT.CASCADE);
						mnulinkTo.setText("Link To Entity...");
						
						Menu mnuEntities = new Menu(mnulinkTo);
						mnulinkTo.setMenu(mnuEntities);
						
						for (IntelEntityRecord entity : editor.getRecord().getEntities()){
							MenuItem eItem = new MenuItem(mnuEntities, SWT.DEFAULT);
							eItem.setText(entity.getEntity().getIdAttributeAsText());
							eItem.setImage(EntityTypeLabelProvider.INSTANCE.getImage(entity.getEntity().getEntityType()));
							eItem.addSelectionListener(new SelectionAdapter() {
								
								@Override
								public void widgetSelected(SelectionEvent e) {
									//TODO: 
									if (!attachmentTable.getSelection().isEmpty()){
										IntelEntityAttachment a = new IntelEntityAttachment();
										a.setEntity(entity.getEntity());
										a.setAttachment(attachmentTable.getSelection().get(0));
										
										//determine if this attachment already exists
										for (IntelEntityAttachment existing : getNewEntityAttachments()){
											if (existing.getAttachment().equals(a.getAttachment())){
												return;
											}
										}
										for (IntelEntityAttachment existing : entity.getEntity().getEntityAttachments()){
											if (existing.getAttachment().equals(a.getAttachment())){
												return;
											}
										}
										
										getNewEntityAttachments().add(a);
										editor.setDirty(true);
										editor.getEntityPanel().init();
									}
									
								}
							});
						}
						
					}
					
				}else{
					if (mnuDelete != null){
						mnuDelete.dispose();
						mnuDelete = null;
					}
					if (mnulinkTo != null){
						mnulinkTo.dispose();
						mnulinkTo = null;
					}
				}
			}
		};
		attachmentTable = new AttachmentTable(this, toolkit, thumbMenu, SWT.BORDER);
		attachmentTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
	}
	
	private void addAttachment(){
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		dialog.open();
		
		if (dialog.getFileNames() != null){
			for (String file : dialog.getFileNames()){
				IntelAttachment ia = new IntelAttachment();
				ia.setConservationArea(SmartDB.getCurrentConservationArea());
				ia.setCopyFromLocation(Paths.get(dialog.getFilterPath()).resolve(file).toFile());
				ia.setCreatedBy(SmartDB.getCurrentEmployee());
				ia.setDateCreated(new Date());
				ia.setFilename(Paths.get(dialog.getFilterPath()).resolve(file).getFileName().toString());
				
				IntelRecordAttachment iea = new IntelRecordAttachment();
				iea.setRecord(editor.getRecord());
				iea.setAttachment(ia);
				if (editor.getRecord().getAttachments() == null){
					editor.getRecord().setAttachments(new ArrayList<IntelRecordAttachment>());
				}
				editor.getRecord().getAttachments().add(iea);
			}
			editor.setDirty(true);
			refreshAttachmentTable();
		}
		
	}
	public void updateEditMode(){
		if (editor.getEditMode()){
			compAttachmentEdit.setVisible(true);
			((GridData)compAttachmentEdit.getLayoutData()).heightHint = compAttachmentEdit.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		}else{
			compAttachmentEdit.setVisible(false);
			((GridData)compAttachmentEdit.getLayoutData()).heightHint = 0;
		}
		compAttachmentEdit.getParent().layout();
	}
	
	public void refreshAttachmentTable(){
		List<ISmartAttachment> attachments = new ArrayList<ISmartAttachment>();
		if (editor.getRecord().getAttachments() != null){
			for (IntelRecordAttachment a : editor.getRecord().getAttachments()){
				attachments.add(a.getAttachment());
			}
		}
		attachmentTable.setAttachments(attachments);
	}
	
	public List<IntelEntityAttachment> getNewEntityAttachments(){
		return newEntityAttachments;
	}

	public List<IntelRecordAttachment> getAttachmentsToDelete(){
		return attachmentsToDelete;
	}
}
