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
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.ICombinableDropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

/**
 * A category group by item for summary queries.
 * <p>This item displays the parent category
 * and allows users to filter direct sub-categories
 * to include in group by.</p>
 * @author egouge
 * @since 1.0.0
 */
public class CategoryGroupByDropItem extends DropItem implements IGroupByDropItem, ICombinableDropItem{

	
	private int level = -1;
	private List<ListItem> filters = null;
	
	
	private Font smallerFont;
	private ToolTip toolTip;
	private Label lblText;
	
	
	public CategoryGroupByDropItem(Category category){
		this.level = Category.hkeyLength(category.getHkey());
		filters = new ArrayList<ListItem>();
		ListItem item = new ListItem(null, category.getName(), category.getHkey());
		filters.add(item);
	}
	
	public CategoryGroupByDropItem(int level){
		this.level = level;
		filters = new ArrayList<ListItem>();
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
		
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try{
				for(Category child : QueryDataModelManager.getInstance().getCategories(s, level)){
					String name = child.getName();
					if (child.getParent() != null){
						name += "   (" + child.getParent().getFullCategoryName() +")" ;  //$NON-NLS-1$//$NON-NLS-2$
					}
					items.add(new ListItem(null, name, child.getHkey())); 
				}
			}catch (Exception ex){
				QueryPlugIn.displayLog(Messages.CategoryGroupByDropItem_ErrorLoadingCategoryName, ex);
			}finally {
				s.getTransaction().rollback();
			}
		}
		return items;		
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
		if (!(dItem instanceof CategoryGroupByDropItem)){
			return false;
		}
		CategoryGroupByDropItem dropItem = (CategoryGroupByDropItem)dItem;
		if (dropItem.level != this.level){
			return false;
		}
		for (ListItem item : dropItem.filters){
			if (!this.filters.contains(item)){
				this.filters.add(item);		
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
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append (Messages.CategoryGroupByDropItem_CategoriesLabel + " - " + Messages.CategoryGroupByDropItem_TreeLevelLabel + " " + this.level + "\n");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		int cnt = 0;
		for (ListItem it : filters){
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
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("category:"); //$NON-NLS-1$
		sb.append(level);
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
	 * @param data - must be a List<ListItem> represent the categories
	 * to be included in the group by or null if all categories
	 * to be included
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
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
				dialog.setGroupByItem(CategoryGroupByDropItem.this, filters);
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
					CategoryGroupByDropItem.this.queryChanged();
					getTargetPanel().redraw();
				}
			}
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setMessage(Messages.CategoryGroupByDropItem_IncludedLabel);
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

}
