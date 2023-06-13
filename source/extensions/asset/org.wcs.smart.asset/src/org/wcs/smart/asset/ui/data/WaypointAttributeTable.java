/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.ui.ObservationDialog;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Manages a collection of waypoint observations, displaying them
 * to the user and allowing the user to modify them.
 * 
 * @author Emily
 *
 */
public class WaypointAttributeTable {

	private Waypoint waypoint;
	
	private Composite parent;
	private FormToolkit toolkit;
	private AssetDataPanel page;
	
	private List<RowItem> rows;
	private int lastSelection;

	private Color selectionColor = null;
	private Color mouseOverColor = null;
	
	public WaypointAttributeTable(Composite parent, FormToolkit toolkit, Waypoint waypoint, AssetDataPanel page) {
		this.waypoint = waypoint;
		this.parent = parent;
		this.toolkit = toolkit;
		this.page = page;
	
		selectionColor = page.selectionColor;
		mouseOverColor = page.mouseOverColor;
		
		createComposite();
	}
	
	/**
	 * recreates the table 
	 */
	public void refresh() {
		createComposite();
		page.resizeScroll();
	}
	
	private void createComposite() {
		for (Control c : parent.getChildren()) c.dispose();
		
		rows = new ArrayList<>();
		if (waypoint.getObservationGroups() != null && !waypoint.getObservationGroups().isEmpty()) {
			try(Session session = HibernateManager.openSession()){
				Waypoint temp = session.get(Waypoint.class, this.waypoint.getUuid());
				for (WaypointObservationGroup g : temp.getObservationGroups()) {
					Composite groupcomp = new Composite(parent, SWT.NONE);
					groupcomp.setLayout(new GridLayout());
					((GridLayout)groupcomp.getLayout()).marginWidth = 0;
					((GridLayout)groupcomp.getLayout()).marginHeight = 0;
					groupcomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					groupcomp.setBackground(groupcomp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					
					if (temp.getObservationGroups().size() > 1) SmartUiUtils.createHeaderLabel(groupcomp, Messages.WaypointAttributeTable_ObsGroupHeader);
					for (WaypointObservation obs : g.getObservations()) {
						RowItem item = new RowItem(obs);
						item.createControl(groupcomp);
						rows.add(item);
					}
					groupcomp.getChildren()[groupcomp.getChildren().length-1].dispose();
				}
			}
		}else if (page.isEdit()){
			Composite placeholder = toolkit.createComposite(parent, SWT.NONE);
			placeholder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			placeholder.addListener(SWT.MouseUp, event->{
				if (event.button == 3) {
					createMenu(placeholder);
					return;
				}
			});
			placeholder.setLayout(new GridLayout());
			ToolBar addtb = new ToolBar(placeholder, SWT.FLAT);
			addtb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
			toolkit.adapt(addtb);
			ToolItem addItem = new ToolItem(addtb, SWT.PUSH);
			addItem.setToolTipText(Messages.WaypointAttributeTable_addobstooltip);
			addItem.setEnabled(true);
			addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			addItem.addListener(SWT.Selection, e->addObservation());
			
		}
		
		parent.layout(true, true);
	}
	
	
	private void processMouseClickEvent(RowItem item, Event event) {
		int index = rows.indexOf(item);

		if ((event.stateMask & SWT.CTRL) != 0){
			item.setSelected(!item.isSelected);
		}else if ((event.stateMask & SWT.SHIFT) != 0){
			boolean newSelection = !item.isSelected;
			//clearSelection();
			
			int from = lastSelection;
			int to = index;
			if (index < lastSelection){
				from = index;
				to = lastSelection;
			}
			
			for (int i = from; i <= to; i ++){
				if (i == index){
					rows.get(i).setSelected(true);
				}else{
					rows.get(i).setSelected(newSelection);		
				}
			}
		}else{
			boolean newSelection = item.isSelected;
			if (event.button == 1) {
				newSelection = !newSelection;
			}else {
				newSelection = true;
			}
			if (event.button != 3 || !item.isSelected) {
				rows.forEach(e->e.setSelected(false));
				item.setSelected(newSelection);
			}
		}	
		lastSelection = index;
		
		if (event.button == 3) {
			createMenu(item.outerPart);
			return;
		}
	}
	
	private void modifyObservation(WaypointObservation wo) {
		if (!page.isEdit()) return;
		ObservationDialog dialog = new ObservationDialog(parent.getShell(), wo);
		if (dialog.open() == Window.OK) {
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try {
					
					//TODO: test and fix this 
					for (WaypointObservationGroup g : wo.getWaypoint().getObservationGroups()) {
						session.merge(g);
						//session.saveOrUpdate(g);
						//for (WaypointObservation wwo : g.getObservations()) {
						//	session.saveOrUpdate(wwo);
						//}
					}
					session.flush();
					
					//evict
					for (WaypointObservationGroup g : wo.getWaypoint().getObservationGroups()) {
						session.evict(g);
					}
					
					//delete empty groups and removed observations
					Waypoint wp  = session.get(Waypoint.class, wo.getWaypoint().getUuid());
					List<WaypointObservationGroup> tod = new ArrayList<>();
					for (WaypointObservationGroup g : wp.getObservationGroups()) {
						if (!wo.getWaypoint().getObservationGroups().contains(g)) {
							session.remove(g);
							tod.add(g);
						}
					}
					wp.getObservationGroups().removeAll(tod);
					
					List<WaypointObservation> otd = new ArrayList<>();
					for (WaypointObservation wwo : wp.getAllObservations()) {
						if (!wo.getWaypoint().getAllObservations().contains(wwo)) otd.add(wwo);
					}
					for (WaypointObservation wwo : otd) {
						wwo.getObservationGroup().getObservations().remove(wwo);
						session.remove(wwo);
					}
					
//					for (WaypointObservationGroup g : wp.getObservationGroups()) {
//						if (g.getObservations().isEmpty()) session.delete(g);
//					}
					
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					AssetPlugIn.displayLog(Messages.WaypointAttributeTable_SaveError + ex.getMessage(), ex);
				}
			}
			refresh();
		}
	}
	
