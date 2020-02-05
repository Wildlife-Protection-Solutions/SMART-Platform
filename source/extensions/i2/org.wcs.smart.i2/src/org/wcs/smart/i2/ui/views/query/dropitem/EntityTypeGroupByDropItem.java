/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views.query.dropitem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem.GroupByType;

/**
 * Entity type group by drop item
 * @author Emily
 *
 */
public class EntityTypeGroupByDropItem extends DropItem implements IGroupByDropItem, ICombinableDropItem {

	private List<ListItem> types;
	
	private Label lbl;
	
	public EntityTypeGroupByDropItem() {
		types = new ArrayList<>();
	}
	
	public EntityTypeGroupByDropItem(IntelEntityType type) {
		this();
		types.add(new ListItem(type.getKeyId(), type.getName()));
	}
	
	public void addEntityType(IntelEntityType type) {
		this.types.add(new ListItem(type.getKeyId(), type.getName(), type.getName()));
		updateLabel();
	}
	
	@Override
	public boolean addItem(DropItem item) {
		if (!(item instanceof EntityTypeGroupByDropItem)) return false;
		this.types.addAll( ((EntityTypeGroupByDropItem)item).types);
		updateLabel();
		return true;
	}

	private void updateLabel() {
		if (lbl != null) {
			lbl.setText( formatStringForLabel(getText()));
			lbl.setToolTipText(getTooltip());
			super.getTargetPanel().redraw();
		}
	}
	
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		if (types == null || types.isEmpty()) {
			sb.append(Messages.EntityTypeGroupByDropItem_AllTypes);
		}else {
			for (int i = 0; i < Math.min(types.size(), 3); i ++) {
				if (i != 0) sb.append("\n"); //$NON-NLS-1$
				sb.append(types.get(i).getName());
			}
			if (types.size() > 3) {
				sb.append("\n"); //$NON-NLS-1$
				sb.append("..."); //$NON-NLS-1$
			}
		}
		
		return sb.toString();
	}
	
	private String getTooltip() {
		StringBuilder sb = new StringBuilder();
		for (ListItem t : types) {
			sb.append(t.getName());
			sb.append("\n"); //$NON-NLS-1$
		}
		return sb.toString();
	}

	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(GroupByItem.GroupByType.ENTITYTYPE.getKey());
		sb.append(GroupByItem.INTERNAL_SEPERATOR);
		for (ListItem t : types) {
			sb.append(t.getKeyId());
			sb.append(GroupByItem.INTERNAL_SEPERATOR);
		}
		return sb.toString();
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		lbl = new Label(main, SWT.NONE);
		lbl.setText( formatStringForLabel(getText()));
		lbl.setToolTipText(getTooltip());
		initDrag(lbl);
		
		Link link = new Link(main, SWT.NONE);
		link.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, false, false));
		link.setText("<a>...</a>"); //$NON-NLS-1$
		link.addListener(SWT.Selection, e->{
			OptionDialog dialog = new OptionDialog(parent.getShell());
			dialog.setGroupByItem(this, types);
			if (dialog.open() == OptionDialog.OK) {
				types.clear();
				if (dialog.getSelectedItems() == null) {
					//select all
				}else {
					for (ListItem i : dialog.getSelectedItems()) {
						types.add(i);
					}
				}
				updateLabel();
				super.queryChanged();
			}
		});
	}

	@Override
	public List<ListItem> getListOptions() {
		try(Session session = HibernateManager.openSession()){
			return (new GroupByItem(GroupByType.ENTITYTYPE, Collections.emptyList()).getAllOptions(session, ProfilesManager.INSTANCE.getActiveProfileIds(), InternalQueryManager.INSTANCE.getQueryItemProvider(), null, Locale.getDefault()));
		}
	}

}
