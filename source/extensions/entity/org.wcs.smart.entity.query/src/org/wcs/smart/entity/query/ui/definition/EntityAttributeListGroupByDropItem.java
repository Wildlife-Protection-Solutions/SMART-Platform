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
package org.wcs.smart.entity.query.ui.definition;

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
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.GroupByFilterDialog;

/**
 * An attribute list drop item for grouping summaries by
 * an attribute of type list. 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class EntityAttributeListGroupByDropItem extends DropItem implements
		IGroupByDropItem {

	private EntityAttribute attribute;
	
	private List<ListItem> filters = null;
	
	private ToolTip toolTip;
	private Font smallerFont;
	
	/**
	 * Creates a new attribute list group by drop item for 
	 * a attribute.
	 * @param attribute
	 */
	public EntityAttributeListGroupByDropItem(EntityAttribute attribute){
		if (attribute.getDmAttribute().getType() != AttributeType.LIST){
			throw new IllegalStateException("Cannot create an attribute list drop item for a non-list attribute"); //$NON-NLS-1$
		}
		this.attribute = attribute;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try{
			List<AttributeListItem> listitems = QueryDataModelManager.getInstance().getActiveAttributeListItems(attribute.getDmAttribute(), session);
			for (AttributeListItem it : listitems){
				items.add(new ListItem(null, it.getName(), it.getKeyId()));
			}
			if (filters != null){
				for(ListItem filter : filters){
					//add disabled items
					if (!items.contains(filter)){
						items.add(filter);
					}
				}
			}
			session.getTransaction().rollback();
			session.close();
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.EntityAttributeListGroupByDropItem_ErrorLoadingListItem, ex);
			session.close();
		}
		return items;
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
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return attribute.getName();
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("entity:"); //$NON-NLS-1$
		sb.append(attribute.getEntityType().getKeyId());
		sb.append(":attribute:"); //$NON-NLS-1$
		sb.append(attribute.getDmAttribute().getType().typeKey);
		sb.append(":"); //$NON-NLS-1$
		sb.append(attribute.getKeyId());
		sb.append(":"); //$NON-NLS-1$
		if (filters != null){
			for (int i =0; i < filters.size(); i ++){
				sb.append(filters.get(i).getKey());
				if (i < filters.size()-1){
					sb.append(":");	 //$NON-NLS-1$
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Takes a List<ListItem> that represent the
	 * filter (can be null if all children item to be included).
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void initializeData(Object data) {
		if (data == null){
			filters = null;
		}else{
			filters = (List<ListItem>)data;
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
		lbl.setText(formatStringForLabel(attribute.getEntityType().getName() + " - " + attribute.getName())); //$NON-NLS-1$
		initDrag(lbl);
		
		final Hyperlink link = new Hyperlink(comp,  SWT.NONE);
		link.setUnderlined(true);
		link.setForeground( parent.getShell().getDisplay().getSystemColor(SWT.COLOR_BLUE) );
		link.setText(Messages.EntityAttributeListGroupByDropItem_FiltersLabel);
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display.getDefault().getActiveShell());
				dialog.setGroupByItem(EntityAttributeListGroupByDropItem.this, filters);
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
					}
					updateToolTipMessage();
					queryChanged();
				}
			}
		});
		
		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.EntityAttributeListGroupByDropItem_IncludedLabel);
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
			tipStr.append(Messages.EntityAttributeListGroupByDropItem_AllLabel);
		}else{
			for (ListItem item: filters){
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}

}
