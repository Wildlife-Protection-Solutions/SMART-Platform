/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.patrol.query;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.patrol.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
/**
 * Drop item for mobile device id group by
 * 
 * @author Emily
 * @since 8.1.0
 *
 */
public class MobileDeviceIdGroupByDropItem extends DropItem implements
		IGroupByDropItem {

	public MobileDeviceIdGroupByDropItem() {
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.IGroupByDropItem#getListItem()
	 */
	@Override
	public List<ListItem> getListItem() {
		
		List<ListItem> items ;
		
		try(Session session = HibernateManager.openSession()){		
			items = (new MobileDeviceIdGroupByViewer(null)).getItems(session);
		}
		Collections.sort(items);
		return items;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		return Messages.MobileDeviceIdGroupByDropItem_GroupByLabel;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(MobileDeviceIdParolGroupBy.KEY + ":"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Does nothing
	 * 
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#initializeData(java.lang.Object)
	 */
	@Override
	public void initializeData(Object data) {
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2, false));

		Label lbl = new Label(comp, SWT.NONE);
		lbl.setText(getText());
		initDrag(lbl);
	}

}
