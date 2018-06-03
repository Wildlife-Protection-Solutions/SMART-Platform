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
package org.wcs.smart.i2.diagram.style;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Dialog for creating new relatinship diagram style.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public class CreateNewStyleOpDialog extends TitleAreaDialog {
	
	private enum CreateStyleOption {
		BLANK,
		STYLE
	}

	private CreateStyleOption option = CreateStyleOption.BLANK;
	private Button opBlank, opStyle;
	private ComboViewer cbStyle;
	private RelationshipDiagramStyle initStyle;
	private String name = null;
	private RelationshipDiagramStyle styleTemplate = null;
	private Text txtName;
	private List<RelationshipDiagramStyle> styleList;
	
	protected CreateNewStyleOpDialog(Shell parentShell, List<RelationshipDiagramStyle> styleList) {
		super(parentShell);
		this.styleList = styleList;
	}

	protected void okPressed() {
		name = txtName.getText();
		IStructuredSelection selection = (IStructuredSelection)cbStyle.getSelection();
		styleTemplate = !selection.isEmpty() ? (RelationshipDiagramStyle) selection.getFirstElement() : null;
		String error = validate();
		setErrorMessage(error);
		if (error == null) {
			super.okPressed();
		}
	}
	
	private String validate() {
		if (CreateStyleOption.STYLE.equals(option) && cbStyle.getSelection().isEmpty()) {
			return "Relationship diagram style that will be used as a template is not selected.";
		}
		return null;
	}

	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(3, false));
		panel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 20;
		Label lblName = new Label(panel, SWT.NONE);
		lblName.setText("Name:");
		
		txtName = new Text(panel, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtName.setText("New Style");
		
		Label lblOp = new Label(panel, SWT.NONE);
		lblOp.setText("Template:");
		
		opBlank = new Button(panel, SWT.RADIO);
		opBlank.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
		opBlank.setSelection(true);
		opBlank.setText("Blank (creates a style with parameters set to their default value)");
		opBlank.setToolTipText("Creates a relationship diagram style with parameters set to their default value.");
		opBlank.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged(CreateStyleOption.BLANK);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		new Label(panel, SWT.NONE);

		opStyle = new Button(panel, SWT.RADIO);
		opStyle.setText("Relationship diagram style");
		opStyle.setToolTipText("Creates a relationship diagram style using a selected relationship diagram style as a template.");
		opStyle.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				optionChanged(CreateStyleOption.STYLE);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});

		cbStyle = new ComboViewer(panel, SWT.READ_ONLY);
		cbStyle.getControl().setEnabled(false);
		cbStyle.getControl().setToolTipText("Creates a relationship diagram style using a selected relationship diagram style as a template.");
		cbStyle.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cbStyle.setContentProvider(ArrayContentProvider.getInstance());
		cbStyle.setLabelProvider(new NamedItemLabelProvider()); //TODO: ZZZZZZZZ need custom label provider that will mark default style
 		cbStyle.setInput(styleList);
		
		getShell().setText("New Relationship Diagram Style");
		setTitle("New Relationship Diagram Style");
		setMessage("Select option for relationship diagram style template");
		
		return parent;
	}

	protected void optionChanged(CreateStyleOption op) {
		option = op;
		cbStyle.getControl().setEnabled(CreateStyleOption.STYLE.equals(option));
	}
	
	public RelationshipDiagramStyle getStyle() throws Exception {
		switch (option) {
		case BLANK:
			return RelationshipDiagramStyleFactory.createUsingDefaults(name);
		case STYLE:
		{
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor progress = SubMonitor.convert(monitor, "Loading relationship diagram style", 1);
					try(Session s = HibernateManager.openSession()){
						s.beginTransaction();
						try {
							RelationshipDiagramStyle fullStyle = (RelationshipDiagramStyle) s.get(RelationshipDiagramStyle.class, styleTemplate.getUuid());
							initStyle = RelationshipDiagramStyleFactory.createStyleClone(fullStyle, name, progress.split(1));
						} catch (Exception ex) {
							SmartPlugIn.displayLog("Error occurs while loading relationship diagram style.", ex);
						} finally {
							s.getTransaction().rollback();
						}
					}
				}
			});
			return initStyle;
		}
		}
		//this line should never be reached
		throw new IllegalStateException("Unknown template option for creaing a CyberTracker properties style: " + option); //$NON-NLS-1$
	}
	
}
