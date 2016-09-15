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
package org.wcs.smart.i2.ui.editors;

import java.io.File;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.i2.ui.dialogs.AttributeFieldEditor;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;

public class EntityEditor extends EditorPart{
	
	public static final String ID = "org.wcs.smart.i2.editor.entity";

	private static final int THUMB_SIZE = 150;
	
	private Font boldFont;
	private Font headerFont;
	private Color hlinkColor;
	
	private EntityEditorInput input;
	private IntelEntity entity;
	private boolean isEditMode;
	public boolean isDirty;
	
	private List<AttributeFieldEditor> fieldEditors = null;
	
	private FormToolkit toolkit;
	private Canvas lblMainImage;
	private Image imgMain;
	
	private Label lblCreated;
	private Label lblModified;
	private Label lblIdentifier;
	
	private Composite compAttributes;
	private Composite compAttachments;
	
	private StackLayout attributeStack;
	private Button btnEditMode;
	private AttachmentTable attachmentTable;
	private Composite attachmentEditPanel;
	
	private List<IntelEntityAttachment> attachmentsToDelete = new ArrayList<IntelEntityAttachment>();
	
	private Job loadEntity = new Job("load entity"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			entity = null;
			IntelEntity temp = null;
			Session s = HibernateManager.openSession();
			try{
				temp = (IntelEntity) s.get(IntelEntity.class, input.getUuid());
				for(IntelEntityTypeAttribute a : temp.getEntityType().getAttributes()){
					a.getAttribute().getName();
					if (a.getAttribute().getAttributeList() != null){
						for(IntelAttributeListItem i : a.getAttribute().getAttributeList()){
							i.getName();
						}
					}
				}
				for (IntelEntityAttributeValue v : temp.getAttributes()){
					v.getAttribute().getName();
					if (v.getAttributeListItem() != null) v.getAttributeListItem().getName();
					v.getAttributeValue();
				}
				if (temp.getEntityAttachments() != null){
					for (IntelEntityAttachment a : temp.getEntityAttachments()){
						try {
							a.getAttachment().computeFileLocation(s);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				temp.getPrimaryAttachment();
			}finally{
				s.close();
			}
			entity = temp;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					
					initControl(entity);
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
	@Override
	public void doSave(IProgressMonitor monitor) {
		for(AttributeFieldEditor editor : fieldEditors){
			if (!editor.isValid()){
				MessageDialog.openError(getSite().getShell(), "Save Error", MessageFormat.format("Fix error with attribute {0} before saving", editor.getAttribute().getName()));
				return;
			}
		}
		for(AttributeFieldEditor editor : fieldEditors){
			IntelEntityAttributeValue value = entity.findAttributeValue(editor.getAttribute());
			if(value == null){
				value = new IntelEntityAttributeValue();
				value.setAttribute(editor.getAttribute());
				value.setEntity(entity);
				
				if (editor.updateValue(value)){
					entity.getAttributes().add(value);
				}
			}else{
				if (!editor.updateValue(value)){
					entity.getAttributes().remove(value);
				}
			}
		}
		
		Session s = HibernateManager.openSession(new AttachmentInterceptor());
		try{
			s.beginTransaction();
			for (IntelEntityAttachment ea : attachmentsToDelete){
				
				//TODO: check for other references before we delete this
				if (ea.getAttachment().getUuid() != null){
					s.delete(ea);
					s.delete(ea.getAttachment());
				}
			}
			if (entity.getEntityAttachments() != null){
				for (IntelEntityAttachment a : entity.getEntityAttachments()){
					s.saveOrUpdate(a.getAttachment());
				}
			}
			s.saveOrUpdate(entity);
			s.getTransaction().commit();
			
			attachmentsToDelete.clear();
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error saving entity changes: " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		setDirty(false);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public void doSaveAs() {		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		this.input = (EntityEditorInput) input;
		setInput(input);
		setSite(site);
		
		super.setTitleImage(input.getImageDescriptor().createImage());
		super.setPartName(input.getName());
	}


	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		
		toolkit = new FormToolkit(parent.getDisplay());

		parent.setLayout(createGridLayoutNoMargin(1));
		
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createTopPanel(sash);
		
		Composite panel = toolkit.createComposite(sash, SWT.NONE);
		panel.setLayout(new GridLayout(1, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}


	@Override
	public void dispose(){
		if (boldFont != null){ boldFont.dispose(); boldFont = null;}
		if (headerFont != null){ headerFont.dispose(); headerFont = null;}
		super.dispose();
	}
	
	private void createTopPanel(Composite parent){
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		
		fd.setHeight(fd.getHeight()  + 1);
		headerFont = new Font(parent.getDisplay(), fd);
		
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(2, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = toolkit.createComposite(panel, SWT.NONE);
		leftPart.setLayout(new GridLayout(2, false));
		leftPart.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		lblMainImage = new Canvas(leftPart, SWT.NONE);
		lblMainImage.addPaintListener(new PaintListener() {			
			@Override
			public void paintControl(PaintEvent e) {
				Rectangle r = lblMainImage.getClientArea();
				GC gc = e.gc;
				gc.setForeground(toolkit.getColors().getBorderColor());
				
				if (imgMain != null  && !imgMain.isDisposed()){
					gc.drawImage(imgMain, 0, 0);
				}else{
					gc.drawLine(0, 0, r.width - 1, r.height - 1);
					gc.drawLine(0, r.height-1, r.width - 1, 0);
				}
				gc.drawRectangle(0, 0, r.width - 1, r.height - 1);
			}
		});
		lblMainImage.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false, 2, 1));
		((GridData)lblMainImage.getLayoutData()).widthHint = THUMB_SIZE;
		((GridData)lblMainImage.getLayoutData()).heightHint = THUMB_SIZE;

		
		toolkit.createLabel(leftPart, "Created:");
		lblCreated = toolkit.createLabel(leftPart, "CREATED");
		
		toolkit.createLabel(leftPart, "Modified:");
		lblModified= toolkit.createLabel(leftPart, DateFormat.getInstance().format(new Date()));
		
		Composite rightPart = toolkit.createComposite(panel, SWT.NONE);
		rightPart.setLayout(new GridLayout(2, false));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lblIdentifier = toolkit.createLabel(rightPart, "");
		lblIdentifier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lblIdentifier.setFont(headerFont);
		btnEditMode = toolkit.createButton(rightPart, "Edit", SWT.TOGGLE);
		btnEditMode.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		btnEditMode.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEditMode(!isEditMode);
				
			}
		});
		

		
		Composite tabList = toolkit.createComposite(rightPart, SWT.NONE);
		tabList.setLayout(new GridLayout(2, false));
		tabList.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		((GridLayout)tabList.getLayout()).marginHeight = 2;
		((GridLayout)tabList.getLayout()).marginWidth = 0;
		tabList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		Hyperlink hAttributes = toolkit.createHyperlink(tabList, "Attributes", SWT.NONE);
		Hyperlink hFiles = toolkit.createHyperlink(tabList, "Files", SWT.NONE);
		hAttributes.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		hFiles.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
		
		Composite tabPart = toolkit.createComposite(rightPart, SWT.NONE);
		attributeStack = new StackLayout();
		tabPart.setLayout(attributeStack);
		tabPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		
		compAttributes = toolkit.createComposite(tabPart, SWT.NONE);
		compAttributes.setLayout(new GridLayout());
		
		compAttachments = toolkit.createComposite(tabPart, SWT.NONE);
		compAttachments.setLayout(new GridLayout());

		createAttachmentPanel(compAttachments);
		
		Font normalFont = hAttributes.getFont();
		
		IHyperlinkListener listener = new IHyperlinkListener() {
			
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				if (e.widget == hAttributes){
					attributeStack.topControl = compAttributes;
					hAttributes.setFont(boldFont);
					hFiles.setFont(normalFont);
					
				}else{
					attributeStack.topControl = compAttachments;
					hFiles.setFont(boldFont);
					hAttributes.setFont(normalFont);
				}
				tabPart.layout();
			}
		};
		hAttributes.addHyperlinkListener(listener);
		hFiles.addHyperlinkListener(listener);
		attributeStack.topControl = compAttributes;
		hAttributes.setFont(boldFont);
		hFiles.setFont(normalFont);
		
		loadEntity.schedule();
	}
	
	public void setEditMode(boolean isEdit){
		if (isEditMode && !isEdit && isDirty){
			doSave(new NullProgressMonitor());
		}
		this.isEditMode = isEdit;
		if (entity != null) initControl(entity);
		
		btnEditMode.setSelection(isEdit);
	}
	
	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isDirty(){
		return this.isDirty;
	}
	
	private void createAttachmentPanel(Composite parent){
		
		attachmentEditPanel = toolkit.createComposite(parent);
		attachmentEditPanel.setLayout(createGridLayoutNoMargin(1));
			
		Button btnAddAttachment = toolkit.createButton(attachmentEditPanel, "Add...", SWT.PUSH);
		btnAddAttachment.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {	
				FileDialog dialog = new FileDialog(getSite().getShell(), SWT.OPEN | SWT.MULTI);
				dialog.open();
				
				if (dialog.getFileNames() != null){
					for (String file : dialog.getFileNames()){
						IntelAttachment ia = new IntelAttachment();
						ia.setConservationArea(SmartDB.getCurrentConservationArea());
						ia.setCopyFromLocation(Paths.get(dialog.getFilterPath()).resolve(file).toFile());
						ia.setCreatedBy(SmartDB.getCurrentEmployee());
						ia.setDateCreated(new Date());
						ia.setFilename(Paths.get(dialog.getFilterPath()).resolve(file).getFileName().toString());
						
						IntelEntityAttachment iea = new IntelEntityAttachment();
						iea.setEntity(entity);
						iea.setAttachment(ia);
						if (entity.getEntityAttachments() == null){
							entity.setEntityAttachments(new ArrayList<IntelEntityAttachment>());
						}
						entity.getEntityAttachments().add(iea);
					}
					setDirty(true);
					attachmentTable.refresh();
				}
				
			}
		});
		
