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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelEntityTypeAttributeGroup;
import org.wcs.smart.i2.model.OtherAttributeGroup;
import org.wcs.smart.i2.ui.AttachmentPopoutShell;
import org.wcs.smart.i2.ui.AttributeValueLabelProvider;
import org.wcs.smart.i2.ui.views.IntelEntitySelectionTransfer;
import org.wcs.smart.ui.Thumbnail;

/**
 * Editor for comparing entities
 * 
 * @author Emily
 *
 */
public class EntityComparisonEditor extends EditorPart{

	public static final String ID = "org.wcs.smart.i2.editor.entity.compare"; //$NON-NLS-1$

	private EntityComparisonInput input;
	private EntityComparisonTable table;
	
	private FormToolkit toolkit;
	
	@Override
	public void dispose(){
		if (getTitleImage() != null) getTitleImage().dispose();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setInput(input);
		super.setSite(site);
	
		this.input = (EntityComparisonInput) input;
	}
	
	public void addEntities(List<IntelEntity> entity){
		entity.stream().forEach(e -> input.addEntity(e));
		loadValues.schedule();
	}

	public void removeEntities(List<IntelEntity> entity){
		entity.stream().forEach(e -> input.removeEntity(e));
		loadValues.schedule();
	}
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(getSite().getShell().getDisplay());
		
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)parent.getLayout()).marginHeight = 2;
		((GridLayout)parent.getLayout()).marginWidth = 2;
		((GridLayout)parent.getLayout()).verticalSpacing = 0;
		((GridLayout)parent.getLayout()).horizontalSpacing = 0;
		toolkit.adapt(parent);
		
		if (IntelSecurityManager.INSTANCE.canEditEntity()){
			Hyperlink l = toolkit.createHyperlink(parent, "Merge...", SWT.NONE);
			l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			
			l.addHyperlinkListener(new IHyperlinkListener() {
				@Override
				public void linkExited(HyperlinkEvent e) {
				}
				
				@Override
				public void linkEntered(HyperlinkEvent e) {
				}
				
				@Override
				public void linkActivated(HyperlinkEvent e) {
					MessageDialog.openInformation(getSite().getShell(), "TODO", "TODO: implement a dialog where users can pick which attributes to use in merged entity");
				}
			});
		}
		table = new EntityComparisonTable(parent);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		addDropTarget(parent);
		
		loadValues.schedule();
	}
	
	private void addDropTarget(Composite parent){
		DropTargetListener dropListener = new DropTargetListener() {
			private PaintListener paintListener = new PaintListener() {
				@Override
				public void paintControl(PaintEvent e) {
					e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.setLineWidth(2);
					e.gc.drawRectangle(0, 0, e.width, e.height);
				}
			};
			
			
			@Override
			public void dropAccept(DropTargetEvent event) {
				validate(event);
			}
			
			private void validate(DropTargetEvent event){
				if (IntelEntitySelectionTransfer.getTransfer().isSupportedType(event.currentDataType)){
					//lets see if we can find entity of correct type
					boolean found = false;
					IStructuredSelection s = (IStructuredSelection) IntelEntitySelectionTransfer.getTransfer().getSelection();
					for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
						Object type = (Object)iterator.next();
						if (type instanceof IntelEntity && ((IntelEntity)type).getEntityType().equals(input.getType())){
							found = true;
							break;
							
						}
					}
					if (!found){
						event.detail = DND.DROP_NONE;
					}else{
						event.detail = DND.DROP_LINK;
					}
					
				}			
			}
			@Override
			public void drop(DropTargetEvent event) {
				if (IntelEntitySelectionTransfer.getTransfer().isSupportedType(event.currentDataType)){
					//lets see if we can find entity of correct type
					IStructuredSelection s = (IStructuredSelection) IntelEntitySelectionTransfer.getTransfer().getSelection();
					List<IntelEntity> entities = new ArrayList<IntelEntity>();
					for (Iterator<?> iterator = s.iterator(); iterator.hasNext();) {
						Object type = (Object)iterator.next();
						if (type instanceof IntelEntity && ((IntelEntity)type).getEntityType().equals(input.getType())){
							entities.add((IntelEntity) type);
						}
					}
					addEntities(entities);
				}	
				
				parent.removePaintListener(paintListener);
				parent.redraw();
			}
			
			@Override
			public void dragOver(DropTargetEvent event) {
			}
			
			@Override
			public void dragOperationChanged(DropTargetEvent event) {
				
			}
			
			@Override
			public void dragLeave(DropTargetEvent event) {
				parent.removePaintListener(paintListener);
				parent.redraw();
				
			}
			
			@Override
			public void dragEnter(DropTargetEvent event) {
				parent.addPaintListener(paintListener);
				parent.redraw();
				validate(event);
			}
		};
		
		DropTarget dropTarget = new DropTarget(parent, DND.DROP_LINK);
		dropTarget.setTransfer(new Transfer[]{IntelEntitySelectionTransfer.getTransfer()});
		dropTarget.addDropListener(dropListener);	
	}

	@Override
	public void setFocus() {
		table.setFocus();
		
	}
	
	private Job loadValues= new Job("Loading Entities Values"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IntelEntityType type;
			List<IntelEntity> entities = new ArrayList<>();
			
			Session s = HibernateManager.openSession();
			try{
				type = (IntelEntityType) s.get(IntelEntityType.class, input.getType().getUuid());
				for (IntelEntityTypeAttribute a : type.getAttributes()){
					a.getAttribute().getName();
					if (a.getAttributeGroup() != null) a.getAttributeGroup().getName();
				}
				
				for (UUID uuid : input.getEntities()){
					IntelEntity i = (IntelEntity)s.get(IntelEntity.class, uuid);
					entities.add(i);
					i.getIdAttributeAsText();
					try{
						if (i.getPrimaryAttachment() != null) i.getPrimaryAttachment().computeFileLocation(s);
					}catch (Exception ex){
						Intelligence2PlugIn.log(ex.getMessage(), ex);
					}
					for (IntelEntityAttributeValue v : i.getAttributes()){
						v.getAttribute().getName();
						if (v.getAttributeListItem() != null) v.getAttributeListItem().getName();
					}
					for (IntelEntityAttachment a : i.getEntityAttachments()){
						try{
							a.getAttachment().computeFileLocation(s);
						}catch (Exception ex){
							Intelligence2PlugIn.log(ex.getMessage(), ex);
						}
					}
				}
				
			}finally{
				s.close();
			}
			
			Display.getDefault().syncExec(()->{
				table.createTable(type, entities);
			});
			return Status.OK_STATUS;
		}
		
	};
	
	
	private class EntityComparisonTable extends Composite{

		private Font headerFont = null;
		private Color headerColor ;
		private Color oddRowColor ;
		private Color evenRowColor ;
	
		public EntityComparisonTable(Composite parent) {
			super(parent, SWT.NONE);
			setLayout(new GridLayout());
			((GridLayout)getLayout()).marginHeight = 0;
			((GridLayout)getLayout()).marginWidth = 0;
			toolkit.adapt(this);
			
			headerColor = new Color(getDisplay(),79,129,189);
			oddRowColor = new Color(getDisplay(),255,255,255);
			evenRowColor = new Color(getDisplay(),219,229,241);
			
			FontData fd = getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			headerFont = new Font(getDisplay(), fd);
			
			parent.addDisposeListener(e->{
				headerColor.dispose();
				oddRowColor.dispose();
				evenRowColor.dispose();
				headerFont.dispose();
			});
			
		}
		
		public void createTable(IntelEntityType type, List<IntelEntity> entities){
			//dispose kids
			for (Control c : getChildren()){
				c.dispose();
			}
			EntityComparisonEditor.this.setPartName(input.getName());
			if (getTitleImage() != null) getTitleImage().dispose();
			
			try {
				EntityComparisonEditor.this.setTitleImage(AWTSWTImageUtils.convertToSWTImage(input.getType().getIconAsImage()));
			} catch (Exception e) {
				EntityComparisonEditor.this.setTitleImage(null);
			}
			
			ScrolledForm form = toolkit.createScrolledForm(this);
			form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			form.getBody().setLayout(new GridLayout());
			((GridLayout)form.getBody().getLayout()).marginHeight = 2;
			((GridLayout)form.getBody().getLayout()).marginWidth = 2;
			
			Composite table = toolkit.createComposite(form.getBody());
			form.setContent(table);
			
			table.setLayout(new GridLayout(entities.size() + 1, false));
			((GridLayout)table.getLayout()).marginHeight = 0;
			((GridLayout)table.getLayout()).marginWidth = 0;
			
			table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			((GridLayout)table.getLayout()).horizontalSpacing = 0;
			((GridLayout)table.getLayout()).verticalSpacing = 0;
			((GridLayout)table.getLayout()).verticalSpacing = 0;
			
			
			createHeader(table, entities);
			createContent(table, type, entities);
			
			layout(true);
		}
		
		private void createHeader(Composite parent, List<IntelEntity> entities){

			
			//spacer
			new Label(parent, SWT.NONE);
			
			for (IntelEntity et : entities){
				Label l = toolkit.createLabel(parent, et.getIdAttributeAsText(), SWT.WRAP);
				GridData gd = new GridData(SWT.LEFT, SWT.BOTTOM, false, false);
				gd.widthHint = 150;
				l.setLayoutData(gd);
				l.setFont(headerFont);
			}
			
			new Label(parent, SWT.NONE);
				
			for (IntelEntity et : entities){			
				Thumbnail thumb = new Thumbnail(et.getPrimaryAttachment(), 100);
				Composite c = thumb.createThumbnail(parent);
				GridData gd = new GridData(SWT.LEFT, SWT.CENTER, false, false);
				gd.widthHint = 100;
				gd.heightHint = 100;
				c.setLayoutData(gd);
				toolkit.adapt(c);
			}
			
			new Label(parent, SWT.NONE);
			
			for (IntelEntity et : entities){
				Hyperlink l = toolkit.createHyperlink(parent, "remove", SWT.NONE);
				GridData gd = new GridData(SWT.LEFT, SWT.FILL, false, false);
				gd.widthHint = 150;
				l.setLayoutData(gd);
				l.addHyperlinkListener(new IHyperlinkListener() {
					
					@Override
					public void linkExited(HyperlinkEvent e) {
					}
					
					@Override
					public void linkEntered(HyperlinkEvent e) {
					}
					
					@Override
					public void linkActivated(HyperlinkEvent e) {
						removeEntities(Collections.singletonList(et));
					}
				});
				
			}
		}
		
		private void createContent(Composite parent, IntelEntityType type, List<IntelEntity> entities){			
			
			Map<IntelEntityTypeAttributeGroup, List<IntelEntityTypeAttribute>> attributeMapping = new HashMap<>();
			for (IntelEntityTypeAttribute a : type.getAttributes()){
				if (attributeMapping.get(a.getAttributeGroup()) == null){
					attributeMapping.put(a.getAttributeGroup(), new ArrayList<>());
				}
				attributeMapping.get(a.getAttributeGroup()).add(a);
			}
			
			Stream<IntelEntityTypeAttributeGroup> groups = type.getAttributes().stream()
				.map(IntelEntityTypeAttribute::getAttributeGroup)
				.distinct().sorted(Comparator.nullsLast((a,b)-> ((Integer)a.getOrder()).compareTo(b.getOrder())));
				
			AttributeValueLabelProvider valueProvider = new AttributeValueLabelProvider();
			addListener(SWT.Dispose, e -> valueProvider.dispose());
			
			groups.forEach((group)->{
				Composite groupRow = toolkit.createComposite(parent);
				groupRow.setLayout(new GridLayout());
				groupRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, entities.size() + 1, 1));
				groupRow.setBackground(headerColor);
				
				
				Label info = toolkit.createLabel(groupRow, "");
				if (group == null){
					info.setText(OtherAttributeGroup.INSTANCE.getName());
				}else{
					info.setText(group.getName());
				}
				info.setBackground(headerColor);
				info.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
				
				List<IntelEntityTypeAttribute> attributes = attributeMapping.get(group);
				attributes.sort((a,b) -> ((Integer)a.getOrder()).compareTo(b.getOrder())); 
				int row = 0;	
				for (IntelEntityTypeAttribute a : attributes){
					Composite outer = toolkit.createComposite(parent);
					outer.setLayout(new GridLayout());
					outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
					((GridData)outer.getLayoutData()).widthHint = 200;
					Label l = toolkit.createLabel(outer, "", SWT.WRAP);
					l.setText(a.getAttribute().getName());
					GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
					gd.widthHint = 200;
					l.setLayoutData(gd);
					
					if (row % 2 == 0){
						l.setBackground(oddRowColor);
						outer.setBackground(oddRowColor);
					}else{
						l.setBackground(evenRowColor);	
						outer.setBackground(evenRowColor);
					}
					
					for (IntelEntity e : entities){
						outer = toolkit.createComposite(parent);
						outer.setLayout(new GridLayout());
						outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						
						l = toolkit.createLabel(outer, "", SWT.WRAP);
						l.setText(valueProvider.getText(e.findAttributeValue(a.getAttribute())));
						gd = new GridData(SWT.FILL, SWT.FILL, false, false);
						gd.widthHint = 150;
						l.setLayoutData(gd);
						
						if (row % 2 == 0){
							l.setBackground(oddRowColor);
							outer.setBackground(oddRowColor);
						}else{
							l.setBackground(evenRowColor);	
							outer.setBackground(evenRowColor);
						}
					}
					
					row++;
				}
				
			});
			Composite groupRow = toolkit.createComposite(parent);
			groupRow.setLayout(new GridLayout());
			groupRow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, entities.size() + 1, 1));
			groupRow.setBackground(headerColor);

			Label info = toolkit.createLabel(groupRow, "Attachments");
			info.setBackground(headerColor);
			info.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
			
			toolkit.createLabel(parent, "");
			for (IntelEntity e : entities){
				Composite outer = toolkit.createComposite(parent, SWT.NONE);
				outer.setLayout(new GridLayout());
				outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				
				e.getEntityAttachments().forEach(ea ->{
					Composite c = toolkit.createComposite(outer, SWT.NONE);
					c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
					((GridData)c.getLayoutData()).widthHint = 100;
					((GridData)c.getLayoutData()).heightHint = 100;
					Thumbnail thumb = new Thumbnail(ea.getAttachment());
					Composite t = thumb.createThumbnail(c);
					
					t.addListener(SWT.MouseHover, (event)->{
						AttachmentPopoutShell popout = new AttachmentPopoutShell(getShell(), ea.getAttachment());
						Point pnt = t.toDisplay(new Point(event.x, event.y));
						popout.open(new Point(pnt.x, pnt.y - (popout.getSize().y/2)));
					});
				});
				
				outer.addListener(SWT.Resize, (event)->{
					int width = outer.getClientArea().width;
					int cols = Math.floorDiv(width, 105);
					if (cols < 1) cols = 1;
					((GridLayout)outer.getLayout()).numColumns = cols;
					outer.layout(true);
				});
				
				
			}
				
		}
	}
}
