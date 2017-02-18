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

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.dialogs.AttachmentPropertiesDialog;
import org.wcs.smart.i2.ui.editors.AttachmentTable;
import org.wcs.smart.i2.ui.editors.IMenuCreator;
import org.wcs.smart.i2.ui.handler.OpenAttachmentViewHandler;
import org.wcs.smart.i2.ui.views.FileSearchView;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.ui.properties.DialogConstants;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

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
			private MenuItem mnuSearch;
			private MenuItem mnuOpen;
			private MenuItem mnuOpenThumbnail;
			private MenuItem mnuDelete;
			private MenuItem mnulinkTo;
			private MenuItem mnuProperties;
			private MenuItem mnuSep;
			
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
					mnuOpenThumbnail.setText("Open Image");
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
					mnuOpen.setText("Open System Editor");
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
					mnuSearch.setText("Search");
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
						mnuAdd.setText("Add Attachment");
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
					
					if (IntelSecurityManager.INSTANCE.canLinkAttachmentsToEntities()){
						mnulinkTo = new MenuItem(this.thumbMenu, SWT.CASCADE,index);
						mnulinkTo.setText("Link To Entity...");
						
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
									for (IntelAttachment attachment : attachmentTable.getSelection()){
										IntelEntityAttachment a = new IntelEntityAttachment();
										a.setEntity(entity.getEntity());
										a.setAttachment(attachment);
										
										//determine if this attachment already exists
										for (IntelEntityAttachment existing : getNewEntityAttachments()){
											if (existing.getAttachment().equals(a.getAttachment())){
												continue;
											}
										}
										for (IntelEntityAttachment existing : entity.getEntity().getEntityAttachments()){
											if (existing.getAttachment().equals(a.getAttachment())){
												continue;
											}
										}
										
										getNewEntityAttachments().add(a);
									}
									editor.setDirty(true);
									editor.getSummaryPage().getEntityPanel().init();
									
								}
							});
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
				if (mnuProperties == null){
					mnuProperties = new MenuItem(thumbMenu,SWT.DEFAULT);
					mnuProperties.setText("Properties...");
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
				
				File imageFile = ia.getCopyFromLocation();
				try{
					Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
					for (Directory directory : metadata.getDirectoriesOfType(GpsDirectory.class)) {
						GeoLocation geoLocation = ((GpsDirectory)directory).getGeoLocation();
						if (geoLocation != null){
							Date dateTime = ((GpsDirectory) directory).getGpsDate();
							Point pnt = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(geoLocation.getLongitude(), geoLocation.getLatitude()));
							editor.addNewLocation(pnt, dateTime);
							break;
						}
						
					}
				}catch (Exception ex){
					//cannot create thumbnail
					//ex.printStackTrace();
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

	public List<IntelRecordAttachment> getAttachmentsToDelete(){
		return attachmentsToDelete;
	}
}
