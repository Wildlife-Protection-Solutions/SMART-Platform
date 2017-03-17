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
package org.wcs.smart.report.birt.map.properties;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.ElementIdDescriptorProvider;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.IDescriptorProvider;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.TextPropertyDescriptorProvider;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.provider.UnitPropertyDescriptorProvider;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.ComplexUnitSection;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.Section;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.SeperatorSection;
import org.eclipse.birt.report.designer.internal.ui.views.attributes.section.TextSection;
import org.eclipse.birt.report.designer.ui.views.attributes.AttributesUtil;
import org.eclipse.birt.report.model.api.ReportItemHandle;
import org.eclipse.birt.report.model.api.elements.ReportDesignConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.report.birt.map.item.SmartMapItem;

/**
 * Smart map item general properties page.
 * 
 * @author Emily
 * 
 */
public class SmartMapItemGeneralPage extends AttributesUtil.PageWrapper {
	protected FormToolkit toolkit;
	protected Object input;
	protected Composite contentpane;

	private List<Section> sections;

	public void buildUI(Composite parent) {
		if (toolkit == null) {
			toolkit = new FormToolkit(Display.getCurrent());
			toolkit.setBorderStyle(SWT.NULL);
		}
		sections = new ArrayList<Section>();
		Control[] children = parent.getChildren();

		if (children != null && children.length > 0) {
			contentpane = (Composite) children[children.length - 1];

			GridLayout layout = new GridLayout(6, false);
			layout.marginLeft = 8;
			layout.verticalSpacing = 12;
			layout.horizontalSpacing = 12;
			contentpane.setLayout(layout);

			// name
			TextPropertyDescriptorProvider nameProvider = new TextPropertyDescriptorProvider(
					ReportItemHandle.NAME_PROP,
					ReportDesignConstants.EXTENDED_ITEM);
			TextSection nameSection = new TextSection(
					nameProvider.getDisplayName(), contentpane, true);
			nameSection.setProvider(nameProvider);
			nameSection.setLayoutNum(2);
			nameSection.setGridPlaceholder(0, true);
			nameSection.setWidth(200);
			sections.add(nameSection);

			// element id
			ElementIdDescriptorProvider elementIdProvider = new ElementIdDescriptorProvider();
			TextSection elementIdSection = new TextSection(
					elementIdProvider.getDisplayName(), contentpane, true);
			elementIdSection.setProvider(elementIdProvider);
			elementIdSection.setWidth(200);
			elementIdSection.setLayoutNum(4);
			elementIdSection.setGridPlaceholder(2, true);
			sections.add(elementIdSection);

			// separator
			SeperatorSection seperator1 = new SeperatorSection(contentpane,
					SWT.HORIZONTAL);
			sections.add(seperator1);

			// width
			IDescriptorProvider provider = new UnitPropertyDescriptorProvider(
					ReportItemHandle.WIDTH_PROP,
					SmartMapItem.EXTENSION_NAME);
			ComplexUnitSection widthPropSelection = new ComplexUnitSection(
					provider.getDisplayName(), contentpane, true);
			widthPropSelection.setProvider(provider);
			widthPropSelection.setWidth(200);
			widthPropSelection.setLayoutNum(2);
			sections.add(widthPropSelection);

			// height
			provider = new UnitPropertyDescriptorProvider(
					ReportItemHandle.HEIGHT_PROP,
					SmartMapItem.EXTENSION_NAME);
			ComplexUnitSection heightPropSelection = new ComplexUnitSection(
					provider.getDisplayName(), contentpane, true);
			heightPropSelection.setProvider(provider);
			heightPropSelection.setWidth(200);
			heightPropSelection.setLayoutNum(4);
			heightPropSelection.setGridPlaceholder(2, true);
			sections.add(heightPropSelection);

			//dpi
			provider = new DpiPropertyProvider(
					SmartMapItem.SMART_DPI,
					SmartMapItem.EXTENSION_NAME);
			
			DpiComboSection dpiSection = new DpiComboSection(provider.getDisplayName(), contentpane, true);
			dpiSection.setProvider(provider);
			dpiSection.setWidth(200);
			sections.add(dpiSection);
			
			for (Section sec : sections) {
				sec.createSection();
				sec.layout();
			}
			
			contentpane.layout(true);
			contentpane.redraw();
		}
	}

	@Override
	public void setInput(Object input) {
		this.input = input;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (toolkit != null) {
			toolkit.dispose();
		}
	}

	private void adaptFormStyle(Composite comp) {
		Control[] children = comp.getChildren();
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Composite) {
				adaptFormStyle((Composite) children[i]);
			}
		}

		toolkit.paintBordersFor(comp);
		toolkit.adapt(comp);
	}

	@Override
	public void refresh() {
		if (contentpane != null && !contentpane.isDisposed()) {
			if (toolkit == null) {
				toolkit = new FormToolkit(Display.getCurrent());
				toolkit.setBorderStyle(SWT.NULL);
			}

			adaptFormStyle(contentpane);

			updateUI();
		}
	}

	@Override
	public void postElementEvent() {
		if (contentpane != null && !contentpane.isDisposed()) {
			updateUI();
		}
	}

	/*
	 * Update ui components
	 */
	protected void updateUI() {
		for (Section sec : sections) {
			sec.setInput(input);
			sec.load();
		}
	}
}