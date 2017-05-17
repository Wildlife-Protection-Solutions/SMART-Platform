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
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
/**
 * Dialog for importing areas from shapefiles.  Asks
 * users which shapefile attributes to use for
 * various area attributes.
 * 
 * @author egouge
 *
 */
public class AreaIdDialog extends TitleAreaDialog {
	
	private SimpleFeatureType schema;
	private List<ComboViewer> langViewers;
	private HashMap<Language, AttributeDescriptor>  results ;
	
	private LabelProvider attributeLabelProvider = new LabelProvider(){
		public String getText(Object element) {
			if (element instanceof AttributeDescriptor){
				return ((AttributeDescriptor)element).getLocalName();
			}
			return super.getText(element);
		}
	};
	
	private Listener validateListener =  new Listener() {
		@Override
		public void handleEvent(Event event) {
			validate();
		}};
	
	
	
	public AreaIdDialog(Shell parentShell, SimpleFeatureType schema) {
		super(parentShell);
		
		this.schema = schema;
	}

	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite outer  = new Composite(composite, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//Create an outer composite for spacing
		ScrolledComposite scrolled = new ScrolledComposite(outer, SWT.V_SCROLL | SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		scrolled.setLayoutData(gd);
		
		// always show the focus control
		scrolled.setShowFocusedControl(true);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		
		Composite main = new Composite(scrolled, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		langViewers = new ArrayList<ComboViewer>();

		ArrayList<Object> atts = new ArrayList<Object>();
		for (AttributeDescriptor att : schema.getAttributeDescriptors()){
			atts.add(att);
		}
		Object[] attributes = atts.toArray(new Object[atts.size()]);
				
		Language l = SmartDB.getCurrentConservationArea().getDefaultLanguage();
		if (l == null) l = SmartDB.getCurrentLanguage();
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(l.getDisplayName() + "*:"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		createCombo(l, main, attributes);
		
		atts.add(""); //$NON-NLS-1$
		attributes = atts.toArray(new Object[atts.size()]);
		for (Language lang : SmartDB.getCurrentConservationArea().getLanguages()){
			if (lang.equals(l)) continue;
			lbl = new Label(main, SWT.NONE);
			lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
			lbl.setText(lang.getDisplayName() + ":"); //$NON-NLS-1$
			createCombo(lang, main, attributes);
		}
		lbl = new Label(outer, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lbl = new Label(outer, SWT.NONE);
		lbl.setText("* " + Messages.AreaIdDialog_RequiredField); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1));
		setMessage(Messages.AreaIdDialog_DialogMessage);
		setTitle(Messages.AreaIdDialog_DialogTitle);
		getShell().setText(Messages.AreaIdDialog_DialogTitle);
		
		
		scrolled.setContent(main);
		Point pnt = scrolled.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(pnt);
		((GridData)scrolled.getLayoutData()).heightHint = Math.min(250, pnt.y);
		
		return composite; 
	}
	
	@Override
	protected Point getInitialSize(){
		Point p = super.getInitialSize();
		p.x = Math.min(p.x, 550);
		return p;
	}
	
	private void createCombo(Language l, Composite parent, Object[] input){
		ComboViewer cmbAttributes = new ComboViewer(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbAttributes.setContentProvider(ArrayContentProvider.getInstance());
		cmbAttributes.setLabelProvider(attributeLabelProvider);
		cmbAttributes.setInput(input);
		cmbAttributes.getCombo().addListener(SWT.Modify, validateListener);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.widthHint = 100;
		cmbAttributes.getCombo().setLayoutData(gd);
		cmbAttributes.getControl().setData(l);
		langViewers.add(cmbAttributes);
	}
	private void validate(){
		boolean ok = true;
		for (ComboViewer cmb : langViewers){
			Language l = (Language) cmb.getCombo().getData();
			if (l.isDefault() && cmb.getSelection().isEmpty()){
				ok = false;
				break;
				
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(ok);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btn = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId) {
		
		results = new HashMap<Language, AttributeDescriptor> ();
		for (ComboViewer cmb : langViewers){
			Language l = (Language) cmb.getCombo().getData();
			if (!cmb.getSelection().isEmpty() ){
				Object d = (Object) ((IStructuredSelection)cmb.getSelection()).getFirstElement();
				if (d instanceof AttributeDescriptor){
					results.put(l, (AttributeDescriptor)d);
				}
			}
		}
		super.buttonPressed(buttonId);
	}
	
	
	public HashMap<Language, AttributeDescriptor> getSelectedFields(){
		return results;
	}
	
	/** dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
}
