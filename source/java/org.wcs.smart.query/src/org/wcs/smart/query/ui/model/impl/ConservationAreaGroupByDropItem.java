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
package org.wcs.smart.query.ui.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.util.UuidUtils;
/**
 * Conservation area group by drop item.
 * 
 * @author Emily
 *
 */
public class ConservationAreaGroupByDropItem extends DropItem implements IGroupByDropItem{

	private List<ListItem> filteredValues = new ArrayList<ListItem>();
	private ToolTip toolTip;
	
	private Font smallerFont;
	
	/**
	 * Creates a new drop item
	 * @param type
	 */
	public ConservationAreaGroupByDropItem(){
	}
	
	
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
			smallerFont = null;
		}
	}
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return Messages.ConservationAreaGroupByDropItem_Name;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder queryPart = new StringBuilder();
		queryPart.append("ca:"); //$NON-NLS-1$
		if (filteredValues.size() > 0){
			for (int i = 0; i < filteredValues.size(); i ++){
				queryPart.append(  UuidUtils.uuidToString( filteredValues.get(i).getUuid())  );
				if (i != filteredValues.size() -1){
					queryPart.append(":"); //$NON-NLS-1$
				}
				
			}
		}
		return queryPart.toString();
	}

	/**
	 * @param data - must be a (ListItem[]) that represents
	 * the selected group by options or null if all selected
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
		if (data == null){
			this.filteredValues.clear();
		}else{
			ListItem[] d = (ListItem[])data;
			this.filteredValues = new ArrayList<ListItem>();
			for (int i = 0; i < d.length; i ++){
				filteredValues.add(d[i]);
			}
		}		
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));
		
		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText( formatStringForLabel(getText()));
		initDrag(lbl);
		
		final Hyperlink link = new Hyperlink(comp,  SWT.NONE);
		link.setUnderlined(true);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText(Messages.ConservationAreaGroupByDropItem_FiltersLabel);
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(ConservationAreaGroupByDropItem.this, filteredValues);
				int ret = dialog.open();
				if (ret == IDialogConstants.OK_ID){
					//save results here
					filteredValues.clear();					
					if (dialog.getSelectedItems() != null){
						for (int i= 0; i < dialog.getSelectedItems().length; i ++){
							filteredValues.add(dialog.getSelectedItems()[i]);
						}
					}
					updateToolTipMessage();
					ConservationAreaGroupByDropItem.this.queryChanged();
				}
			}
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.ConservationAreaGroupByDropItem_IncludedLabel);
		toolTip.setAutoHide(false);
		updateToolTipMessage();
		link.addListener(SWT.MouseHover, new Listener(){
			@Override
			public void handleEvent(Event event) {
				toolTip.setVisible(true);
			}
		});
		
		link.addListener(SWT.MouseExit, new Listener(){
			@Override
			public void handleEvent(Event event) {
				toolTip.setVisible(false);
			}});
	}
	
	private void updateToolTipMessage(){
		StringBuilder tipStr = new StringBuilder();
		if (filteredValues == null){
			tipStr.append(Messages.ConservationAreaGroupByDropItem_AllLabel);
		}else{
			for (ListItem item: filteredValues){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}
	
	/**
	 * Opens and closes a hibernate session so this should
	 * be executed in a separate thread.
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
		for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
			items.add(new ListItem(ca.getUuid(), ca.getNameLabel()));
		}
		Collections.sort(items);
		return items;

	}

}
