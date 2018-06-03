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

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.ui.CheckBoxDropDown;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Composite that contains relationship graph filtering components.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphFilterComposite extends Composite {
	
	private static final List<Integer> GRAPH_DEPTH_OPTIONS = Arrays.asList(1 ,2, 3, 4);
	
	private ComboViewer cmbDepth;
	private CheckBoxDropDown cmbEntityTypes;
	private CheckBoxDropDown cmbRelationTypes;
	
	private LoadEntityTypeJob entityTypeJob = new LoadEntityTypeJob() {
		@Override
		protected void processData(List<IntelEntityType> types) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					cmbEntityTypes.setInput(types);
				}		
			});
		}
	};
	
	private LoadRelationshipTypeJob relationshipTypeJob = new LoadRelationshipTypeJob() {
		@Override
		protected void processData(List<IntelRelationshipType> types) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					cmbRelationTypes.setInput(types);
				}		
			});
		}
	};

	public RelationshipGraphFilterComposite(Composite parent) {
		super(parent, SWT.NONE);
		GridLayout layout = new GridLayout(6, false);
		layout.marginHeight = layout.marginWidth = 0;
		this.setLayout(layout);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createContent(this);
	}

	private void createContent(Composite parent) {
		Label lblDepth = new Label(parent, SWT.NONE);
		lblDepth.setText("Depth:");

		cmbDepth = new ComboViewer(this, SWT.READ_ONLY | SWT.BORDER);
		cmbDepth.getCombo().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbDepth.setContentProvider(ArrayContentProvider.getInstance());
		cmbDepth.setInput(GRAPH_DEPTH_OPTIONS);
		cmbDepth.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				//TODO: ZZZZZZZZ implement
			}
		});
		
		Label lblEntityTypes = new Label(parent, SWT.NONE);
		lblEntityTypes.setText("Entity Types:");

		cmbEntityTypes = new CheckBoxDropDown(parent);
		cmbEntityTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbEntityTypes.setLabelProvider(new EntityTypeLabelProvider());
		cmbEntityTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbEntityTypes.setInput(Arrays.asList(DialogConstants.LOADING_TEXT));
		cmbEntityTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				//TODO: ZZZZZZZZ implement
			}
		});
		
		Label lblRelationTypes = new Label(parent, SWT.NONE);
		lblRelationTypes.setText("Relationship Types:");

		cmbRelationTypes = new CheckBoxDropDown(parent);
		cmbRelationTypes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		cmbRelationTypes.setLabelProvider(new RelationshipTypeLabelProvider());
		cmbRelationTypes.setContentProvider(ArrayContentProvider.getInstance());
		cmbRelationTypes.setInput(Arrays.asList(DialogConstants.LOADING_TEXT));
		cmbRelationTypes.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				//TODO: ZZZZZZZZ implement
			}
		});
		
		entityTypeJob.schedule();
		relationshipTypeJob.schedule();
	}

}