	private void addObservation() {
		if (!page.isEdit()) return;
		ObservationDialog dialog = new ObservationDialog(parent.getShell(), waypoint);
		if (dialog.open() == Window.OK) {
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try {
					session.merge(waypoint);
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					AssetPlugIn.displayLog(Messages.WaypointAttributeTable_SaveError + ex.getMessage(), ex);
				}
			}
			refresh();
		}
	}
	
	private void deleteObservation(Collection<WaypointObservation> wos) {
		if (!page.isEdit()) return;
		Set<UUID> toDelete = wos.stream().map(e->e.getUuid()).collect(Collectors.toSet());
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				waypoint = session.get(Waypoint.class, waypoint.getUuid());
				List<WaypointObservationGroup> empty = new ArrayList<>();
				for (WaypointObservationGroup g : waypoint.getObservationGroups()) {
					List<WaypointObservation> remove = new ArrayList<>();
					for (WaypointObservation wo : g.getObservations()) {
						if (toDelete.contains(wo.getUuid())) {
							remove.add(wo);
						}
					}
					g.getObservations().removeAll(remove);
					remove.forEach(r->session.remove(r));
					if (g.getObservations().isEmpty()) empty.add(g);
				}
				waypoint.getObservationGroups().removeAll(empty);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog(Messages.WaypointAttributeTable_SaveError + ex.getMessage(), ex);
			}
		}
		refresh();
	}
	
	private void createMenu(Control control) {
		if (!page.isEdit()) return;
		List<WaypointObservation> selected = new ArrayList<>();
		for (RowItem item : rows) {
			if (item.isSelected) selected.add(item.observation);
		}
		
		Menu mnu = new Menu(control);
		
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection, e->addObservation());
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->modifyObservation(selected.get(0)));
		if (selected.size() != 1) mnuEdit.setEnabled(false);
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->deleteObservation(selected));
		mnuDelete.setEnabled(!selected.isEmpty());
		
		mnu.setVisible(true);
	}
	
	private class RowItem{
		
		private WaypointObservation observation;
		
		private boolean isSelected = false;
		private boolean isMouseOver = false;
		
		private Composite outerPart;
		private Color bgColor = null;
		
		public RowItem(WaypointObservation observation) {
			this.observation = observation;
		}
		
		public void setSelected(boolean isSelected) {
			if (this.isSelected == isSelected) return;
			
			this.isSelected = isSelected;
			if (isSelected) {
				AssetDataPanel.colorControl(outerPart, selectionColor);
			}else {
				AssetDataPanel.colorControl(outerPart, bgColor);
			}
			outerPart.redraw();
		}
		
		public void createControl(Composite parent) {
			Listener clickListener = e->{
				switch(e.type) {
				case SWT.MouseUp:
					processMouseClickEvent(RowItem.this, e);
				case SWT.MouseEnter:
					this.isMouseOver = true;
					if (!isSelected) AssetDataPanel.colorControl(outerPart, mouseOverColor);
					outerPart.redraw();
					break;
				case SWT.MouseExit:
					this.isMouseOver = false;
					if (isSelected) AssetDataPanel.colorControl(outerPart, selectionColor);
					else AssetDataPanel.colorControl(outerPart, bgColor);
					outerPart.redraw();
					break;
				case SWT.MouseDoubleClick:
					modifyObservation(observation);
					break;
				}
			};
			
			outerPart = toolkit.createComposite(parent, SWT.NONE);
			outerPart.setLayout(new GridLayout(2, false));
			outerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)outerPart.getLayout()).marginWidth = 2;
			((GridLayout)outerPart.getLayout()).marginHeight = 2;
			((GridLayout)outerPart.getLayout()).verticalSpacing = 0;
			bgColor = outerPart.getBackground();
			
			Composite c = toolkit.createComposite(outerPart, SWT.NONE);
			c.setLayout(new GridLayout(2, false));
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)c.getLayout()).marginWidth = 0;
			((GridLayout)c.getLayout()).marginHeight = 0;
			((GridLayout)c.getLayout()).verticalSpacing = 0;
			
			Label ll = toolkit.createLabel(c, observation.getCategory().getName());
			ll.setToolTipText(observation.getCategory().getFullCategoryName());
			ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
			if (observation.getAttributes() != null) {
				for (WaypointObservationAttribute a : observation.getAttributes()) {
					
					ll = toolkit.createLabel(c, a.getAttribute().getName() + ":"); //$NON-NLS-1$
					ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
					((GridData)ll.getLayoutData()).horizontalIndent = 20;
					
					ll = toolkit.createLabel(c, a.getAttributeValueAsString(Locale.getDefault()), SWT.WRAP);
					ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					((GridData)ll.getLayoutData()).widthHint = 50;
					
				}
			}
			
			Label l = toolkit.createLabel(parent, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			if (page.isEdit()) {
				outerPart.addPaintListener(e->{
					if (isMouseOver) {
						e.gc.setLineWidth(2);
						e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE));
					}else if (isSelected) {
						e.gc.setLineWidth(2);
						e.gc.setForeground(selectionColor);
					}else {
						e.gc.setLineWidth(2);
						e.gc.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BORDER));
					}
				});	
			
				ToolBar toolbar = new ToolBar(outerPart, SWT.VERTICAL | SWT.FLAT);
				toolkit.adapt(toolbar);
				toolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
				
				ToolItem btnEdit = new ToolItem(toolbar, SWT.PUSH);
				btnEdit.setToolTipText(Messages.WaypointAttributeTable_edittooltip);
				btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
				btnEdit.addListener(SWT.Selection, e->modifyObservation(observation));
				
				ToolItem btnDelete = new ToolItem(toolbar, SWT.PUSH);
				btnDelete.setToolTipText(Messages.WaypointAttributeTable_deletetooltip);
				btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
				btnDelete.addListener(SWT.Selection, e->deleteObservation(Collections.singleton(observation)));
				
				
				AssetDataPanel.forEachChild(outerPart, e->{
					e.addListener(SWT.MouseEnter, clickListener);
					e.addListener(SWT.MouseExit, clickListener);
					e.addListener(SWT.MouseUp, clickListener);
					e.addListener(SWT.MouseDoubleClick, clickListener);
					return true;
				});
			}
			

			
		}
	}
}
