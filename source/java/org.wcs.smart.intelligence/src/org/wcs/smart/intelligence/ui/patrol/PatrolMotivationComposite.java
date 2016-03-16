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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.common.control.MultipleSelectComposite.IListChanged;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Composite for collecting the patrol motivation information based on intelligence
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolMotivationComposite extends Composite {

	private Button btnMotivated;
	private Label selectLabel;
	private IntelligenceFilteredSelectComposite selectComposite;
	
	private List<IPartolMotivationChangeListener> inputListeners = new ArrayList<IPartolMotivationChangeListener>();
	private String errorMessage;

	private List<Intelligence> selectedIntelligences = new ArrayList<Intelligence>();
	
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
        btnMotivated.setText(Messages.PatrolMotivationComposite_Motivated_Checkbox_Label);
        btnMotivated.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		applyCurrentState();
        		handleInputChanged();
        	}
        });
        
		selectLabel = new Label(this, SWT.NONE);
		selectLabel.setText(Messages.PatrolMotivationComposite_Selector_Label);

		selectComposite = new IntelligenceFilteredSelectComposite(this, SWT.NONE);
		selectComposite.addSelectionChangedListener(new IListChanged<Intelligence>() {
			@Override
			public void listChanged(List<Intelligence> items) {
				handleInputChanged();
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

	public boolean updateModel(Patrol p) {
		selectedIntelligences.clear();
    	for (Iterator<?> iterator = selectComposite.getSelectedItems().iterator(); iterator.hasNext();) {
    		Intelligence i = (Intelligence) iterator.next();
    		selectedIntelligences.add(i);
		}
		return true;
	}

	public void initFromModel(Patrol p, Session session, Collection<Intelligence> selectedItems) {
		if (selectedItems != null) {
			selectedIntelligences.addAll(selectedItems);
			selectComposite.setSelectedIntelligences(selectedIntelligences);
		}
		btnMotivated.setSelection(!selectedIntelligences.isEmpty());
		applyCurrentState();
		handleInputChanged();
	}

	public List<Intelligence> getCurrentIntelligences() {
		return btnMotivated.getSelection() ? Collections.unmodifiableList(selectedIntelligences) : Collections.<Intelligence>emptyList();
	}

    private void handleInputChanged() {
    	validate();
    	fireInputChangeListeners();
    }
    
    private void validate() {
    	if (btnMotivated.getSelection()) {
     		if (selectComposite.getSelectedItems().isEmpty()) {
    			setErrorMessage(Messages.PatrolMotivationComposite_Selector_Error);
    			return;
    		}
    	}
		setErrorMessage(null);
    }
    
	public String getErrorMessage() {
		return errorMessage;
	}

	protected void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void addInputChangeListener(IPartolMotivationChangeListener listener) {
		inputListeners.add(listener);
	}

	public void removeInputChangeListener(IPartolMotivationChangeListener listener) {
		inputListeners.remove(listener);
	}
	
	protected void fireInputChangeListeners() {
		for (IPartolMotivationChangeListener listener : inputListeners) {
			listener.inputChanged();
		}
	}

	/**
	 * Change listener for {@link PatrolMotivationComposite}.
	 * 
	 * @author elitvin
	 * @since 1.0.0
	 */
	public interface IPartolMotivationChangeListener {

		public void inputChanged();
		
	}	
}
