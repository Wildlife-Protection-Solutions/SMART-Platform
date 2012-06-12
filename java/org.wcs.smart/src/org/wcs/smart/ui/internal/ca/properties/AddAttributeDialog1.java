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
package org.wcs.smart.ui.internal.ca.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;

/**
 * Dialog to prompt user if they want to create a new attribute or add one of
 * the existing attributes.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AddAttributeDialog1 extends TitleAreaDialog {
	/**
	 * Value returned by open() existing attributes are added and FINISH button
	 * selected
	 */
	public static final int FINISH = 2;
	/**
	 * Value returned by open() user wants to create new attribute and NEXT
	 * button selected.
	 */
	public static final int NEXT = 4;

	/* GUI Components */
	private CheckboxTableViewer checkboxTableViewer; // list of existing
														// attribute
	private Button btnAddNew; // add new radio
	private Button btnAddExsiting; // add existing radio
	private Label lblSelectAttribute; // select attribute label

	/* Data Model Items */
	private Language defaultLang; // current working language
	private Category category; // category attribute being added to
	private DataModel dm; // data model being updated

	/**
	 * Creates a new attribute dialog that prompts the user if they want to add
	 * existing or create new attributes
	 * 
	 * @param parentShell
	 * @param cat
	 *            category attribute is to be added to
	 * @param dm
	 *            the data model being modified
	 * @param defaultLang
	 *            the current language being modified
	 */
	protected AddAttributeDialog1(Shell parentShell, Category cat,
			DataModel dm, Language defaultLang) {
		super(parentShell);
		this.category = cat;
		this.dm = dm;
		this.defaultLang = defaultLang;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Add Attribute");
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent) {

		Composite myparent = (Composite) super.createDialogArea(parent);
		myparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Create an outer composite for spacing
		ScrolledComposite scrolled = new ScrolledComposite(myparent,
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		// inner composite
		Composite composite = new Composite(scrolled, SWT.BORDER);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));

		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setText("Would you like to:");

		btnAddNew = new Button(composite, SWT.RADIO);
		btnAddNew.setText("Create a new attribute");
		btnAddNew.setSelection(false);
		btnAddNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableAddAttribute(false);
				getButton(IDialogConstants.FINISH_ID).setEnabled(false);
				getButton(IDialogConstants.NEXT_ID).setEnabled(true);

			}
		});

		btnAddExsiting = new Button(composite, SWT.RADIO);
		btnAddExsiting.setSelection(true);
		btnAddExsiting.setText("Add existing attribute(s)");
		btnAddExsiting.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableAddAttribute(true);
				getButton(IDialogConstants.FINISH_ID).setEnabled(true);
				getButton(IDialogConstants.NEXT_ID).setEnabled(false);
			}
		});

		Composite compAddExisting = new Composite(composite, SWT.NONE);
		compAddExisting.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));
		compAddExisting.setLayout(new GridLayout(2, false));
		((GridLayout) compAddExisting.getLayout()).marginLeft = 20;

		lblSelectAttribute = new Label(compAddExisting, SWT.NONE);
		lblSelectAttribute.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 2, 1));
		lblSelectAttribute.setBounds(0, 0, 58, 13);
		lblSelectAttribute.setText("Select the attribute(s) to add:");

		checkboxTableViewer = CheckboxTableViewer.newCheckList(compAddExisting,
				SWT.BORDER | SWT.FULL_SELECTION);
		Table tblAttributes = checkboxTableViewer.getTable();
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 300;
		tblAttributes.setLayoutData(gd);
		checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());

		checkboxTableViewer.setLabelProvider(new LabelProvider() {
			/**
			 * The <code>LabelProvider</code> implementation of this
			 * <code>ILabelProvider</code> method returns the element's
			 * <code>toString</code> string. Subclasses may override.
			 */
			public String getText(Object element) {
				if (element instanceof Attribute) {
					Attribute att = (Attribute) element;
					return att.findName(defaultLang) + " [" + att.getKeyId()
							+ "]";
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
		});
		ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
		attributeList.addAll(dm.getAttributes());
		Collections.sort(attributeList, new Comparator<Attribute>() {
			@Override
			public int compare(Attribute o1, Attribute o2) {
				return o1.findName(defaultLang).compareTo(o2.findName(defaultLang));
			}
		});
		checkboxTableViewer.setInput(attributeList.toArray());

		if (dm.getAttributes().size() == 0) {
			btnAddExsiting.setSelection(false);
			btnAddNew.setSelection(true);
			enableAddAttribute(false);
		}

		final AttributeInfoPanel attributeInfo = new AttributeInfoPanel(
				compAddExisting, SWT.NONE, false, false, defaultLang, null) {
			@Override
			public Collection<Attribute> getSiblings() {
				return null;
			}

		};

		checkboxTableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						Attribute sel = (Attribute) (((StructuredSelection) checkboxTableViewer
								.getSelection()).getFirstElement());
						if (sel != null) {
							attributeInfo.setVisible(true);
							attributeInfo.setAttribute(sel);
						} else {
							attributeInfo.setVisible(false);
						}

					}
				});
		attributeInfo.setVisible(false);

		//update scrolled size
		scrolled.setContent(composite);
		scrolled.setMinSize(scrolled.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		//set title message for dialog
		setMessage("Add a new attribute to '" + category.findName(defaultLang)
				+ "'");
		return myparent;
	}

	/**
	 *  enables/disables the add existing components
	 */
	private void enableAddAttribute(boolean enable) {
		lblSelectAttribute.setEnabled(enable);
		checkboxTableViewer.getTable().setEnabled(enable);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.BACK_ID,
				IDialogConstants.BACK_LABEL, false);
		createButton(parent, IDialogConstants.NEXT_ID,
				IDialogConstants.NEXT_LABEL, true);
		createButton(parent, IDialogConstants.FINISH_ID,
				IDialogConstants.FINISH_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);

		getButton(IDialogConstants.BACK_ID).setEnabled(false);
		getButton(IDialogConstants.FINISH_ID).setEnabled(
				!btnAddNew.getSelection());
		getButton(IDialogConstants.NEXT_ID)
				.setEnabled(btnAddNew.getSelection());
	}

	/*
	 * adds all selected attributes to the given category 
	 * 
	 */
	private void addAttributes(Category cat, DataModel dm) {
		Object[] checked = checkboxTableViewer.getCheckedElements();
		for (int i = 0; i < checked.length; i++) {
			dm.addExistingAttribute((Attribute) checked[i], cat);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.NEXT_ID == buttonId) {
			setReturnCode(NEXT);
		} else if (IDialogConstants.FINISH_ID == buttonId) {
			addAttributes(this.category, this.dm);
			setReturnCode(FINISH);
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}
		close();
	}
}
