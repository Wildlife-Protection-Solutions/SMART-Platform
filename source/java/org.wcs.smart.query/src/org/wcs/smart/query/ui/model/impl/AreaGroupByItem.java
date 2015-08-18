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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ICombinableDropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Drop item for area group bys
 * @author Emily
 *
 */
public class AreaGroupByItem extends DropItem implements IGroupByDropItem,ICombinableDropItem {

	private AreaType type;
	private List<ListItem> filters = null;
	
	private Font smallerFont;
	private ToolTip toolTip;
	private Label lblText;
	
	/**
	 * Creates a new drop item from a given area
	 * @param area
	 */
	public AreaGroupByItem(Area area){
		this.type = area.getType();
		
		filters = new ArrayList<ListItem>();
		ListItem item = new ListItem(null, area.getName(), area.getKeyId());
		filters.add(item);
	}
	
	/**
	 * Creates a new drop item from a given area type.  This will block
	 * until all areas for the given type are loaded.
	 * 
	 * @param areaType
	 */
	public AreaGroupByItem(AreaType areaType){
		this.type = areaType;
		Job j = new Job(Messages.AreaGroupByItem_LoadingAreaJobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				filters = getListItem();
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		try{
			j.join();
		}catch (Exception ex){
			//eatme
		}
	}
	
	/**
	 * Opens and closes a hibernate session so this should
	 * be executed in a separate thread.
	 * 
	 * @see org.wcs.smart.query.ui.model.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
		
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			List<Area> areas = HibernateManager.loadAreas(type, s);
			for (Area a : areas){
				items.add(new ListItem(null, a.getName(), a.getKeyId()));
			}
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.AreaGroupByItem_LoadError, ex);
			s.close();
		}finally{
			s.getTransaction().rollback();
			s.close();
		}
		return items;		
	}

	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append (getLabel(type));
		int cnt = 0;
		if (filters != null){
			for (ListItem it : filters){
				sb.append("\n"); //$NON-NLS-1$
				if (cnt >= 3){
					sb.append("..."); //$NON-NLS-1$
					break;
				}
				sb.append("   " + it.getName()); //$NON-NLS-1$
				cnt ++;
			}
		}
		return sb.toString();
	}

	@Override
	public String asQueryPart() {
		StringBuffer sb = new StringBuffer();
		sb.append("area"); //$NON-NLS-1$
		sb.append(":"); //$NON-NLS-1$
		sb.append(type.name());
		sb.append(":"); //$NON-NLS-1$
		if (filters != null){
			for (int i =0; i < filters.size(); i ++){
				sb.append(filters.get(i).getKey());
				if (i < filters.size()-1){
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
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
		if (!(dItem instanceof AreaGroupByItem)){
			return false;
		}
		AreaGroupByItem dropItem = (AreaGroupByItem)dItem;
		if (dropItem.type != this.type){
			return false;
		}
		for (ListItem item : dropItem.filters){
			if (!this.filters.contains(item)){
				this.filters.addAll(dropItem.filters);		
			}
		}
		
		updateLabel();
		queryChanged();
		
		return true;
	}
	
	/**
	 * @param data - a list of {@link ListItem} that
	 * represent the various polygons in the layer
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void initializeData(Object data) {
		if (data == null){
			this.filters = null;
		}else{
			this.filters = (List<ListItem>)data;
		}
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
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
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
		link.setText(Messages.CategoryGroupByDropItem_FiltersLabel);
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(AreaGroupByItem.this, filters);
				int ret = dialog.open();
				if (ret == IDialogConstants.OK_ID){
					//save results here
					if (filters == null){
						filters = new ArrayList<ListItem>();
					}else{
						filters.clear();
					}
					if (dialog.getSelectedItems() != null){
						for (int i= 0; i < dialog.getSelectedItems().length; i ++){
							filters.add(dialog.getSelectedItems()[i]);
						}
					}else{
						filters.addAll(dialog.getAllOptions());
					}
					
					updateLabel();
					AreaGroupByItem.this.queryChanged();
					getTargetPanel().redraw();
				}
			}
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.CategoryGroupByDropItem_IncludedLabel);
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
	
	/**
	 * Updates the text label
	 */
	private void updateLabel(){
		lblText.setText( formatStringForLabel(getText()));
		updateToolTipMessage();
	}
	private void updateToolTipMessage(){
		StringBuilder tipStr = new StringBuilder();
		if (filters == null){
			tipStr.append(Messages.CategoryGroupByDropItem_AllLabel);
		}else{
			for (ListItem item: filters){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}
	protected String getLabel(Area.AreaType area){
		return SmartLabelProvider.getAreaTypeName(area);
	}
}
