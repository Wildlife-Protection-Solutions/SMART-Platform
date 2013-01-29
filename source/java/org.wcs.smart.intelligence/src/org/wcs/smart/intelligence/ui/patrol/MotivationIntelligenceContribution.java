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
package org.wcs.smart.intelligence.ui.patrol;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;

/**
 * Motivation Intelligence contribution item for the patrol editor.
 * Lists all the intelligence that was motivated this patrol.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class MotivationIntelligenceContribution implements IPatrolEditorContribution {

	private Label label;
	private Hyperlink labelLink;
	
	private TableViewer tableViewer;
	private Hyperlink tableLink;

	public MotivationIntelligenceContribution() {}

	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent) {
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(2, false));
		
		label = toolkit.createLabel(main, ""); //$NON-NLS-1$
		//toolkit.createLabel(main, ""); //$NON-NLS-1$
		labelLink = createEditLink(toolkit, main);
		
		Table reportedTable = toolkit.createTable(main, SWT.V_SCROLL | SWT.H_SCROLL);
		tableViewer = new TableViewer(reportedTable);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		tableViewer.setLabelProvider(new IntelligenceLabelProvider());
		reportedTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		tableLink = createEditLink(toolkit, main);
		
		return main;
	}

	@Override
	public String getName() {
		return "Motivation Intelligence";
	}

	@Override
	public void setPatrol(Patrol patrol) {
		List<Intelligence> data = IntelligenceHibernateManager.getMotivatedIntelligences(patrol);
		if (data.isEmpty()) {
			label.setText("This patrol was not motivated by intelligence.");
			tableViewer.getControl().setVisible(false);
			labelLink.setVisible(true);
			tableLink.setVisible(false);
		} else {
			label.setText("Intelligence that motivated this patrol:");
			tableViewer.getControl().setVisible(true);
			tableViewer.setInput(data.toArray());
			labelLink.setVisible(false);
			tableLink.setVisible(true);
		}
	}

	/**
	 * Creates an edit hyperlink button
	 * @param toolkit toolkit
	 * @param parent parent composite
	 * @return hyperlink created
	 */
	private Hyperlink createEditLink(FormToolkit toolkit, Composite parent) {
		Hyperlink editLink = toolkit.createHyperlink(parent, Messages.IntelligenceEditor_Edit_LinkLabel, SWT.WRAP);
		editLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				showEditDialog();
			}
		});
		return editLink;
	}

	/**
	 * Displays and edit dialog 
	 */
	private void showEditDialog() {
		// TODO Auto-generated method stub
	}
	
}
