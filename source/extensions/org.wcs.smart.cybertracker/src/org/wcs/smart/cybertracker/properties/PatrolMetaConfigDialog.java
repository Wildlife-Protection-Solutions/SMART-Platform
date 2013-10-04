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
package org.wcs.smart.cybertracker.properties;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker dialog for managing patrol meta screen
 * that will be shown in generated CyberTracker application
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class PatrolMetaConfigDialog extends AbstractPropertyJHeaderDialog {

	public enum PatrolMetaScreen {
		PATROL_TYPE("Patrol Type"),
		TRANSPORT("Transport Type"),
		ARMED("Armed"),
		STATION("Station"),
		TEAM("Team"),
		MANDATE("Patrol Mandate"),
		OBJECTIVE("Patrol Objective"),
		COMMENT("Patrol Comment"),
		MEMBER("Patrol Members"),
		LEADER("Patrol Leader"),
		PILOT("Patrol Pilot");
		
		private String label;
		public String getLabel() {
			return label;
		}
	
		private PatrolMetaScreen(String label){
			this.label = label;
		}
	}
	
	private TableViewer modelListViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;
	
	public PatrolMetaConfigDialog() {
		super(Display.getDefault().getActiveShell(), "Patrol Metadata Data Collection Configuration");
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));

		modelListViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelListViewer.setLabelProvider(new PatrolMetaScreenLabelProvider());
		modelListViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelListViewer.setInput(PatrolMetaScreen.values());
		modelListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanel();
			}
		});

		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		infoInnerPanel = new Composite(rightPanel, SWT.NONE);
		infoInnerPanel.setLayout(new StackLayout());
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		return container;
	}

	private void updateRightPanel() {
		IStructuredSelection selection = (IStructuredSelection) modelListViewer.getSelection();
		Object obj = selection.getFirstElement();

		if (obj instanceof PatrolMetaScreen) {
			//TODO: implement
		} else {
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
		}
		infoInnerPanel.layout();
	}

	@Override
	protected boolean performSave() {
		// TODO Auto-generated method stub
		return false;
	}

	private class PatrolMetaScreenLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof PatrolMetaScreen) {
				PatrolMetaScreen i = (PatrolMetaScreen)element;
				return i.getLabel();
			}
			return super.getText(element);
		}
	}
}
