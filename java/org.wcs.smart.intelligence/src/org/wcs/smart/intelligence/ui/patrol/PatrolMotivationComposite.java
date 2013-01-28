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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.common.control.MultipleSelectComposite;
import org.wcs.smart.intelligence.model.Intelligence;

/**
 * Composite for collecting the patrol motivation information based on intelligence
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolMotivationComposite extends Composite {

	private Button btnMotivated;
	private Label selectLabel;
	private MultipleSelectComposite<Intelligence> selectComposite;
	/**
	 * @param parent
	 * @param style
	 */
	public PatrolMotivationComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}

	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        btnMotivated = new Button(this, SWT.CHECK);
        btnMotivated.setText("Patrol is motivated by intelligence");
        btnMotivated.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		applyCurrentState();
        	}
        });
        
		selectLabel = new Label(this, SWT.NONE);
		selectLabel.setText("Select intelligence");
		
		selectComposite = new MultipleSelectComposite<Intelligence>(this, SWT.NONE);
		selectComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		selectComposite.setLabelProvider(new IntelligenceLabelProvider());
		selectComposite.setItemComparator(new IntelligenceComparator());
		selectComposite.setLabelAllText("All Intelligences:");
		selectComposite.setLabelSelectedText("Selected Intelligences:");
		
		selectComposite.addSelectionChangedListener(new MultipleSelectComposite.IListChanged<Intelligence>() {
			@Override
			public void listChanged(List<Intelligence> items) {
				if (items.size() == 0) {
//					setPageComplete(false);
//					setErrorMessage(Messages.PatrolMemberWizardPage_Error_NoEmployees);
				} else {
//					setPageComplete(true);
//					setErrorMessage(null);
				}
			}
		});
        
		applyCurrentState();
	}

    /**
     * updates gui state according to "motivated" checkbox state
     */
    private void applyCurrentState() {
		boolean isMotivated = btnMotivated.getSelection();
		selectLabel.setVisible(isMotivated);
		selectComposite.setVisible(isMotivated);
    }	
	
	public MultipleSelectComposite<Intelligence> getSelectComposite() {
		return selectComposite;
	}
}
