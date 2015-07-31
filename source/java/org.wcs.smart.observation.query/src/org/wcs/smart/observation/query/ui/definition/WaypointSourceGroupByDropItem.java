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
package org.wcs.smart.observation.query.ui.definition;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ICombinableDropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.GroupByFilterDialog;
/**
 * Group by option for waypoint source field
 * @author Emily
 *
 */
public class WaypointSourceGroupByDropItem extends DropItem implements IGroupByDropItem, ICombinableDropItem{ 

	private Font smallerFont;
	private ToolTip toolTip;
	private Label lblText;
	
	private List<ListItem> selectedItems = new ArrayList<ListItem>();
	
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> ops = new ArrayList<ListItem>();
		for (IWaypointSource src : WaypointSourceEngine.INSTANCE.getSupportedSources()){
			ops.add(new ListItem(null, src.getName(), src.getKey()));
		}
		return ops;
	}

	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append (Messages.WaypointSourceGroupByDropItem_WaypointSourceLabel);
		sb.append("\n"); //$NON-NLS-1$
		int cnt = 0;
		for (ListItem it : selectedItems){
			if (cnt >= 3){
				sb.append("..."); //$NON-NLS-1$
				break;
			}
			sb.append("   " + it.getName()); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			cnt ++;
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
			smallerFont = null;
		}
	}
	
	
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("wpt:src:"); //$NON-NLS-1$
		if (selectedItems != null){
			for (int i =0; i < selectedItems.size(); i ++){
				sb.append(selectedItems.get(i).getKey());
				if (i < selectedItems.size()-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	/**
	 * @param data - must be a List<ListItem> represent the waypoint source
	 * items to be included or null if all are to be included;
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void initializeData(Object data) {
		if (data == null){
			this.selectedItems = null;
		}else{
			this.selectedItems = (List<ListItem>)data;
		}
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		comp.setLayout(new GridLayout(2, false));
		
		lblText = new Label(comp, SWT.WRAP);
		lblText.setText( formatStringForLabel(getText()));
		lblText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		initDrag(lblText);
		
		final Hyperlink link = new Hyperlink(comp,  SWT.NONE);
		link.setUnderlined(true);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText(Messages.WaypointSourceGroupByDropItem_FiltersLabel);
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(link.getDisplay(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(link.getShell());
				dialog.setGroupByItem(WaypointSourceGroupByDropItem.this, selectedItems);
				int ret = dialog.open();
				if (ret == IDialogConstants.OK_ID){
					//save results here
					if (selectedItems == null){
						selectedItems = new ArrayList<ListItem>();
					}else{
						selectedItems.clear();
					}
					if (dialog.getSelectedItems() != null){
						for (int i= 0; i < dialog.getSelectedItems().length; i ++){
							selectedItems.add(dialog.getSelectedItems()[i]);
						}
					}else{
						selectedItems.addAll(dialog.getAllOptions());
					}
					
					updateLabel();
					WaypointSourceGroupByDropItem.this.queryChanged();
					getTargetPanel().redraw();
				}
			}
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setMessage(""); //$NON-NLS-1$
		toolTip.setAutoHide(true);
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
	
	/**
	 * Adds another drop item to this
	 * category drop item. Used to combine drop
	 * items with the same category level.
	 * 
	 * @param dropItem
	 * @return
	 */
	@Override
	public boolean addItem(DropItem dItem){
		if (!(dItem instanceof WaypointSourceGroupByDropItem)){
			return false;
		}
		WaypointSourceGroupByDropItem dropItem = (WaypointSourceGroupByDropItem)dItem;
		
		for (ListItem item : dropItem.selectedItems){
			if (!this.selectedItems.contains(item)){
				this.selectedItems.add(item);		
			}
		}
		
		updateLabel();
		queryChanged();
		
		return true;
	}
	
	/**
	 * Updates the text label
	 */
	private void updateLabel(){
		lblText.setText( formatStringForLabel(getText()));
		updateToolTipMessage();
	}

	private void updateToolTipMessage(){
		StringBuilder tipStr = new StringBuilder();
		if (selectedItems == null){
			tipStr.append(Messages.WaypointSourceGroupByDropItem_AllLabel);
		}else{
			for (ListItem item: selectedItems){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}

}
