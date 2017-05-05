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
package org.wcs.smart.i2.ui.editors.record;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.FileLocationParser;
import org.wcs.smart.i2.ui.TransparentInfoDialog;
import org.wcs.smart.i2.ui.dialogs.AttachmentPropertiesDialog;
import org.wcs.smart.i2.ui.editors.AttachmentTable;
import org.wcs.smart.i2.ui.editors.IMenuCreator;
import org.wcs.smart.i2.ui.handler.OpenAttachmentViewHandler;
import org.wcs.smart.i2.ui.views.FileSearchView;
import org.wcs.smart.ui.properties.DialogConstants;

import com.ibm.icu.text.MessageFormat;

/**
 * Composite for displaying attachments and thumbnails
 * 
 * @author Emily
 *
 */
public class AttachmentListComposite extends Composite{

	private AttachmentTable attachmentTable;
	private Composite compAttachmentEdit;
	private RecordEditor editor;
	
	private List<IntelEntityAttachment> newEntityAttachments = new ArrayList<IntelEntityAttachment>();
	private List<IntelEntityAttachment> deleteEntityAttachments = new ArrayList<IntelEntityAttachment>();
	
	private List<IntelRecordAttachment> attachmentsToDelete = new ArrayList<IntelRecordAttachment>();
	
	
	public AttachmentListComposite(Composite parent, FormToolkit toolkit, RecordEditor editor){
		super(parent, SWT.NONE);
		toolkit.adapt(this);
		this.editor = editor;
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
	
		compAttachmentEdit= toolkit.createComposite(this, SWT.NONE);
		compAttachmentEdit.setLayout(new GridLayout(1, false));
		((GridLayout)compAttachmentEdit.getLayout()).marginHeight = 0;
		((GridLayout)compAttachmentEdit.getLayout()).marginWidth = 0;
		((GridLayout)compAttachmentEdit.getLayout()).horizontalSpacing = 0;
		((GridLayout)compAttachmentEdit.getLayout()).verticalSpacing = 0;
		compAttachmentEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		final Button btnAddAttachment = toolkit.createButton(compAttachmentEdit, DialogConstants.ADD_BUTTON_TEXT, SWT.PUSH);
		btnAddAttachment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {	
				addAttachment();
			}
		});
		
		updateEditMode();
		IMenuCreator thumbMenu = new IMenuCreator() {
			private MenuItem mnuAdd;
			private MenuItem mnuSearch;
			private MenuItem mnuOpen;
			private MenuItem mnuOpenThumbnail;
			private MenuItem mnuDelete;
			private MenuItem mnulinkTo;
			private MenuItem mnuProperties;
			private MenuItem mnuRefresh;
			private MenuItem mnuSep;
			private MenuItem mnuunlink;
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
				if (mnuOpenThumbnail == null){
					mnuOpenThumbnail = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuOpenThumbnail.setText(Messages.AttachmentListComposite_openMenuItem);
					mnuOpenThumbnail.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (!attachmentTable.getSelection().isEmpty()){
								(new OpenAttachmentViewHandler()).execute(attachmentTable.getSelection().get(0), editor.getContext());
							}
						}
					});
					
				}
				if (mnuOpen == null){
					mnuOpen = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuOpen.setText(Messages.AttachmentListComposite_OpenSystemMenuItem);
					mnuOpen.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (!attachmentTable.getSelection().isEmpty()){
								AttachmentUtil.openAttachment(attachmentTable.getSelection().get(0));
							}
						}
					});
					
				}
				if (mnuSearch == null){
					mnuSearch = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuSearch.setText(Messages.AttachmentListComposite_SearchItem);
					mnuSearch.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ATTACHMENT_SEARCH));
					mnuSearch.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							FileSearchView.doSearch(editor.getContext(), attachmentTable.getSelection());
						}
					});
					
					new MenuItem(thumbMenu, SWT.SEPARATOR);
				}
				
				if (editor.getEditMode()){
					int index = 4;
					if (mnuAdd == null){
						mnuAdd = new MenuItem(thumbMenu,SWT.DEFAULT,index);
						mnuAdd.setText(Messages.AttachmentListComposite_AddAttachmentItem);
						mnuAdd.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								addAttachment();
							}
						});
					}
					index ++;
					
					if (mnuDelete == null){
						mnuDelete = new MenuItem(thumbMenu,SWT.DEFAULT,index);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!attachmentTable.getSelection().isEmpty()){
									for (IntelAttachment toDelete : attachmentTable.getSelection()){
										for (IntelRecordAttachment ea : editor.getRecord().getAttachments()){
											if (ea.getAttachment().equals(toDelete)){
												attachmentsToDelete.add(ea);
												editor.getRecord().getAttachments().remove(ea);
												break;
											}
										}
									}
									refreshAttachmentTable();
									editor.setDirty(true);
								}
							}	
						});
					}
					index ++;
					
					if (mnulinkTo != null){
						mnulinkTo.dispose();
						mnulinkTo = null;
					}
					if (mnuunlink != null){
						mnuunlink.dispose();
						mnuunlink = null;
					}
					if (IntelSecurityManager.INSTANCE.canLinkAttachmentsToEntities()){
						mnulinkTo = new MenuItem(this.thumbMenu, SWT.CASCADE,index);
						mnulinkTo.setText(Messages.AttachmentListComposite_LinkEntityItem);
						
						Menu mnuEntities = new Menu(mnulinkTo);
						mnulinkTo.setMenu(mnuEntities);
						if (editor.getRecord().getEntities() == null) editor.getRecord().setEntities(new ArrayList<IntelEntityRecord>());
						for (IntelEntityRecord entity : editor.getRecord().getEntities()){
							MenuItem eItem = new MenuItem(mnuEntities, SWT.DEFAULT);
							eItem.setText(entity.getEntity().getIdAttributeAsText());
							if (entity.getEntity().getEntityType().getIcon() != null){
								eItem.setImage(EntityTypeLabelProvider.createImageDescriptor(entity.getEntity().getEntityType()).createImage());
								eItem.addListener(SWT.Dispose, (event) -> {if (eItem.getImage() != null) eItem.getImage().dispose();});
							}
							eItem.addSelectionListener(new SelectionAdapter() {
								
								@Override
								public void widgetSelected(SelectionEvent e) {
									boolean changes = false;
									for (IntelAttachment attachment : attachmentTable.getSelection()){
										IntelEntityAttachment a = new IntelEntityAttachment();
										a.setEntity(entity.getEntity());
										a.setAttachment(attachment);
										
										boolean exists = false;
										//determine if this attachment already exists
										for (IntelEntityAttachment existing : getNewEntityAttachments()){
											if (existing.getAttachment().equals(a.getAttachment())){
												exists = true;
												break;
											}
										}
										if (exists) continue;
										for (IntelEntityAttachment existing : entity.getEntity().getEntityAttachments()){
											if (existing.getAttachment().equals(a.getAttachment())){
												exists = true;
												break;
											}
										}
										if (exists) continue;
										getNewEntityAttachments().add(a);
										changes = true;
									}
									if (changes){
										editor.setDirty(true);
										editor.getSummaryPage().getEntityPanel().init();
									}else{
										editor.showMessage("Attachments already linked to entities");
									}
									
								}
							});
						}
						index ++;
						
						
						mnuunlink = new MenuItem(this.thumbMenu, SWT.CASCADE,index);
						mnuunlink.setText("Unlink From Entity...");
						
						Menu mnuEntitiesUnlink = new Menu(mnuunlink);
						mnuunlink.setMenu(mnuEntitiesUnlink);
						
						if (attachmentTable.getSelection().size() > 0){
							IntelAttachment ia = attachmentTable.getSelection().get(0);
							
							Set<IntelEntity> eas = new HashSet<>();
							for (IntelEntityRecord entity : editor.getRecord().getEntities()){
								for (IntelEntityAttachment ea : entity.getEntity().getEntityAttachments()){
									if (!ea.getAttachment().equals(ia)) continue;
									eas.add(ea.getEntity());
								}
							}
							for (IntelEntityAttachment ea : getNewEntityAttachments()){
								eas.add(ea.getEntity());
							}
							for (IntelEntity ea : eas){
								MenuItem eItem = new MenuItem(mnuEntitiesUnlink, SWT.DEFAULT);
								eItem.setText(ea.getIdAttributeAsText());
								if (ea.getEntityType().getIcon() != null){
									eItem.setImage(EntityTypeLabelProvider.createImageDescriptor(ea.getEntityType()).createImage());
									eItem.addListener(SWT.Dispose, (event) -> {if (eItem.getImage() != null) eItem.getImage().dispose();});
								}
								
								eItem.addSelectionListener(new SelectionAdapter() {
										
									@Override
									public void widgetSelected(SelectionEvent e) {
										boolean changes = false;
										for (IntelAttachment attachment : attachmentTable.getSelection()){
											
											IntelEntityAttachment iea = null;
											for (IntelEntityAttachment existing : ea.getEntityAttachments()){
												if (existing.getAttachment().equals(attachment)){
													iea = existing;
													break;
												}
											}
											if (iea != null){
												iea.getEntity().getEntityAttachments().remove(iea);
												getRemovedEntityAttachments().add(iea);
												changes = true;
												continue;
											}
											
											for (IntelEntityAttachment existing : getNewEntityAttachments()){
												if (existing.getAttachment().equals(attachment)){
													iea = existing;
													break;
												}
											}	
											
											if (iea != null){
												getNewEntityAttachments().remove(iea);
												changes = true;
											}
										}
										if (changes){
											editor.setDirty(true);
											editor.getSummaryPage().getEntityPanel().init();
										}
									}
								});
							}
						}
						index ++;
					}
					
					if (mnuSep == null) mnuSep = new MenuItem(thumbMenu, SWT.SEPARATOR, index);
					index ++;
				}else{
					if (mnuAdd != null){
						mnuAdd.dispose();
						mnuAdd = null;
					}
					if (mnuSep != null){
						mnuSep.dispose();
						mnuSep = null;
					}
					if (mnuDelete != null){
						mnuDelete.dispose();
						mnuDelete = null;
					}
					if (mnulinkTo != null){
						mnulinkTo.dispose();
						mnulinkTo = null;
					}
				}
				if (mnuRefresh == null){
					mnuRefresh = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuRefresh.setText(Messages.AttachmentListComposite_Refresh);
					mnuRefresh.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							attachmentTable.refreshThumbnails();
						}
					});
				}
				
				if (mnuProperties == null){
					mnuProperties = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuProperties.setText(Messages.AttachmentListComposite_PropertiesItem);
					mnuProperties.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (!attachmentTable.getSelection().isEmpty()){
								(new AttachmentPropertiesDialog(getShell(), attachmentTable.getSelection().get(0))).open();
							}
						}
					});
				}
				
			}
		};
		attachmentTable = new AttachmentTable(this, toolkit, thumbMenu, SWT.NONE);
		attachmentTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));	
	}
	
	private void addAttachment(){
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		dialog.open();
		
		if (dialog.getFileNames() != null){
			
			final List<IntelAttachment> toSearch = new ArrayList<IntelAttachment>();
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
				toSearch.add(ia);
			}
			
			//parse locations from file in pmd dialog incase files are large
			@SuppressWarnings("unchecked")
			List<IntelLocation>[] locations = new List[]{null}; 
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			try{
				pmd.run(true, false, new IRunnableWithProgress() {
						
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						List<IntelLocation> addedLocations = new ArrayList<IntelLocation>();
						monitor.beginTask(Messages.AttachmentListComposite_ProcessingFilesTask, IProgressMonitor.UNKNOWN);
						for (IntelAttachment ia : toSearch){
							List<IntelLocation> toAdd = FileLocationParser.INSTANCE.parseFile(ia.getCopyFromLocation());
							if (toAdd != null) addedLocations.addAll(toAdd);
						}
						locations[0] = addedLocations;
						monitor.done();
					}
				});
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(Messages.AttachmentListComposite_ProcessingError +ex.getMessage(), ex);
			}
			
			if (locations[0] != null){
				List<IntelLocation> added =  (List<IntelLocation>)locations[0];
				editor.addNewLocations(added);
				if (added.size() > 0){
					editor.showMessage(MessageFormat.format(Messages.AttachmentListComposite_LocationsParsedMsg, added.size(), toSearch.size()));
				}
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

	public List<IntelEntityAttachment> getRemovedEntityAttachments(){
		return deleteEntityAttachments;
	}
	
	public List<IntelRecordAttachment> getAttachmentsToDelete(){
		return attachmentsToDelete;
	}
}