		IMenuCreator thumbMenu = new IMenuCreator() {
			private MenuItem mnuOpen;
			private MenuItem mnuDelete;
			private MenuItem mnuPrimary;
	
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
				if (isEditMode){
					if (mnuDelete == null){
						mnuDelete = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!attachmentTable.getSelection().isEmpty()){
									IntelAttachment toDelete = attachmentTable.getSelection().get(0);
									
									if (toDelete.equals(entity.getPrimaryAttachment())){
										entity.setPrimaryAttachment(null);
										if (imgMain != null) imgMain.dispose();
										lblMainImage.redraw();	
									}
									for (IntelEntityAttachment ea : entity.getEntityAttachments()){
										if (ea.getAttachment().equals(toDelete)){
											attachmentsToDelete.add(ea);
											entity.getEntityAttachments().remove(ea);
											break;
										}
									}
									attachmentTable.refresh();
									setDirty(true);
								}
							}	
						});
					}
					if (mnuPrimary == null){
						mnuPrimary = new MenuItem(thumbMenu,SWT.DEFAULT);
						mnuPrimary.setText("Set as Primary Image");
						mnuPrimary.addSelectionListener(new SelectionAdapter(){
							@Override
							public void widgetSelected(SelectionEvent e) {
								if (!attachmentTable.getSelection().isEmpty()){
									entity.setPrimaryAttachment(attachmentTable.getSelection().get(0));
									
									Thumbnail thum = new Thumbnail(entity.getPrimaryAttachment(), THUMB_SIZE);
									if (imgMain != null) imgMain.dispose();
									imgMain = thum.getImage();
									lblMainImage.redraw();
									setDirty(true);
								}
							}	
						});
					}
				}else{
					if (mnuDelete != null){
						mnuDelete.dispose();
						mnuDelete = null;
					}
					if (mnuPrimary != null){
						mnuPrimary.dispose();
						mnuPrimary = null;
					}
				}
			}
		};
		attachmentTable = new AttachmentTable(parent, toolkit, thumbMenu);
		attachmentTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	
	private void initControl(IntelEntity entity){
		fieldEditors = new ArrayList<AttributeFieldEditor>();
		
		lblCreated.setText(DateFormat.getInstance().format(entity.getDateCreated()));
		lblModified.setText(DateFormat.getInstance().format(entity.getDateModified()));
		lblIdentifier.setText(entity.getIdAttributeAsText());
		lblModified.getParent().layout();
		lblModified.redraw();
		
		if (entity.getPrimaryAttachment() != null){
			File imageFile = entity.getPrimaryAttachment().getAttachmentFile();
			if (imageFile.exists()){
				Thumbnail thum = new Thumbnail(entity.getPrimaryAttachment(), THUMB_SIZE);
				imgMain = thum.getImage();
				lblMainImage.redraw();
			}
		}
			
		//attribute composite
		for (Control kid : compAttributes.getChildren()){
			kid.dispose();
		}
		
		ScrolledForm attributelist = toolkit.createScrolledForm(compAttributes);
		attributelist.getBody().setLayout(createGridLayoutNoMargin(1));
		attributelist.setExpandHorizontal(true);
		attributelist.setExpandVertical(true);
		attributelist.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite part = toolkit.createComposite(attributelist.getBody(), SWT.NONE);
		part.setLayout(createGridLayoutNoMargin(2));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		for (IntelEntityTypeAttribute a : entity.getEntityType().getAttributes()){
			if (isEditMode){
				AttributeFieldEditor e = new AttributeFieldEditor(part, a.getAttribute());
				e.adapt(toolkit);
				IntelEntityAttributeValue initValue = entity.findAttributeValue(a.getAttribute());
				if (initValue != null) e.initControl(initValue);
				e.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						EntityEditor.this.setDirty(true);
					}
				});
				fieldEditors.add(e);
			}else{
				Label key = toolkit.createLabel(part, a.getAttribute().getName() + ":");
				key.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
				
				String text = "";
				for (IntelEntityAttributeValue v : entity.getAttributes()){
					if (v.getAttribute().equals(a.getAttribute())){
						text = AttributeValueLabelProvider.INSTANCE.getText(v);
						break;
						
					}
				}
				Text tmp = toolkit.createText(part, text, SWT.BORDER);
				tmp.setEditable(false);
//				tmp.setEnabled(false);
//				Label tmp = toolkit.createLabel(part, text, SWT.BORDER);
				tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
			
		}
		compAttributes.layout(true);
		
		//attachment composite
		if (isEditMode){
			attachmentEditPanel.setVisible(true);
			((GridData)attachmentEditPanel.getLayoutData()).heightHint = attachmentEditPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		}else{
			attachmentEditPanel.setVisible(false);
			((GridData)attachmentEditPanel.getLayoutData()).heightHint = 0;
		}
		attachmentEditPanel.getParent().layout(true);
		attachmentTable.setEntity(entity);
	}
	
	private GridLayout createGridLayoutNoMargin(int col){
		GridLayout gd = new GridLayout(col, false);
		gd.marginWidth = 0;
		gd.marginHeight = 0;
		return gd;
	}
	@Override
	public void setFocus() {

	}

}
