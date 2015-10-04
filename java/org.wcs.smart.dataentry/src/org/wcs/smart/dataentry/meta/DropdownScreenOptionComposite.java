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
package org.wcs.smart.dataentry.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Screen option with dropdown as default value selector.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class DropdownScreenOptionComposite extends ScreenOptionComposite {

	public static final NamedItem EMPTY_DROP_OPTION = new NamedItem() {
		public UUID getUuid() {
			return null;
		}
		public String getName() {
			return Messages.DropdownScreenOptionComposite_EmptyValue;
		}
	};

	private List<NamedItem> ddInput;
	
	public DropdownScreenOptionComposite(Composite parent, ScreenOption model, String title, List<? extends NamedItem> cInput) {
		super(parent);

		ddInput = new ArrayList<NamedItem>();
		ddInput.add(EMPTY_DROP_OPTION);
		ddInput.addAll(cInput);

		new DropdownScreenOptionGroup(this, model, title);
	}

	private class DropdownScreenOptionGroup extends ScreenOptionGroup {

		private ComboViewer viewer;

		public DropdownScreenOptionGroup(Composite parent, ScreenOption option, String title) {
			super(parent, option, title);
		}

		@Override
		protected void createDefaultControl(Group group) {
			viewer = new ComboViewer(group, SWT.READ_ONLY);
			viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			((GridData)viewer.getControl().getLayoutData()).widthHint = 100;
			viewer.getControl().setEnabled(!getModel().isVisible());
			viewer.setContentProvider(ArrayContentProvider.getInstance());
			viewer.setLabelProvider(new NamedItemLabelProvider());
			viewer.setInput(ddInput);
			UUID uuid = getModel().getUuidValue();
			for (NamedItem item : ddInput) {
				if (uuid == item.getUuid() || item.getUuid().equals(uuid)) {
					viewer.setSelection(new StructuredSelection(item));
					break;
				}
			}
			viewer.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					onViewerSelectionChanged();
				}
			});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean visible = getBtnDisplayPage().getSelection();
			getModel().setVisible(visible);
			viewer.getControl().setEnabled(!visible);
			fireScreenOptionListeners();
		}
		
		protected void onViewerSelectionChanged() {
			IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
			Object obj = selection.getFirstElement();
			if (obj instanceof UuidItem) {
				UuidItem i = (UuidItem) obj;
				getModel().setUuidValue(i.getUuid());
				fireScreenOptionListeners();
			}
		}
		
	}
}
