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
package org.wcs.smart.ui.ca.properties;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.FilterComposite;

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
	protected CheckboxTableViewer checkboxTableViewer; // list of existing
														// attribute
	private Button btnAddNew; // add new radio
	private Button btnAddExsiting; // add existing radio
	private Label lblSelectAttribute; // select attribute label
	
	private FilterComposite txtFilter;

	/* Data Model Items */
	private Language lang; // current working language
	private Category category; // category attribute being added to
	private DataModel dm; // data model being updated

	/* items added */
	List<CategoryAttribute> addedElements;
	private Session session;
	
	private Job loadAttributesJob = new Job("Loading Attributes"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
			attributeList.addAll(dm.getAttributes());
			Collections.sort(attributeList, new Comparator<Attribute>() {
				@Override
				public int compare(Attribute o1, Attribute o2) {
					String name1 = findName(o1, lang);
					String name2 = findName(o2, lang);
					return Collator.getInstance().compare(name1,  name2);
				}
			});
			final Object[] input = attributeList.toArray();
			Display.getDefault().asyncExec(new Runnable(){

				@Override
				public void run() {
					if (!checkboxTableViewer.getTable().isDisposed()){
						checkboxTableViewer.setInput(input);
					}
					
				}});
			return Status.OK_STATUS;
		}
		
	};
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
	public AddAttributeDialog1(Shell parentShell, Category cat,
			DataModel dm, Language lang, Session session) {
		super(parentShell);
		this.category = cat;
		this.dm = dm;
		this.lang = lang;
		this.session = session;
	}

	/*
	 * find the attribute name; first looking for the name
	 * that matches the language then the default  if the
	 * language provided is the default it calls getName instead of
	 * findName as this is much faster 
	 */
	private String findName(Attribute a, Language lang){
		String name = null;
		if(lang.equals(SmartDB.getCurrentLanguage())){
			name = a.getName();
			if (name != null){
				return name;
			}
		}
		name = a.findNameNull(lang);
		if (name != null){
			return name;
		}
		name = a.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
		if (name != null){
			return name;
		}
		return ""; //$NON-NLS-1$
	}
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.AddAttributeDialog1_DialogTitle);
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
				SWT.V_SCROLL | SWT.H_SCROLL | SWT.NONE);
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		// inner composite
		Composite composite = new Composite(scrolled, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));

		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setText(Messages.AddAttributeDialog1_DialogQuestion_Label);

		btnAddNew = new Button(composite, SWT.RADIO);
		btnAddNew.setText(Messages.AddAttributeDialog1_OpCreateNew);
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
		btnAddExsiting.setText(Messages.AddAttributeDialog1_OpAddExisting);
		btnAddExsiting.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableAddAttribute(true);
				getButton(IDialogConstants.FINISH_ID).setEnabled(true);
				getButton(IDialogConstants.NEXT_ID).setEnabled(false);
			}
		});

		Composite compAddExisting = new Composite(composite, SWT.NONE);
		compAddExisting.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 1, 1));
		compAddExisting.setLayout(new GridLayout(2, false));
		((GridLayout) compAddExisting.getLayout()).marginLeft = 20;

		lblSelectAttribute = new Label(compAddExisting, SWT.NONE);
		lblSelectAttribute.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
				false, false, 2, 1));
		lblSelectAttribute.setBounds(0, 0, 58, 13);
		lblSelectAttribute.setText(Messages.AddAttributeDialog1_SelectAttribute_Label);

		//search filter field
		final AttributeNameFilter viewerFilter = new AttributeNameFilter();
		txtFilter = new FilterComposite(compAddExisting, SWT.NONE);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtFilter.addChangeListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				viewerFilter.setFilterString(txtFilter.getPatternFilter());
				checkboxTableViewer.refresh();
			}
		});
	
		//spacer label
		new Label(compAddExisting, SWT.NONE);
		
		//table viewer
		checkboxTableViewer = CheckboxTableViewer.newCheckList(compAddExisting,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		Table tblAttributes = checkboxTableViewer.getTable();
		tblAttributes.setHeaderVisible(false);
		checkboxTableViewer.addFilter(viewerFilter);
		
		checkboxTableViewer.getTable().addKeyListener(new KeyAdapter() {
			//spacebar check
			@Override
			public void keyPressed(KeyEvent e) {
				if (checkboxTableViewer.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)checkboxTableViewer.getSelection());
					selection.getFirstElement();
					boolean value = checkboxTableViewer.getChecked(selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						checkboxTableViewer.setChecked(tp, !value);
					}
					e.doit = false;
							
				}
				
			}
		});
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd.heightHint = 300;
		gd.widthHint = 300;
		tblAttributes.setLayoutData(gd);
		checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		checkboxTableViewer.setLabelProvider(new ColumnLabelProvider(){
			@Override
			public String getText(Object element) {
				if (element instanceof Attribute) {
					Attribute att = (Attribute) element;
					return findName(att, lang) + " [" + att.getKeyId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return element == null ? "" : element.toString();//$NON-NLS-1$
			}
			
			@Override
			public Image getImage(Object element) {
				if (element instanceof Attribute){
					return DataModel.getAttributeImage(((Attribute) element).getType());
				}
				return null;
			}
		});
		checkboxTableViewer.setInput(new String[]{Messages.AddAttributeDialog1_LoadingAttributesText});
		
		/* load attributes */
		loadAttributesJob.setSystem(true);
		loadAttributesJob.schedule();
		
		if (dm.getAttributes().size() == 0) {
			btnAddExsiting.setSelection(false);
			btnAddNew.setSelection(true);
			enableAddAttribute(false);
		}

		final AttributeInfoPanel attributeInfo = new AttributeInfoPanel(
				compAddExisting, SWT.NONE, false, false, session);
		attributeInfo.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		checkboxTableViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						Object x = (((StructuredSelection) checkboxTableViewer.getSelection()).getFirstElement());
						if (x!= null && x instanceof Attribute){
							Attribute sel = (Attribute)x ;
							attributeInfo.setVisible(true);
							attributeInfo.setAttribute(sel, null, lang);	//don't care about siblings; only viewing
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
		if (category != null){
			setMessage(Messages.AddAttributeDialog1_DialogMessage + category.findName(lang));
		}else{
			setMessage(Messages.AddAttributeDialog1_NewAttributeTitle);
		}
		setTitle(Messages.AddAttributeDialog1_DialogTitle);
		return myparent;
	}

	/**
	 *  enables/disables the add existing components
	 */
	private void enableAddAttribute(boolean enable) {
		lblSelectAttribute.setEnabled(enable);
		checkboxTableViewer.getTable().setEnabled(enable);
		txtFilter.setEnabled(enable);
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
	private List<CategoryAttribute> addAttributes(Category cat, DataModel dm) {
		Object[] checked = checkboxTableViewer.getCheckedElements();
		addedElements = new ArrayList<CategoryAttribute>();
		for (int i = 0; i < checked.length; i++) {
			Object x = checked[i];
			if (x instanceof Attribute){
				try{
					addedElements.add(dm.addExistingAttribute((Attribute) checked[i], cat));
				}catch (Exception ex){
					SmartPlugIn.displayLog(ex.getMessage(), ex);
				}
			}
		}
		return addedElements;
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
	
	/**
	 * 
	 * @return the list of attributes added on the first page if any
	 */
	public List<CategoryAttribute> getAddedAttributes(){
		return this.addedElements;
	}
	
	
	private class AttributeNameFilter extends ViewerFilter{
		private String filter;
		
		public void setFilterString(String filter){
			this.filter = filter;
		}
		
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			
			if (filter == null || filter.length() == 0) {
				return true;
			}
			String search = ".*" + Pattern.quote(filter.toLowerCase()) + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
			Attribute a = (Attribute) element;
			if (a.getName().toLowerCase().matches(search)){
				return true;
			}
			return false;
		}
		
	}
}
