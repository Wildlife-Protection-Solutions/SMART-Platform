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
package org.wcs.smart.er.ui.samplingunit.wizard;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;

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
	
	public BufferPage(){
		super("BUFFER_PAGE");
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
		setTitle("Sampling Unit Buffer");
		setMessage("Select the buffer or leave blank if not applicable.");
	}
	
	public void setType(SamplingUnitType type){
		for (Control c : main.getChildren()){
			c.dispose();
		}
		
		Composite t = new Composite(main, SWT.NONE);
		t.setLayout(new GridLayout(2, false));
		
		if (type == SamplingUnitType.OPEN_TRANSECT){
			Label l = new Label(t, SWT.NONE);
			l.setText("Transect Type:");
			
			cmbViewer = new ComboViewer(t, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
			cmbViewer.setLabelProvider(new LabelProvider(){
				public String getText(Object element){
					if (element instanceof SamplingUnitType){
						return ((SamplingUnitType) element).getGuiName();
					}
					return super.getText(element);
				}
				
			});
			cmbViewer.setInput(new Object[]{SamplingUnitType.OPEN_TRANSECT, SamplingUnitType.STRIP_TRANSECT});
			
			final Label ll = new Label(t, SWT.NONE);
			ll.setText("Buffer Area:");
			
			txtArea = new Text(t, SWT.BORDER);
			txtArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
				
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					SamplingUnitType type = (SamplingUnitType) ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
					txtArea.setEnabled(type == SamplingUnitType.STRIP_TRANSECT);
				}
			});
			cmbViewer.setSelection(new StructuredSelection(SamplingUnitType.OPEN_TRANSECT));
			
		}else if (type == SamplingUnitType.PLOT){
			Label l = new Label(t, SWT.NONE);
			l.setText("Plot Area:");
			
			txtArea = new Text(t, SWT.BORDER);
			txtArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		}
		main.layout(true);
		main.getParent().layout(true);
	}
	
	public SamplingUnitType getType(){
		if (cmbViewer == null){
			return SamplingUnitType.PLOT;
		}else{
			return (SamplingUnitType) ((IStructuredSelection)cmbViewer.getSelection()).getFirstElement();
		}
	}
	
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