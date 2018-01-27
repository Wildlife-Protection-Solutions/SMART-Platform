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
package org.wcs.smart.asset.query.ui.definition.dropItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
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
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.model.AssetFilterOption;
import org.wcs.smart.asset.query.model.AssetQueryOptionType;
import org.wcs.smart.asset.query.ui.IAssetGroupByOptionData;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.GroupByFilterDialog;
import org.wcs.smart.util.UuidUtils;

/**
 * A patorl group by drop item
 * @author egouge
 * @since 1.0.0
 */
public class AssetGroupByDropItem extends DropItem implements IGroupByDropItem{

	private IAssetGroupByOptionData data;
	private AssetFilterOption groupBy;
	private List<ListItem> filteredValues = new ArrayList<ListItem>();
	
	private ToolTip toolTip;
	private Font smallerFont;
	
	/**
	 * Creates a new drop item
	 * @param type
	 */
	public AssetGroupByDropItem(AssetFilterOption type){
		this.groupBy = type;
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
		return groupBy.getGuiName(Locale.getDefault());
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder queryPart = new StringBuilder();
		queryPart.append("asset:"); //$NON-NLS-1$
		queryPart.append(groupBy.getKey());
		queryPart.append(":"); //$NON-NLS-1$
		if (filteredValues.size() > 0){
			for (int i = 0; i < filteredValues.size(); i ++){
				if (groupBy.getType() == AssetQueryOptionType.UUID){
					queryPart.append(  UuidUtils.uuidToString( filteredValues.get(i).getUuid())  );
				}else if (groupBy.getType() == AssetQueryOptionType.KEY){
					queryPart.append(  filteredValues.get(i).getKey() );
				}else{
					queryPart.append("\""); //$NON-NLS-1$
					queryPart.append( filteredValues.get(i).getKey() );
					queryPart.append("\""); //$NON-NLS-1$
				}
				if (i != filteredValues.size() -1){
					queryPart.append(":"); //$NON-NLS-1$
				}
				
			}
		}
		return queryPart.toString();
	}

	/**
	 * @param data - must be a array of two elements.  The first
	 * is the IAssetOptionData associated with the asset option
	 * the second is an array of ListItem[] that represents the
	 * select group of options (or null if all are selected)
	 * the selected group by options or null if all selected
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
		Object[] values = (Object[])data;
		this.data = (IAssetGroupByOptionData) values[0];
		if (values.length < 2 || values[1] == null){
			this.filteredValues.clear();
		}else{
			ListItem[] d = (ListItem[])values[1];
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
		lbl.setText( formatStringForLabel(groupBy.getGuiName(Locale.getDefault())));
		initDrag(lbl);
		
		final Hyperlink link = new Hyperlink(comp,  SWT.NONE);
		link.setUnderlined(true);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText(Messages.AssetGroupByDropItem_FiltersLabel);
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(AssetGroupByDropItem.this, filteredValues);
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
					AssetGroupByDropItem.this.queryChanged();
				}
			}
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.NONE);
		toolTip.setText(Messages.AssetGroupByDropItem_IncludedLabel);
		toolTip.setAutoHide(false);
		updateToolTipMessage();
		link.addListener(SWT.MouseHover, new Listener(){
			@Override
			public void handleEvent(Event event) {
				Point p = link.toDisplay(event.x, event.y);
				toolTip.setLocation(p.x + 15, p.y + 5);
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
		if (filteredValues == null || filteredValues.isEmpty()){
			tipStr.append(Messages.AssetGroupByDropItem_AllLabel);
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
		
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				items = data.getAllValues(s);
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.AssetGroupByDropItem_Error_LoadingListItems, ex);
			}finally {
				s.getTransaction().rollback();
			}
		}
		Collections.sort(items);
		return items;
		
	}

}
