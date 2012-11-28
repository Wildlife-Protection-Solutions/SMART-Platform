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

import java.util.List;

import org.eclipse.birt.report.designer.ui.extensions.IPropertyTabUI;
import org.eclipse.birt.report.designer.ui.views.attributes.AbstractPageGenerator;
import org.eclipse.birt.report.designer.ui.views.attributes.AttributesUtil;
import org.eclipse.birt.report.designer.ui.views.attributes.TabPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;

/**
 * Smart map item page generator
 * 
 * @author Emily
 * 
 */
@SuppressWarnings("rawtypes")
public class SmartMapItemPageGenerator extends AbstractPageGenerator {
	
	private IPropertyTabUI generalPage;

	protected void buildItemContent(CTabItem item) {
		if (itemMap.containsKey(item) && itemMap.get(item) == null) {
			String title = tabFolder.getSelection().getText();

			if (SmartLayersPage.PAGE_KEY.equals(title)) {
				TabPage page = new SmartLayersPage().getPage();
				if (page != null) {
					setPageInput(page);
					refresh(tabFolder, page, true);
					item.setControl(page.getControl());
					itemMap.put(item, page);
				}
			}
		} else if (itemMap.get(item) != null) {
			setPageInput(itemMap.get(item));
			refresh(tabFolder, itemMap.get(item), false);
		}
	}

	public void refresh() {
		createTabItems(input);

		generalPage.setInput(input);
		addSelectionListener(this);
		((TabPage) generalPage).refresh();
	}

	public void createTabItems(List input) {
		if (generalPage == null || generalPage.getControl().isDisposed()) {
			tabFolder.setLayout(new FillLayout());
			generalPage = AttributesUtil
					.buildGeneralPage(
							tabFolder,
							new String[] { null, AttributesUtil.BORDER,
									AttributesUtil.MARGIN,
									AttributesUtil.SECTION,
									AttributesUtil.VISIBILITY,
									AttributesUtil.TOC,
									AttributesUtil.BOOKMARK,
									AttributesUtil.USERPROPERTIES,
									AttributesUtil.NAMEDEXPRESSIONS,
									AttributesUtil.ADVANCEPROPERTY },
							new String[] { "General" }, //$NON-NLS-1$
							new String[] { "General" }, //$NON-NLS-1$
							new AttributesUtil.PageWrapper[] { new SmartMapItemGeneralPage() },
							input);

			CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
			tabItem.setText(ATTRIBUTESTITLE);
			tabItem.setControl(generalPage.getControl());
		}

		this.input = input;
		generalPage.setInput(input);
		addSelectionListener(this);
		((TabPage) generalPage).refresh();

		createTabItem(SmartLayersPage.PAGE_KEY, ATTRIBUTESTITLE);

		if (tabFolder.getSelection() != null) {
			buildItemContent(tabFolder.getSelection());
		}
	}
}