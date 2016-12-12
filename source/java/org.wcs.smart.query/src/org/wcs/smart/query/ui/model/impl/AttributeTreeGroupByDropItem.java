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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ICombinableDropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;

/**
 * Tree group by drop item
 * 
 * @author egouge
 * @since 1.0.0
 */
public class AttributeTreeGroupByDropItem extends DropItem implements
		IGroupByDropItem, ICombinableDropItem {

	private Attribute attribute;
	private Integer level = null;
	private Category category;

	private List<ListItem> filters = null;

	private ToolTip toolTip;
	private Font smallerFont;

	private Label lblText;
	
	/**
	 * Creates a new attribute list group by drop item for a attribute.
	 * 
	 * @param attribute
	 */
	public AttributeTreeGroupByDropItem(AttributeTreeNode treeNode) {
		this.attribute = treeNode.getAttribute();
		this.level = Category.hkeyLength(treeNode.getHkey());
		filters = new ArrayList<ListItem>();
		filters.add(new ListItem(null, treeNode.getName(), treeNode.getHkey()));
	}

	/**
	 * Creates a new attribute list group item for a category/attribute.
	 * 
	 * @param catAtt
	 */
	public AttributeTreeGroupByDropItem(AttributeTreeNode treeNode, Category category) {
		this(treeNode);
		this.category = category;
	}

	public AttributeTreeGroupByDropItem(Attribute attribute, int level){
		this.attribute = attribute;
		this.level = level;
		filters = new ArrayList<ListItem>();
	}
	
	public AttributeTreeGroupByDropItem(Attribute attribute, int level, Category category){
		this(attribute, level);
		this.category = category;
	}
	
	/**
	 * Adds another drop item to this
	 * attribute tree drop item. Used to combine drop
	 * items with the same attribute-tree level.
	 * 
	 * @param dropItem
	 * @return
	 */
	public boolean addItem(DropItem dItem){
		if (!(dItem instanceof AttributeTreeGroupByDropItem)){
			return false;
		}
		AttributeTreeGroupByDropItem dropItem = (AttributeTreeGroupByDropItem)dItem;
		if (category == null && dropItem.category != null){
			return false;
		}else if (category != null && !category.equals(dropItem.category)){
			return false;
		}else if (!attribute.equals(dropItem.attribute)){
			return false;
		}
		if (dropItem.level.intValue() != this.level.intValue()){
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
	 * @see org.wcs.smart.query.ui.model.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			boolean showInactive = QueryFilterConfigManager.getInstance().getCurrentConfig().isShowInactiveItems();
			List<AttributeTreeNode> nodes = QueryDataModelManager.getInstance().getAttributeTreeNodes(session, attribute, level, !showInactive);
			for (AttributeTreeNode it : nodes) {
				String name = it.getName();
				if (it.getParent() != null){
					name += "   (" + it.getParent().getFullCategoryName() +")" ;  //$NON-NLS-1$//$NON-NLS-2$
				}
				items.add(new ListItem(null, name, it.getHkey(), it.getName(), it.getIsActive()));
			}
			for (ListItem filter: filters){
				if (!items.contains(filter)){
					items.add(filter);
				}
			}
			session.getTransaction().rollback();
			session.close();
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.AttributeTreeGroupByDropItem_ErrorLoadingTreeItems, ex);
			session.close();
		}
		return items;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (smallerFont != null) {
			smallerFont.dispose();
			smallerFont = null;
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append(attribute.getName());
		sb.append(" - " + Messages.AttributeTreeGroupByDropItem_TreeLevelLabel ); //$NON-NLS-1$
		sb.append(this.level);
		
		if (category != null){
			sb.append(" ("); //$NON-NLS-1$
			sb.append(category.getFullCategoryName());
			sb.append(")"); //$NON-NLS-1$
		}
		sb.append("\n"); //$NON-NLS-1$
		int cnt = 0;
		if (filters != null) {
			for (ListItem it : filters) {
				if (cnt >= 3) {
					sb.append("..."); //$NON-NLS-1$
					break;
				}
				sb.append("   " + it.getName()); //$NON-NLS-1$
				sb.append("\n"); //$NON-NLS-1$
				cnt++;
			}
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		if (category != null) {
			sb.append("category:"); //$NON-NLS-1$
			sb.append(category.getHkey());
			sb.append(":"); //$NON-NLS-1$
		}
		sb.append("attribute:"); //$NON-NLS-1$
		sb.append(attribute.getType().typeKey);
		sb.append(":"); //$NON-NLS-1$
		sb.append(attribute.getKeyId());
		sb.append(":"); //$NON-NLS-1$
		sb.append(level);
		sb.append(":"); //$NON-NLS-1$
		if (filters != null) {
			for (int i = 0; i < filters.size(); i++) {
				sb.append(filters.get(i).getKey());
				if (i < filters.size() - 1) {
					sb.append(":"); //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Takes a List<ListItem> that represent the filter (can be null if all
	 * children item to be included).
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void initializeData(Object data) {
		if (data == null) {
			filters = null;
		} else {
			filters = (List<ListItem>) data;
		}

	}

	/**
	 * Updates the text label
	 */
	private void updateLabel(){
		lblText.setText(formatStringForLabel(getText()));
		updateToolTipMessage();
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
		lblText.setText(formatStringForLabel(getText()));
		lblText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		initDrag(lblText);
		
		final Hyperlink link = new Hyperlink(comp,  SWT.NONE);
		link.setUnderlined(true);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText(Messages.AttributeTreeGroupByDropItem_FiltersLink);
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(AttributeTreeGroupByDropItem.this, filters);
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
					//update the name to the short name for ui
					for (ListItem filter : filters){
						if (filter.getShortName() != null){
							filter.updateName(filter.getShortName());
						}
					}
					
					updateLabel();
					queryChanged();
					getTargetPanel().redraw();
				}
			}
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.AttributeTreeGroupByDropItem_IncludedLabel);
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
		if (filters == null){
			tipStr.append(Messages.AttributeTreeGroupByDropItem_AllLabel);
		}else{
			for (ListItem item: filters){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}

}
