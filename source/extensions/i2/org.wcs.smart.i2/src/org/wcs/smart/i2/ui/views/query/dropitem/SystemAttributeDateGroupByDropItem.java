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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;
import org.wcs.smart.i2.query.ListItem;
import org.wcs.smart.i2.query.observation.filter.GroupByItem;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;

/**
 * Intelligence attribute group by drop item
 * 
 * @author Emily
 *
 */
public class SystemAttributeDateGroupByDropItem extends DropItem implements IGroupByDropItem{

	
	private SystemAttributeFilter.SystemAttribute attribute;
	
	private ComboViewer cmbOptions;
	
	private GroupByItem.DateOption initDateOption = null;
	
	public SystemAttributeDateGroupByDropItem(SystemAttributeFilter.SystemAttribute attribute) {
		this.attribute = attribute;
	}

	public void setDateOption(GroupByItem.DateOption dateOption) {
		this.initDateOption = dateOption;
		if(cmbOptions != null) cmbOptions.setSelection(new StructuredSelection(initDateOption));
	}
	
	@Override
	public String getText() {
		return MessageFormat.format("{0}", IntelligenceLabelProviderImpl.getName(attribute)); //$NON-NLS-1$
	}

	@Override
	public String asQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(SystemAttributeFilter.SA_KEY);
		sb.append(GroupByItem.INTERNAL_SEPERATOR);
		sb.append(attribute.name().toLowerCase(Locale.ROOT));
		sb.append(GroupByItem.INTERNAL_SEPERATOR);
		if (cmbOptions != null) {
			Object x = cmbOptions.getStructuredSelection().getFirstElement();
			if (x instanceof GroupByItem.DateOption) {
				sb.append(((GroupByItem.DateOption) x).getKey());
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
	}
	
	private void updateLabel() {
		super.getTargetPanel().redraw();
		queryChanged();
	}
	
	@Override
	public List<ListItem> getListOptions() {
		return Collections.emptyList();
	}
	
}
