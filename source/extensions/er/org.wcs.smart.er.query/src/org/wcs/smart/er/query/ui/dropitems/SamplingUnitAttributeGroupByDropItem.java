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
package org.wcs.smart.er.query.ui.dropitems;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolTip;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.er.model.SamplingUnitAttribute;
import org.wcs.smart.er.model.SamplingUnitAttributeListItem;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.GroupByFilterDialog;

/**
 * Mission attribute group by drop item.  Applicable for only
 * list mission attributes.
 * 
 * @author Emily
 *
 */
public class SamplingUnitAttributeGroupByDropItem extends DropItem implements
		IGroupByDropItem {

	private SamplingUnitAttribute attribute;
	private List<ListItem> filters = null;

	private ToolTip toolTip;
	private Font smallerFont;
	private Label lblText;
	
	/**
	 * Creates a new attribute list group by drop item for a attribute.
	 * 
	 * @param attribute
	 */
	public SamplingUnitAttributeGroupByDropItem(SamplingUnitAttribute attribute) {
		if (attribute.getType() != AttributeType.LIST) {
			throw new IllegalStateException(
					"Cannot create an attribute list drop item for a non-list attribute"); //$NON-NLS-1$
		}
		this.attribute = attribute;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<ListItem> getListItem() {
		List<ListItem> items = new ArrayList<ListItem>();
		Session session = HibernateManager.openSession();
		session.beginTransaction();
		try {
			Criteria c = session.createCriteria(SamplingUnitAttributeListItem.class)
					.add(Restrictions.eq("attribute", attribute)); //$NON-NLS-1$
			List<SamplingUnitAttributeListItem> listitems = c.list();
			
			for (SamplingUnitAttributeListItem it : listitems) {
				items.add(new ListItem(null, it.getName(), it.getKeyId()));
			}
			if (filters != null) {
				for (ListItem filter : filters) {
					// add disabled items
					if (!items.contains(filter)) {
						items.add(filter);
					}
				}
			}
			session.getTransaction().rollback();
		} catch (Exception ex) {
			ERQueryPlugIn
					.displayLog(
							Messages.SamplingUnitAttributeGroupByDropItem_LoadError,
							ex);
		}finally{
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
		sb.append (attribute.getName() + "\n");  //$NON-NLS-1$ 
		int cnt = 0;
		if (filters != null){
			for (ListItem it : filters){
				if (cnt >= 3){
					sb.append("..."); //$NON-NLS-1$
					break;
				}
				sb.append("   " + it.getName()); //$NON-NLS-1$
				sb.append("\n"); //$NON-NLS-1$
				cnt ++;
			}
		}
		return sb.toString();
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	//s:missionproperty:l:" < DM_KEY > ":" ( < DM_KEY > )? (":" < DM_KEY > )* ) 
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append("sgb:suproperty:"); //$NON-NLS-1$
		sb.append(attribute.getType().typeKey);
		sb.append(":"); //$NON-NLS-1$
		sb.append(attribute.getKeyId());
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
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		lblText = new Label(comp, SWT.NONE);
		lblText.setText(formatStringForLabel(attribute.getName()));
		initDrag(lblText);

		final Link link = new Link(comp, SWT.NONE);
		link.setText("<a>" + Messages.SamplingUnitAttributeGroupByDropItem_FiltersLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		FontData fd = (link.getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(Display.getCurrent(), fd);
		link.setFont(smallerFont);
		link.setLayoutData(new GridData(SWT.BOTTOM, SWT.RIGHT, false, false));
		link.addListener(SWT.MouseUp, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GroupByFilterDialog dialog = new GroupByFilterDialog(Display
						.getDefault().getActiveShell());
				dialog.setGroupByItem(SamplingUnitAttributeGroupByDropItem.this, filters);
				int ret = dialog.open();
				if (ret == IDialogConstants.OK_ID) {
					// save results here
					if (filters == null) {
						filters = new ArrayList<ListItem>();
					} else {
						filters.clear();
					}
					if (dialog.getSelectedItems() != null) {
						for (int i = 0; i < dialog.getSelectedItems().length; i++) {
							filters.add(dialog.getSelectedItems()[i]);
						}
					}
					updateLabel();
					queryChanged();
					getTargetPanel().redraw();
				}
			}
		});

		toolTip = new ToolTip(parent.getShell(), SWT.BALLOON);
		toolTip.setText(Messages.SamplingUnitAttributeGroupByDropItem_IncludedLabel);
		toolTip.setAutoHide(false);
		updateLabel();
		link.addMouseTrackListener(new MouseTrackAdapter() {			
			@Override
			public void mouseExit(MouseEvent e) {
				toolTip.setVisible(false);
			}
			
			@Override
			public void mouseEnter(MouseEvent e) {
				toolTip.setVisible(true);
			}
		});
	}

	private void updateToolTipMessage() {
		StringBuilder tipStr = new StringBuilder();
		if (filters == null || filters.size() == 0) {
			tipStr.append(Messages.SamplingUnitAttributeGroupByDropItem_AllLabel);
		} else {
			for (ListItem item : filters) {
				tipStr.append("'" + item.getName() + "'" + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}
		toolTip.setMessage(tipStr.toString());
	}

	/**
	 * Updates the text label
	 */
	private void updateLabel(){
		lblText.setText( formatStringForLabel(getText()));
		updateToolTipMessage();
	}
}
