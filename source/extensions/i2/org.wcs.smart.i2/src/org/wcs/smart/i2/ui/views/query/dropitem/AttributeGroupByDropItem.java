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

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem.GroupByType;

/**
 * Intelligence attribute group by drop item
 * 
 * @author Emily
 *
 */
public class AttributeGroupByDropItem extends DropItem implements IGroupByDropItem{

	
	private IntelEntityType type;
	private IntelAttribute attribute;
	
	private ComboViewer cmbOptions;
	
	private List<ListItem> filteredItems;
	private GroupByItem.DateOption initDateOption = null;
	private Area.AreaType initAreaType = null;
	
	public AttributeGroupByDropItem(IntelEntityTypeAttribute entityAttribute) {
		this(entityAttribute.getAttribute());
		this.type = entityAttribute.getEntityType();
	}
	
	public AttributeGroupByDropItem(IntelAttribute attribute) {
		this.type = null;
		this.attribute = attribute;
		filteredItems = new ArrayList<>();
	}

	public void setDateOption(GroupByItem.DateOption dateOption) {
		this.initDateOption = dateOption;
		if(cmbOptions != null) cmbOptions.setSelection(new StructuredSelection(initDateOption));
	}
	
	public void setAreaOption(Area.AreaType initAreaType) {
		this.initAreaType = initAreaType;
		if(cmbOptions != null) cmbOptions.setSelection(new StructuredSelection(initAreaType));
	}
	
	public void addFilterOption(ListItem item) {
		filteredItems.add(item);
	}
	
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append(attribute.getName());
		
		if (type != null) {
			sb.append(" [");
			sb.append(type.getName());
			sb.append("]");
		}
		return sb.toString();
	}

	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(GroupByItem.GroupByType.ATTRIBUTE.getKey());
		sb.append(":");
		sb.append(attribute.getType().key);
		sb.append(":");
		sb.append(attribute.getKeyId());
		sb.append(":");
		if (type != null) sb.append(type.getKeyId());
		sb.append(":");
		
		if (attribute.getType() == AttributeType.LIST) {
			for (ListItem key : filteredItems) {
				sb.append(key.getKeyId());
				sb.append(":");
			}
		}
		if (attribute.getType() == AttributeType.EMPLOYEE) {
			for (ListItem key : filteredItems) {
				sb.append(key.getKeyId());
				sb.append(":");
			}
		}
		if (attribute.getType() == AttributeType.DATE) {
			if (cmbOptions != null) {
				Object x = cmbOptions.getStructuredSelection().getFirstElement();
				if (x instanceof GroupByItem.DateOption) {
					sb.append(((GroupByItem.DateOption) x).getKey());
				}
			}
		}
		if (attribute.getType() == AttributeType.POSITION) {
			if (cmbOptions != null) {
				Object x = cmbOptions.getStructuredSelection().getFirstElement();
				if (x instanceof Area.AreaType) {
					sb.append(((Area.AreaType) x).name());
				}
				sb.append(":");
				for (ListItem key : filteredItems) {
					sb.append(key.getKeyId());
					sb.append(":");
				}
			}
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
		
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText( formatStringForLabel(getText()));
		initDrag(lbl);
		
		if (attribute.getType() == AttributeType.DATE) {
			cmbOptions = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbOptions.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof GroupByItem.DateOption) return ((GroupByItem.DateOption) element).name();
					return super.getText(element);
				}
			});
			cmbOptions.setContentProvider(ArrayContentProvider.getInstance());
			cmbOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbOptions.setInput(GroupByItem.DateOption.values());
			if (initDateOption != null) {
				cmbOptions.setSelection(new StructuredSelection(initDateOption));
			}else {
				cmbOptions.setSelection(new StructuredSelection(GroupByItem.DateOption.MONTH));
			}
			cmbOptions.addSelectionChangedListener(e->{updateLabel();});
			FontData fd = cmbOptions.getControl().getFont().getFontData()[0];
			fd.setHeight(fd.getHeight() - 1);
			Font f = new Font(cmbOptions.getControl().getDisplay(), fd);
			cmbOptions.getControl().setFont(f);
			cmbOptions.getControl().addListener(SWT.Dispose, e->f.dispose());
			
		}else if (attribute.getType() == AttributeType.POSITION) {
	
			cmbOptions = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbOptions.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof Area.AreaType) return ((Area.AreaType) element).name();
					return super.getText(element);
				}
			});
			cmbOptions.setContentProvider(ArrayContentProvider.getInstance());
			cmbOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbOptions.setInput(Area.AreaType.values());
			if (initAreaType != null) {
				cmbOptions.setSelection(new StructuredSelection(initAreaType));
			}else {
				cmbOptions.setSelection(new StructuredSelection(Area.AreaType.CA));
			}
			cmbOptions.addSelectionChangedListener(e->{filteredItems.clear(); updateLabel();});
			((GridLayout)main.getLayout()).numColumns = 3;
			
			FontData fd = cmbOptions.getControl().getFont().getFontData()[0];
			fd.setHeight(fd.getHeight() - 1);
			Font f = new Font(cmbOptions.getControl().getDisplay(), fd);
			cmbOptions.getControl().setFont(f);
			cmbOptions.getControl().addListener(SWT.Dispose, e->f.dispose());
			
			Link link = new Link(main, SWT.NONE);
			link.setText("<a>" + "..." + "</a>");
			link.addListener(SWT.Selection, e->openOptionDialog(parent.getShell()));
		}else {
			//list or employee
			Link link = new Link(main, SWT.NONE);
			link.setText("<a>" + "..." + "</a>");	
			link.addListener(SWT.Selection, e->openOptionDialog(parent.getShell()));
		}
	}
	private void updateLabel() {
		super.getTargetPanel().redraw();
		queryChanged();
	}
	private void openOptionDialog(Shell parent) {
		OptionDialog dialog = new OptionDialog(parent);
		dialog.setGroupByItem(this, filteredItems);
		if (dialog.open() != Window.OK) return;
		
		filteredItems.clear();
		if (dialog.getSelectedItems() != null) {
			for (ListItem i : dialog.getSelectedItems()) filteredItems.add(i);
		}
		updateLabel();
	}
	
	@Override
	public List<ListItem> getListOptions() {
		try(Session session = HibernateManager.openSession()){
			if (attribute.getType() == AttributeType.EMPLOYEE) {
				return (new GroupByItem(GroupByType.ATTRIBUTE, attribute.getKeyId(), attribute.getType(), type == null ? null : type.getKeyId(), Collections.emptyList())).getAllOptions(session, SmartDB.getCurrentConservationArea());
			}else if (attribute.getType() == AttributeType.LIST) {
				return (new GroupByItem(GroupByType.ATTRIBUTE, attribute.getKeyId(), attribute.getType(), type == null ? null : type.getKeyId(), Collections.emptyList())).getAllOptions(session, SmartDB.getCurrentConservationArea());
			}else if (attribute.getType() == AttributeType.POSITION) {
				final Area.AreaType[] atype = new Area.AreaType[] {null};
				Display.getDefault().syncExec(()->{
					atype[0] = (AreaType) cmbOptions.getStructuredSelection().getFirstElement();
				});
				return (new GroupByItem(GroupByType.ATTRIBUTE, attribute.getKeyId(), attribute.getType(), type == null ? null : type.getKeyId(), atype[0], Collections.emptyList())).getAllOptions(session, SmartDB.getCurrentConservationArea());
			}
			
		}
		return Collections.emptyList();
	}
}
