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
package org.wcs.smart.i2.diagram;

import org.eclipse.gef.layout.algorithms.SpringLayoutAlgorithm;
import org.eclipse.gef.zest.fx.jface.ZestContentViewer;
import org.eclipse.gef.zest.fx.jface.ZestFxJFaceModule;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.i2.RelationshipDiagramManager;
import org.wcs.smart.i2.diagram.style.RelationshipDiagramStyleLabelProvider;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.RelationshipDiagramNodeStyleOptions.ImageSizeOption;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Composite that contains controls related to relationship graph visualization.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphComposite extends Composite {

	private FormToolkit toolkit;

	private RelationshipGraphFilterComposite cmpFilter;
	private ZestContentViewer graphViewer;
	private RelationshipGraphLabelProvider graphLabelProvider;
	
	public RelationshipGraphComposite(Composite parent, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.toolkit = toolkit;
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = layout.verticalSpacing = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void createContent(Composite parent) {
		Composite topCmp = toolkit.createComposite(parent, SWT.NONE);
		topCmp.setLayout(new GridLayout(3, false));
		topCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		cmpFilter = new RelationshipGraphFilterComposite(topCmp);

		toolkit.createLabel(topCmp, "Style:");
		
		ComboViewer cmbStyle = new ComboViewer(topCmp, SWT.READ_ONLY | SWT.BORDER);
		cmbStyle.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbStyle.setContentProvider(ArrayContentProvider.getInstance());
		cmbStyle.setLabelProvider(new RelationshipDiagramStyleLabelProvider());
		cmbStyle.setInput(RelationshipDiagramManager.INSTANCE.loadStyles(getShell()));
		cmbStyle.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) cmbStyle.getStructuredSelection();
				graphLabelProvider.setStyle((RelationshipDiagramStyle)selection.getFirstElement());
				graphViewer.refresh();
			}
		});
		
		Composite mainCmp = new Composite(parent, SWT.NONE);
		StackLayout stackLayout = new StackLayout();
		stackLayout.marginHeight = stackLayout.marginWidth = 0;
		mainCmp.setLayout(stackLayout);
		mainCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite sizingCmp = toolkit.createComposite(mainCmp, SWT.NONE);
		sizingCmp.setLayout(new GridLayout());
		
		Composite graphCmp = toolkit.createComposite(mainCmp, SWT.NONE);
		graphCmp.setLayout(new FillLayout(SWT.VERTICAL));
		
		((StackLayout)mainCmp.getLayout()).topControl = graphCmp; //NOTE: this is a hack to coordinate sizing of composites with GridLayout and FillLayout
		
		graphViewer = new ZestContentViewer(new ZestFxJFaceModule());
		graphViewer.createControl(graphCmp, SWT.NONE);
		graphViewer.setContentProvider(new RelationshipGraphContentProvider());
		graphLabelProvider = new RelationshipGraphLabelProvider(graphViewer);
		graphViewer.setLabelProvider(graphLabelProvider);
		graphViewer.setLayoutAlgorithm(new SpringLayoutAlgorithm());
		graphViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				System.out.println(
						"Selection changed: " + (event.getSelection()));
			}
		});
	}

	public void setInput(IntelEntity entity) {
		graphViewer.setInput(entity);
	}

}
