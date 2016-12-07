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
package org.wcs.smart.patrol.meta;

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
import org.wcs.smart.dataentry.meta.ScreenOptionComposite;
import org.wcs.smart.dataentry.meta.ScreenOptionGroup;
import org.wcs.smart.dataentry.model.ScreenOption;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.ui.LabelConstants;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Patrol Type/Transport screens configuration.
 *  
 * @author elitvin
 * @since 2.0.0
 */
public class TypeTransportScreenOptionComposite extends ScreenOptionComposite {

	private ScreenOption transportOption;
	
	private List<PatrolTransportType> transportTypes;

	private ComboViewer transportViewer;

	public TypeTransportScreenOptionComposite(Composite parent, ScreenOption transportOption, List<PatrolTransportType> transportTypes) {
		super(parent);
		this.transportOption = transportOption;
		this.transportTypes = transportTypes;
		
		new TransportOptionGroup(this, this.transportOption, LabelConstants.getLabel(PatrolScreenOptionMeta.TRANSPORT));
	}

	private List<PatrolTransportType> getTransportTypes() {
		return transportTypes;
	}
	
	private class TransportOptionGroup extends ScreenOptionGroup {

		public TransportOptionGroup(Composite parent, ScreenOption option, String title) {
			super(parent, option, title);
		}

		@Override
		protected void createDefaultControl(Group group) {
			transportViewer = new ComboViewer(group, SWT.READ_ONLY);
			transportViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			transportViewer.getControl().setEnabled(!transportOption.isVisible());
			transportViewer.setContentProvider(ArrayContentProvider.getInstance());
			transportViewer.setLabelProvider(new NamedItemLabelProvider());
			List<PatrolTransportType> tTypes = getTransportTypes();
	 		transportViewer.setInput(tTypes);
	 		UUID uuid = transportOption.getUuidValue();
	 		if (uuid == null && !tTypes.isEmpty()) {
	 			uuid = tTypes.get(0).getUuid();
	 			transportOption.setUuidValue(uuid);
	 		}
	 		if (uuid != null) {
	 			for (NamedItem item : tTypes) {
	 				if (item.getUuid().equals(uuid)) {
	 					transportViewer.setSelection(new StructuredSelection(item));
	 					break;
	 				}
	 			}
	 		}
			transportViewer.addSelectionChangedListener(new ISelectionChangedListener() {
	 			@Override
	 			public void selectionChanged(SelectionChangedEvent event) {
	 				IStructuredSelection selection = (IStructuredSelection) transportViewer.getSelection();
	 				Object obj = selection.getFirstElement();
	 				if (obj instanceof UuidItem) {
	 					UuidItem i = (UuidItem) obj;
	 					transportOption.setUuidValue(i.getUuid());
	 	 				fireScreenOptionListeners();
	 				}
	 			}
	 		});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean visible = getBtnDisplayPage().getSelection();
			transportOption.setVisible(visible);
			transportViewer.getControl().setEnabled(!visible);
			fireScreenOptionListeners();
		}
		
	}
	
}
