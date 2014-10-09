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
package org.wcs.smart.er.ui.samplingunit.load.wizard;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.GeometryType;

/**
 * Import sampling unit buffer size wizard page.
 *  
 * @author Emily
 *
 */
public class BufferPage extends WizardPage {

	private Composite main;
	private Text txtArea;
	private ComboViewer cmbViewer;
	
	private ControlDecoration cd;
	
	public BufferPage(){
		super("BUFFER_PAGE"); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		main = new Composite(outer, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		setControl(outer);
		setTitle(Messages.BufferPage_Title);
		setMessage(Messages.BufferPage_Message);
	}
	
	/**
	 * Sets the sampling unit time.
	 * @param type
	 */
	public void setType(GeometryType type){
		String initValue = null;
		ISelection initSelection = null;
		if (txtArea != null){
			initValue = txtArea.getText();
		}
		if (cmbViewer != null){
			initSelection = cmbViewer.getSelection();
		}
		
		for (Control c : main.getChildren()){
			c.dispose();
		}
		txtArea = null;
		cmbViewer = null;		
		
		Composite t = new Composite(main, SWT.NONE);
		t.setLayout(new GridLayout(2, false));
		
		if (type == GeometryType.TRANSECT){
			Label l = new Label(t, SWT.NONE);
			l.setText(Messages.BufferPage_TtLabel);
			
			cmbViewer = new ComboViewer(t, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewer.setLabelProvider(new LabelProvider(){
				public String getText(Object element){
					if (element instanceof GeometryType){
						return ((GeometryType) element).getGuiName();
					}
					return super.getText(element);
				}
				
			});
			String[] input = new String[]{Messages.BufferPage_OpenTransect, Messages.BufferPage_StripTransect};
			cmbViewer.setInput(input);
			
			final Label ll = new Label(t, SWT.NONE);
			ll.setText(Messages.BufferPage_BufferLabel);
			
			txtArea = new Text(t, SWT.BORDER);
			txtArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			
			cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					txtArea.setEnabled(cmbViewer.getCombo().getSelectionIndex() == 1);
				}
			});
			if (initSelection != null){
				cmbViewer.setSelection(initSelection);
			}else{
				cmbViewer.setSelection(new StructuredSelection(input[0]));
			}
			
			
		}else if (type == GeometryType.PLOT){
			Label l = new Label(t, SWT.NONE);
			l.setText(Messages.BufferPage_PlotLabel);
			
			txtArea = new Text(t, SWT.BORDER);
			txtArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		}
		
		cd = new ControlDecoration(txtArea, SWT.LEFT | SWT.TOP);
		cd.setDescriptionText(Messages.BufferPage_InvalidNumber);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.hide();
		
		txtArea.addListener(SWT.Modify, new Listener(){
			@Override
			public void handleEvent(Event event) {
				try{
					if (txtArea.getText().trim().length() > 0){
						Double d = Double.parseDouble(txtArea.getText());
						if (d < 0){
							cd.show();
						}else{
							cd.hide();
						}
					}else{
						cd.hide();
					}
				}catch (Exception ex){
					cd.show();
				}
				getWizard().getContainer().updateButtons();
		}});
		if (initValue != null){
			txtArea.setText(initValue);
		}
		
		
		main.layout(true);
		main.getParent().layout(true);
	}
	
	private boolean canMoveNext(){
		return !cd.isVisible();
	}
	
	@Override
	public IWizardPage getNextPage(){
		if (canMoveNext() ){
			return super.getNextPage();
		}else{
			return null;
		}
	}
	
	/**
	 * 
	 * @return the sampling unit type
	 */
	public GeometryType getType(){
		if (cmbViewer == null){
			return GeometryType.PLOT;
		}else{
			return GeometryType.TRANSECT;
		}
	}
	
	/**
	 * The plot error or null if not applicable
	 * 
	 * @return
	 */
	public Double getArea(){
		if (txtArea.getEnabled()){
			if (txtArea.getText().trim().length() == 0){
				return null;
			}
			return Double.parseDouble(txtArea.getText());
		}
		return null;
	}

}