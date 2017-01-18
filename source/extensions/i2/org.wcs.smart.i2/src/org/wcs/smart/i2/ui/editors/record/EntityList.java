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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelEntityRelationship;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.dialogs.RelationshipAttributeDialog;
import org.wcs.smart.i2.ui.editors.RelationshipSearchJob;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Displays a list of entities.
 * 
 * @author Emily
 *
 */
public class EntityList extends Composite {

	private Color selectionColor = null;
	private Color mouseOverColor = null;
	
	private Composite core = null;
	
	private List<IntelEntityRecord> entities;
	private HashMap<IntelEntity, List<IntelLocation>> locationCounter;
	
	private FormToolkit toolkit = null;
	private List<EntityComponent> components = null;
	
	private EntityListComposite listParent;
	private MenuItem mnuDelete;
	private MenuItem mnuOpen;
	private MenuItem mnuRelationship;
	
	private int entityColumnCnt = 2;
	
	public EntityList(EntityListComposite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		this.listParent = parent;
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).marginWidth = 0;
		
		Color color = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		selectionColor = new Color(parent.getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 75));
		mouseOverColor = new Color(parent.getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 90));
		
		addListener(SWT.Dispose, (e)->{
			selectionColor.dispose(); 
			mouseOverColor.dispose();
		});
	}
	
	/**
	 * set to null to configure loading message; set to empty list of results return 
	 * no records
	 * 
	 * @param entities
	 */
	public void setEntities(List<IntelEntityRecord> entities){
		locationCounter = new HashMap<IntelEntity, List<IntelLocation>>();
		for (IntelEntityLocation location : listParent.getEditor().getEntityLocationLinks()){
			List<IntelLocation> x = locationCounter.get(location.getEntity());
			if (x == null){
				x = new ArrayList<IntelLocation>();
				locationCounter.put(location.getEntity(), x);
			}
			x.add(location.getLocation());
		}
		this.entities = entities;
		createTable();
	}
	
	public void refreshTable(){
		createTable();
	}
	private void createTable(){
		if (core != null){
			core.dispose();
			core = null;
			components = null;
		}

		
		core = toolkit.createComposite(this, SWT.NONE);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;

		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (entities == null){
			toolkit.createLabel(core, "");
		}else{
			ScrolledComposite sc = new ScrolledComposite(core, SWT.V_SCROLL |  SWT.H_SCROLL);
			toolkit.adapt(sc);
			Composite main = toolkit.createComposite(sc, SWT.NONE);
			sc.setContent(main);
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			main.setLayout(new GridLayout(entityColumnCnt, true));
			((GridLayout)main.getLayout()).marginWidth = 0;
			((GridLayout)main.getLayout()).marginHeight = 0;
			main.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

			core.addListener(SWT.Resize, r->{
				sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			});
			components = new ArrayList<EntityComponent>();
			int cnt = 0;
			
			
			for (IntelEntityRecord i : entities){		
				EntityComponent entityComposite = new EntityComponent(main, i, locationCounter.get(i.getEntity()), cnt++, components);
				components.add(entityComposite);
				toolkit.adapt(entityComposite);
				entityComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
			layout(true);
			sc.setMinSize(main.computeSize(sc.getClientArea().width, SWT.DEFAULT));
			createMenu(main);
		}
		layout(true);
	}
	
	private void openEntity(){
		if (!getCurrentSelection().isEmpty()){
			(new OpenEntityHandler()).openEntity(getCurrentSelection().get(0), listParent.getEditor().getContext());
		}
	}
	
	private void createMenu(Composite parent){
		Menu mnuEntities = new Menu(parent);
		
		mnuEntities.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				if (!listParent.getEditor().getEditMode()){
					if (mnuDelete != null){
						mnuDelete.dispose();
						mnuDelete = null;
					}
					if (mnuRelationship != null){
						mnuRelationship.dispose();
						mnuRelationship = null;
					}
				}else{
					if (mnuDelete == null || mnuDelete.isDisposed()){
						mnuDelete = new MenuItem(mnuEntities, SWT.PUSH, 0);
						mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
						mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
						mnuDelete.addSelectionListener(new SelectionAdapter() {
							
							@Override
							public void widgetSelected(SelectionEvent e) {
								listParent.deleteEntityLink();
							}
						});
					}
					if (mnuRelationship == null || mnuRelationship.isDisposed()){
						mnuRelationship = new MenuItem(mnuEntities, SWT.CASCADE, 0);
						mnuRelationship.setText("New Relationship...");
						
						if (!getCurrentSelection().isEmpty()){
							
							final IntelEntity srcEntity = getCurrentSelection().get(0);
							List<IntelEntity> targets = listParent.getEditor().getRecord().getEntities()
								.stream()
								.map(ie->ie.getEntity())
								.collect(Collectors.toList());
							targets.remove(srcEntity);
							Menu mm = new Menu(mnuRelationship);
							mnuRelationship.setMenu(mm);
							
							for (IntelEntity targetEntity : targets){
								MenuItem m = new MenuItem(mm, SWT.CASCADE);
								m.setText(targetEntity.getIdAttributeAsText());
								m.setData(targetEntity);
														
								Menu rMenu = new Menu(m);
								m.setMenu(rMenu);
								rMenu.addMenuListener(new MenuListener() {
									
									@Override
									public void menuShown(MenuEvent e) {
										for (MenuItem kid : rMenu.getItems()) kid.dispose();
										
										MenuItem loading = new MenuItem(rMenu, SWT.PUSH);
										loading.setText(DialogConstants.LOADING_TEXT);
										loading.setEnabled(false);
										
										(new RelationshipSearchJob(srcEntity.getEntityType(), targetEntity.getEntityType()) {
											@Override
											protected void afterLoad() {
												Display.getDefault().syncExec(()->{
													
													loading.dispose();
													if (rtypes.isEmpty()){
														MenuItem loading = new MenuItem(rMenu, SWT.PUSH);
														loading.setText("(No Relationship Types Found)");
														loading.setEnabled(false);
														return;
													}
													for (IntelRelationshipType t : rtypes){
														MenuItem mi = new MenuItem(rMenu, SWT.PUSH);
														String name = t.getName();
														if (t.getRelationshipGroup() != null){
															name = name + " (" + t.getRelationshipGroup().getName() + ")";
														}
														mi.setText(name);
														mi.addSelectionListener(new SelectionAdapter(){

															@Override
															public void widgetSelected(
																	SelectionEvent e) {
																createRelationship(srcEntity, targetEntity, t);																
															}
															
														});
													}
												});
											}
										}).schedule(0); 
									}
									
									@Override
									public void menuHidden(MenuEvent e) {
									}
								});
							}
						}
					}
				}
				mnuOpen.setEnabled(!getCurrentSelection().isEmpty());
				if (mnuDelete != null && !mnuDelete.isDisposed()) mnuDelete.setEnabled(!getCurrentSelection().isEmpty());
				if (mnuRelationship != null && !mnuRelationship.isDisposed()) mnuRelationship.setEnabled(!getCurrentSelection().isEmpty());
				
				
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
				
			}
		});
		
		mnuOpen = new MenuItem(mnuEntities, SWT.PUSH);
		mnuOpen.setText("Open");
		mnuOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openEntity();
			}
		});
		
		List<Control> kids = new ArrayList<Control>();
		kids.add(parent);
		while(!kids.isEmpty()){
			Control c = kids.remove(0);
			if (c.getMenu() == null){
				c.setMenu(mnuEntities);
			}
			if (c instanceof Composite){
				for (Control cc : ((Composite)c).getChildren()){
					kids.add(cc);
				}
			}
		}
	}
	
	private void createRelationship(IntelEntity srcEntity, IntelEntity targetEntity, IntelRelationshipType rType){
		IntelEntity e1 = srcEntity;
		IntelEntity e2 = targetEntity;
		
		IntelEntityRelationship newRelationship = new IntelEntityRelationship();
		newRelationship.setRelationshipType(rType);
		
		boolean add = false;
		if (rType.getSourceEntityType() == null && rType.getTargetEntityType() == null){
			newRelationship.setSourceEntity(e1);
			newRelationship.setTargetEntity(e2);
			add = true;
		}else if (rType.getSourceEntityType() == null && rType.getTargetEntityType() != null){
			if (rType.getTargetEntityType().getUuid().equals(e1.getEntityType().getUuid())){
				newRelationship.setSourceEntity(e2);
				newRelationship.setTargetEntity(e1);
				add = true;
			}else if (rType.getTargetEntityType().getUuid().equals(e2.getEntityType().getUuid())){
				newRelationship.setSourceEntity(e1);
				newRelationship.setTargetEntity(e2);
				add = true;
			}
		}else if (rType.getTargetEntityType() == null && rType.getSourceEntityType() != null){
			if (rType.getSourceEntityType().getUuid().equals(e1.getEntityType().getUuid())){
				newRelationship.setSourceEntity(e1);
				newRelationship.setTargetEntity(e2);
				add = true;
			}else if (rType.getSourceEntityType().getUuid().equals(e2.getEntityType().getUuid())){
				newRelationship.setSourceEntity(e2);
				newRelationship.setTargetEntity(e1);
				add = true;
			}
		}else if (rType.getSourceEntityType().getUuid().equals(e1.getEntityType().getUuid()) &&
				rType.getTargetEntityType().getUuid().equals(e2.getEntityType().getUuid())){
			newRelationship.setSourceEntity(e1);
			newRelationship.setTargetEntity(e2);
			add = true;
		}else if (rType.getSourceEntityType().getUuid().equals(e2.getEntityType().getUuid()) &&
				rType.getTargetEntityType().getUuid().equals(e1.getEntityType().getUuid())){
			newRelationship.setSourceEntity(e2);
			newRelationship.setTargetEntity(e1);
			add = true;
		} 
		//check duplicates
		if (add){
			try{
				Session s = HibernateManager.openSession();
				try{
					s.beginTransaction();
					IntelEntityRelationship existing = (IntelEntityRelationship) s.createCriteria(IntelEntityRelationship.class)
							.add(Restrictions.eq("sourceEntity", newRelationship.getSourceEntity()))
							.add(Restrictions.eq("targetEntity", newRelationship.getTargetEntity()))
							.add(Restrictions.eq("relationshipType", newRelationship.getRelationshipType()))
							.uniqueResult();
					if (existing != null){
						MessageDialog.openInformation(getShell(), "Create Relationship", MessageFormat.format("The relationship of type {0} already exists between {1} and {2}.  Cannot duplicate relationship", rType.getName(), newRelationship.getSourceEntity().getIdAttributeAsText(), newRelationship.getTargetEntity().getIdAttributeAsText()));
						return;
					}
				
					s.save(newRelationship);
					
					if (!newRelationship.getRelationshipType().getAttributes().isEmpty()){
						RelationshipAttributeDialog dialog = new RelationshipAttributeDialog(getShell(), newRelationship);
						if (dialog.open() != Window.OK){
							//cancel creating relationship
							return;
						}
					}
					s.getTransaction().commit();
				}finally{
					s.close();
				}
				
				ArrayList<IntelEntity> modified = new ArrayList<IntelEntity>();
				modified.add(newRelationship.getSourceEntity());
				modified.add(newRelationship.getTargetEntity());
				listParent.getEditor().getContext().get(IEventBroker.class).send(IntelEvents.ENTITY_MODIFIED, modified);
				MessageDialog.openInformation(getShell(), "Relationship Created", MessageFormat.format("Relationship Created.\n\nA relationship of type {0} was successfully created between {1} and {2}", rType.getName(), newRelationship.getSourceEntity().getIdAttributeAsText(), newRelationship.getTargetEntity().getIdAttributeAsText()));
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog("Error occurred while creating new relationship." + ex.getMessage(), ex);
			}
		}else{
			MessageDialog.openInformation(getShell(), "Create Relationship", MessageFormat.format("Could not create a valide relationship of type {0} between {1} and {2}.  Cannot duplicate relationship", rType.getName(), newRelationship.getSourceEntity().getIdAttributeAsText(), newRelationship.getTargetEntity().getIdAttributeAsText()));
		}
		
	}
	
	public List<IntelEntity> getCurrentSelection(){
		ArrayList<IntelEntity> selections = new ArrayList<IntelEntity>();
		if (components == null) return selections;
		
		for (EntityComponent c : components){
			if (c.isSelected) selections.add(c.item.getEntity());
		}
		return selections;
		
	}
	private class EntityComponent extends Composite implements Listener{

		private static final int THUMB_SIZE = 30;
		
		private IntelEntityRecord item;
		private Color backgroundColor = null;
		private boolean isSelected = false;
		private boolean mouseOver = false;
		private int index;
		private List<EntityComponent> siblings;
		
		private int locationCount;
		private String locationTooltip = null;
		
		
		public EntityComponent(Composite parent, IntelEntityRecord item, List<IntelLocation> locationCnt, int index, List<EntityComponent> siblings){
			super(parent, SWT.BORDER);
			this.locationCount = locationCnt == null ? 0 : locationCnt.size();
			if (locationCnt != null){
				StringBuilder sb = new StringBuilder();
				for (IntelLocation x : locationCnt){
					sb.append(x.getId() + "\n");
				}
				locationTooltip = sb.toString();
			}
			this.item = item;
			createPart();
			this.index = index;
			this.siblings = siblings;
		}
		
		private void clearSelection(){
			for (EntityComponent c : siblings){
				c.isSelected = false;
				c.colorAll();
			}
		}
		private void createPart(){
			setLayout(new GridLayout(2, false));
			((GridLayout)getLayout()).marginHeight = 0;
			((GridLayout)getLayout()).marginWidth = 0;
			addListener(this);
			
			Thumbnail t = new Thumbnail(item.getEntity().getPrimaryAttachment(), THUMB_SIZE);
			Composite c = t.createThumbnail(this);
			addListener(c);
			toolkit.adapt(c);
			c.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			((GridData)c.getLayoutData()).widthHint = THUMB_SIZE;
			((GridData)c.getLayoutData()).heightHint = THUMB_SIZE;
			
			Composite info = toolkit.createComposite(this, SWT.NONE);
			info.setLayout(new GridLayout());
			((GridLayout)info.getLayout()).marginWidth = 0;
			((GridLayout)info.getLayout()).marginHeight = 0;
			info.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			addListener(info);
			
			Label l = toolkit.createLabel(info, item.getEntity().getIdAttributeAsText(), SWT.WRAP);
			l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			((GridData)l.getLayoutData()).widthHint = 100;
			addListener(l);
			
			
			
			List<IntelRecordAttachment> allAttachments = listParent.getEditor().getRecord().getAttachments();
			List<IntelAttachment> toShow1 = new ArrayList<IntelAttachment>();
			List<IntelEntityAttachment> toShow2 = new ArrayList<IntelEntityAttachment>();
			if(allAttachments != null){
				for (IntelEntityAttachment att : item.getEntity().getEntityAttachments()){
					for (IntelRecordAttachment a : allAttachments){
						if (att.getAttachment().equals(a.getAttachment())){
							toShow1.add(a.getAttachment());
							break;
						}
					}
				}
				for (IntelEntityAttachment att : listParent.getEditor().getSummaryPage().getAttachmentPanel().getNewEntityAttachments()){
					if(att.getEntity().equals(item.getEntity())){
						for (IntelRecordAttachment a : allAttachments){
							if (att.getAttachment().equals(a.getAttachment())){
								toShow2.add(att);
								break;
							}
						}
					}
				}
			}
			
			Composite attach = toolkit.createComposite(info, SWT.NONE);
			attach.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, true));
			((GridData)attach.getLayoutData()).widthHint = 100;
			RowLayout layout = new RowLayout();
			layout.wrap = true;
			layout.marginBottom = layout.marginLeft = layout.marginRight = layout.marginTop = 0;
			attach.setLayout(layout);
			addListener(attach);
			
			for (IntelAttachment att : toShow1){
				Thumbnail thumb = new Thumbnail(att, THUMB_SIZE-5);
				Composite c2 = thumb.createThumbnail(attach, SWT.NONE);
				toolkit.adapt(c2);
				c2.setLayoutData(new RowData(THUMB_SIZE-5,THUMB_SIZE-5));
				c2.setToolTipText(att.getFilename());
				addListener(c2);
				
			}
			for (IntelEntityAttachment att : toShow2){
				Thumbnail thumb = new Thumbnail(att.getAttachment(), THUMB_SIZE-2);
				Composite c2 = thumb.createThumbnail(attach, SWT.BORDER);
				toolkit.adapt(c2);
				c2.setLayoutData(new RowData(THUMB_SIZE-2,THUMB_SIZE-2));
				c2.setToolTipText(att.getAttachment().getFilename());
				
				Menu menu = new Menu(c2);
				c2.setMenu(menu);
				MenuItem mi = new MenuItem(menu, SWT.PUSH);
				mi.setText("Remove Attachment");
				mi.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				mi.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						listParent.getEditor().getSummaryPage().getAttachmentPanel().getNewEntityAttachments().remove(att);
						setEntities(listParent.getEditor().getRecord().getEntities());
					}
				});
			}
			
			l = toolkit.createLabel(info, MessageFormat.format("Locations: {0}", locationCount));
			l.setToolTipText(locationTooltip);
			l.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false));
			addListener(l);
		}
		
		private void addListener(Control c){
			c.addListener(SWT.MouseEnter, this);
			c.addListener(SWT.MouseExit, this);
			c.addListener(SWT.MouseDown, this);
			c.addListener(SWT.MouseDoubleClick, this);
		}
		
		private void colorAll(){
			if (backgroundColor == null){
				backgroundColor = getBackground();
			}
			Color color = null;
			if(isSelected){
				color = selectionColor;
			}else{
				if (mouseOver){
					color = mouseOverColor;
				}else{
					color = backgroundColor;
				}
			}
			
			
			List<Control> kids = new ArrayList<Control>();
			kids.add(this);
			while(!kids.isEmpty()){
				Control c = kids.remove(0);
				c.setBackground(color);
				if (c instanceof Composite){
					for (Control cc : ((Composite)c).getChildren()){
						kids.add(cc);
					}
				}
			}
		}
		
		@Override
		public void handleEvent(Event event) {
			if (event.type == SWT.MouseDoubleClick){
				openEntity();
			}else if (event.type == SWT.MouseEnter){
				mouseOver = true;
				colorAll();
			}else if (event.type == SWT.MouseExit){
				mouseOver = false;
				colorAll();
			}else if (event.type == SWT.MouseDown){
				Integer lastSelection = (Integer) getParent().getData("last_selection_index");
				if (lastSelection == null) lastSelection = 0;
				getParent().setData("last_selection_index", index);
				
				if ((event.stateMask & SWT.CTRL) != 0){
					isSelected = !isSelected;
				}else if ((event.stateMask & SWT.SHIFT) != 0){
					boolean newSelection = !isSelected;
					//clearSelection();
					int from = lastSelection;
					int to = index;
					if (index < lastSelection){
						from = index;
						to = lastSelection;
					}
					
					for (int i = from; i <= to; i ++){
						if (i == index){
							siblings.get(i).isSelected = true;
						}else{
							siblings.get(i).isSelected = newSelection;		
						}
						siblings.get(i).colorAll();
					}
					
				}else{
					
					clearSelection();
					isSelected = true;
				}
				colorAll();
			}
		}
		
	}
	
    private static int blend(int v1, int v2, int ratio) {
        int b = (ratio * v1 + (100 - ratio) * v2) / 100;
        return Math.min(255, b);
    }

    /**
     * Blends c1 and c2 based in the provided ratio.
     * 
     * @param c1
     *            first color
     * @param c2
     *            second color
     * @param ratio
     *            percentage of the first color in the blend (0-100)
     * @return the RGB value of the blended color
     * @since 3.1
     */
    private static RGB blend(RGB c1, RGB c2, int ratio) {
        int r = blend(c1.red, c2.red, ratio);
        int g = blend(c1.green, c2.green, ratio);
        int b = blend(c1.blue, c2.blue, ratio);
        return new RGB(r, g, b);
    }
}
