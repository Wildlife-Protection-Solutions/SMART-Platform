/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.observation.ui.input;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * Displays a list of observations group by waypoint observation group
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ObservationItemList {

	private Color selectionColor = null;
	private Color mouseOverColor = null;
	private Font boldFont= null;
	
	private List<ObservationItem> items = new ArrayList<>();
	
	private ObservationWizard wizard;
	private boolean hideDetails = false;
	
	private Menu obsMenu;
	
	private int lastIndex = -1;
	private Composite parent;
	public ObservationItemList(Composite parent, ObservationWizard wizard) {
		
		Color color = parent.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		selectionColor = new Color(parent.getDisplay(), SmartUtils.blend(new RGB(255, 255, 255), color.getRGB(), 75));
		mouseOverColor = new Color(parent.getDisplay(), SmartUtils.blend(new RGB(255, 255, 255), color.getRGB(), 90));
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		
		
		parent.addListener(SWT.Dispose, e->{
			selectionColor.dispose();
			mouseOverColor.dispose();
			boldFont.dispose();
		});
		
		this.wizard = wizard;
		this.parent = parent;
		buildObservations();
	}
	
	private void createMenu() {
		obsMenu = new Menu(parent);
		
		MenuItem miGroup = new MenuItem(obsMenu, SWT.PUSH);
		miGroup.setText(Messages.ObservationItemList_GroupMenuItem);
		miGroup.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miGroup.addListener(SWT.Selection, e->{
			List<ObservationItem> selections = new ArrayList<>();
			for (ObservationItem i : items) if (i.getSelection()) selections.add(i);
			
			if (selections.isEmpty()) return;
			
			WaypointObservationGroup newGroup = new WaypointObservationGroup();
			newGroup.setWaypoint(wizard.getWaypoint());
			wizard.getWaypoint().getObservationGroups().add(0, newGroup);
			newGroup.setObservations(new ArrayList<>());
			for (ObservationItem oi : selections) {
				oi.getWaypointObservation().getObservationGroup().getObservations().remove(oi.getWaypointObservation());
				oi.getWaypointObservation().setObservationGroup(newGroup);
				newGroup.getObservations().add(oi.getWaypointObservation());
			}
			
			buildObservations();
		});
		
		new MenuItem(obsMenu, SWT.SEPARATOR);
		
		MenuItem miHideDetails = new MenuItem(obsMenu, SWT.PUSH);
		miHideDetails.setText(Messages.ObservationItemList_HideDetails);
		miHideDetails.addListener(SWT.Selection, e->{
			this.hideDetails = !hideDetails;
			buildObservations();
		});
		
		new MenuItem(obsMenu, SWT.SEPARATOR);
		
		MenuItem miEdit = new MenuItem(obsMenu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addListener(SWT.Selection, e->{
			WaypointObservation toEdit = null;
			for (ObservationItem i : items) {
				if (i.getSelection()) {
					toEdit = i.getWaypointObservation();
					break;
				}
			}
			if (toEdit == null) return;
			wizard.editCategory(toEdit);
		});
		
		MenuItem miDelete = new MenuItem(obsMenu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->{
			List<WaypointObservation> wos = new ArrayList<>();
			for (ObservationItem i : items) {
				if (i.getSelection()) wos.add(i.getWaypointObservation());
			}
			if (wos.isEmpty()) return;
			
			if (!MessageDialog.openConfirm(parent.getShell(), Messages.ObservationItemList_DeleteTitle, 
					MessageFormat.format(Messages.ObservationItemList_DeleteMsg, wos.size()))) 
				return;
			
			for (WaypointObservation wo : wos) {
				wizard.removeObservation(wo);
			}
			buildObservations();
		});
		
		obsMenu.addMenuListener(new MenuAdapter() {
			
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = false;
				for (ObservationItem i : items) {
					if (i.getSelection()) {
						hasSelection = true;
						break;
					}
				}
				
				miGroup.setEnabled(hasSelection);
				miEdit.setEnabled(hasSelection);
				miDelete.setEnabled(hasSelection);
				
				if (hideDetails) {
					miHideDetails.setText(Messages.ObservationItemList_ShowDetails);
				}else {
					miHideDetails.setText(Messages.ObservationItemList_HideDetails);
				}
			}
		});
	}
	
	private void buildObservations( ) {
		for (Control c : parent.getChildren()) c.dispose();
		
		items = new ArrayList<>();
		if (obsMenu == null)  createMenu();

		int number = wizard.getWaypoint().getObservationGroups().size();
		for (int i = 0; i < number; i ++) {
			WaypointObservationGroup g = wizard.getWaypoint().getObservationGroups().get(i);
			if (g.getObservations().isEmpty()) continue;
			
			Composite groupc = new Composite(parent, SWT.NONE);
			groupc.setLayout(new GridLayout());
			((GridLayout)groupc.getLayout()).marginWidth = 4;
			((GridLayout)groupc.getLayout()).marginHeight = 4;
			groupc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			groupc.addListener(SWT.Paint, e->{
				e.gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
				e.gc.setLineWidth(2);
				e.gc.drawRectangle(1, 1, groupc.getSize().x-3, groupc.getSize().y-3);
			});
			
			if (number > 1 ) {
				SmartUiUtils.createHeaderLabel(groupc, Messages.ObservationItemList_ObsGroupHeader);
			}
			for (int j = 0; j < g.getObservations().size(); j ++) {
				ObservationItem item = new ObservationItem(groupc, g.getObservations().get(j));
				item.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				items.add(item);
				
				if (j < g.getObservations().size() - 1) {
					Label spacer = new Label(groupc, SWT.SEPARATOR | SWT.HORIZONTAL);
					spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
			}	
		}
		
		parent.layout(true);
		int newWidth = parent.getSize().x;
		((ScrolledComposite)parent.getParent()).setMinHeight(parent.computeSize(newWidth, SWT.DEFAULT).y);
	}
	
	
	private class ObservationItem extends Composite implements Listener{

		private WaypointObservation wo;
		
		private boolean isSelected = false;
		private boolean isMouseIn = false;
		
		public ObservationItem(Composite parent, WaypointObservation wo) {
			super(parent, SWT.NONE);
			this.wo = wo;
			
			createComposite();
		}

		public WaypointObservation getWaypointObservation() {
			return this.wo;
		}
		
		private void createComposite() {
			setLayout(new GridLayout(2, true));

			Composite left = new Composite(this, SWT.NONE);
			left.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			if (wizard.getIconSet() != null && wo.getCategory().getIcon() != null) {
				left.setLayout(new GridLayout(2, false));
				IconFile file = wo.getCategory().getIcon().getIconFile(wizard.getIconSet());
				if (file != null) {
					Label img = new Label(left, SWT.NONE);
					img.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 2));
					
					Image image = SmartUtils.getImage(file.getAttachmentFile(), 32);
					img.setImage(image);
					img.addListener(SWT.Dispose, e->image.dispose());
				}
			}else {
				left.setLayout(new GridLayout(1, false));
			}
			((GridLayout)left.getLayout()).marginWidth = 0;
			((GridLayout)left.getLayout()).marginHeight = 0;
			
			Label clabel = new Label(left, SWT.NONE);
			clabel.setText(SmartUtils.formatStringForLabel(wo.getCategory().getName()));
			
			Label clabel2 = new Label(left, SWT.WRAP );
			clabel2.setText(wo.getCategory().getParent() == null ? "" :SmartUtils.formatStringForLabel(wo.getCategory().getParent().getFullCategoryName(true)) ); //$NON-NLS-1$
			clabel2.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, true));
			((GridData)clabel2.getLayoutData()).widthHint = 100;

			clabel.setFont(boldFont);
			
			Composite right = new Composite(this, SWT.NONE);
			right.setLayout(new GridLayout(2, false));
			right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)right.getLayout()).marginWidth = 0;
			((GridLayout)right.getLayout()).marginHeight = 0;
			
			Composite attributes = new Composite(right, SWT.NONE);
			attributes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			attributes.setLayout(new GridLayout(2, false));
			((GridLayout)attributes.getLayout()).marginWidth = 0;
			((GridLayout)attributes.getLayout()).marginHeight = 0;
			
			int cnt = 0;
			
			for (WaypointObservationAttribute a : wo.getAttributes()) {
				Label l = new Label(attributes, SWT.NONE);
				l.setText(SmartUtils.formatStringForLabel(a.getAttribute().getName()) +":"); //$NON-NLS-1$
				l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));

				l = new Label(attributes, SWT.NONE);
				l.setText(SmartUtils.formatStringForLabel(a.getAttributeValueAsString(Locale.getDefault())));
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				cnt++;
				if (cnt >= 2 && hideDetails) {
					l.setText( l.getText() + "   ..."); //$NON-NLS-1$
					break;
				}
			}
			if (cnt < 2 || !hideDetails) {
				Label l = new Label(attributes, SWT.NONE);
				l.setText(Messages.ObservationItemList_Attachments);
				l.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		
				l = new Label(attributes, SWT.NONE);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setText(MessageFormat.format("{0}", wo.getAttachments().size())); //$NON-NLS-1$
			}
			
			ToolBar btns = new ToolBar(right, SWT.VERTICAL);
			btns.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
			ToolItem tiEdit = new ToolItem(btns, SWT.PUSH);
			tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			tiEdit.setToolTipText(DialogConstants.EDIT_BUTTON_TEXT);
			tiEdit.addListener(SWT.Selection, e->{
				wizard.editCategory(wo);
			});
			
			ToolItem tiDelete = new ToolItem(btns, SWT.PUSH);
			tiDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			tiDelete.setToolTipText(DialogConstants.DELETE_BUTTON_TEXT);
			tiDelete.addListener(SWT.Selection, e->{
				if (!MessageDialog.openConfirm(getShell(), Messages.ObservationItemList_DeleteTitle, Messages.ObservationItemList_DeleteObsMsg)) 
					return;
				wizard.removeObservation(wo);
				buildObservations();
			});
			configureChildren();
		}
		
		
		@Override
		public void handleEvent(Event e) {
			if (e.type == SWT.MouseEnter){
				this.isMouseIn = true;
			}else if (e.type == SWT.MouseExit){
				this.isMouseIn = false;
			}else if (e.type == SWT.MouseDown) {
				Widget w = e.widget;
				ObservationItem it = null;
				while(w != null) {
					if (w instanceof ObservationItem) {
						it = (ObservationItem)w;
						break;
					}
					if (w instanceof Control) {
						w = ((Control)w).getParent();
					}else {
						break;
					}
				}
				if (it == null) return;
				
				if (e.button == 1) { 
					updateSelection(e, it);
				}else if (e.button == 3) {
					int cnt = 0;
					for (ObservationItem i : items) {
						if (i.getSelection()) cnt++;
					}
					if (cnt <= 1 && !it.getSelection()) updateSelection(e, it);
					
					obsMenu.setLocation( ((Control)e.widget).toDisplay(e.x,e.y) ); //relative to display
					obsMenu.setVisible(true);
					return;
				}
			}
			
			colorItem();
		}
		
		private void updateSelection(Event e, ObservationItem it) {
			if(e.stateMask==SWT.SHIFT) {
				if (lastIndex == -1) {
					it.setSelection(!it.getSelection());	
				}else {
					//form  it to last index = selected
					for (ObservationItem item : items) {
						item.setSelection(false);
					}	
					int start = items.indexOf(it);
					int min = Math.min(start, lastIndex);
					int max = Math.max(start, lastIndex);
					for (int i = min; i <= max; i ++) {
						items.get(i).setSelection(true);
					}
				}
			}else if (e.stateMask == SWT.CONTROL) {
				it.setSelection(!it.getSelection());
			}else {
				for (ObservationItem item : items) {
					if (item == it) continue;
					item.setSelection(false);
				}	
				it.setSelection(!it.getSelection());
				lastIndex = items.indexOf(it);
			}
		}
		public boolean getSelection() {
			return this.isSelected;
		}
		
		public void setSelection(boolean isSelected) {
			this.isSelected = isSelected;
			colorItem();
		}
		
		private void colorItem() {
			if (isSelected) {
				setBackground(selectionColor);
			}else if (isMouseIn){
				setBackground(mouseOverColor);
			}else {
				setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
			}
		}
		private void configureChildren() {
			List<Control> c = new ArrayList<>();
			c.add(this);
			while(!c.isEmpty()) {
				Control i = c.remove(0);
				i.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				i.addListener(SWT.MouseDown, this);
				i.addListener(SWT.MouseEnter, this);
				i.addListener(SWT.MouseExit, this);
				if (i instanceof Composite) {
					for (Control kid : ((Composite)i).getChildren()) c.add(kid);
				}
			}
		}
	}
}
