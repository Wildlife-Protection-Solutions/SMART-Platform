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
package org.wcs.smart.dataentry.dialog.composite;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditorDefaultTab.ControlButton;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Info composite for {@link CmRootNode}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class CmRootNodeInfoComposite extends AbstractInfoComposite {

	private CmRootNode rootNode;

	private Button btnGpsFirst;
	
	public CmRootNodeInfoComposite(Composite parent, ConfigurableModel model) {
		super(parent, model);
		createControls();
	}
	
	private void createControls() {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		this.setLayout(layout);		
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite container = createContentContainer(this);
		createDisplayNameControls(container);
		createDisplayModeControls(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmRootNodeInfoComposite_GpsFirst);
		label.setToolTipText(Messages.CmRootNodeInfoComposite_GpsFirstTooltip);
		btnGpsFirst = new Button(container, SWT.CHECK);
		btnGpsFirst.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().getModel().setGpsFirst(btnGpsFirst.getSelection());
				fireModelChanged();
			}
		});
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				ConfigurableModel cm = getSourceObject().getModel();
				if (btnGpsFirst != null) {
					btnGpsFirst.setSelection(cm.isGpsFirst());
				}
			}
		});
	}

	public boolean isButtonValid(ConfigurableModelEditorDefaultTab.ControlButton button){
		if (button == ControlButton.DELETE){
			return false;
		}
		return true;
	}
	
	@Override
	public CmRootNode getSourceObject() {
		return rootNode;
	}
	
	public void setSourceObject(CmRootNode rootNode, Language language) {
		this.rootNode = rootNode;
		fireSourceObjectChanged(rootNode, language);
	}
	
}
