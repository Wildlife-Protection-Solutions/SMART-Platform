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
package org.wcs.smart.intelligence.ui.panel;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceSourceType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.PatrolFilteredComboViewer;

/**
 * Composite for collecting the intelligence source information
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceSourceComposite extends IntelligenceComposite {

	private static final String ERROR_SOURCE_REQUIRED = Messages.IntelligenceSource_Error_SourceRequired;
	private static final String ERROR_PATROL_ID_REQUIRED = Messages.IntelligenceSource_Error_PatrolIdRequired;

	private static final int DECORATION_MARGIN = 2;
	
    private ComboViewer sourceType;
    
    private Label patrolLabel;
    private PatrolFilteredComboViewer patrolId;

    private ControlDecoration patrolIdDecoration;

	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceSourceComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.IntelligenceSource_Message);
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        
        Label sourceLabel = new Label(this, SWT.NONE);
        sourceLabel.setText(Messages.IntelligenceSource_IntelligenceSource_Label);
        sourceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        sourceType = new ComboViewer(this, SWT.READ_ONLY);
        sourceType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sourceType.setContentProvider(ArrayContentProvider.getInstance());
        sourceType.setLabelProvider(new IntelligenceSourceLabelProvider());
 
        sourceType.setInput(IntelligenceSourceType.values());
        sourceType.setSelection(new StructuredSelection(IntelligenceSourceType.PATROL));
        sourceType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean isPatrolSelected = IntelligenceSourceType.PATROL.equals(getSelectedSourceType());
				patrolLabel.setVisible(isPatrolSelected);
				patrolId.setVisible(isPatrolSelected);
				refreshPatrolDecoration();
				fireDataValidStateListeners();
				fireInputChangeListeners();				
			}
		});
        
        patrolLabel = new Label(this, SWT.NONE);
        patrolLabel.setText(Messages.IntelligenceSource_PatrolId_Label);
        patrolLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        patrolId = new PatrolFilteredComboViewer(this);
        //below line is to fix decorator truncation issue
        ((GridLayout)patrolId.getLayout()).marginLeft = DECORATION_MARGIN;
        patrolId.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				refreshPatrolDecoration();
				fireDataValidStateListeners();
				fireInputChangeListeners();
			}
		});
        
        patrolIdDecoration = new ControlDecoration(patrolId.getControl(), SWT.LEFT);
        patrolIdDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        patrolIdDecoration.setShowHover(true);
        patrolIdDecoration.setDescriptionText(ERROR_PATROL_ID_REQUIRED);

        
	}

	private void refreshPatrolDecoration() {
		if (patrolIdDecoration.getControl().isVisible() && getSelectedPatrol() == null) {
			patrolIdDecoration.show();
		} else {
			patrolIdDecoration.hide();
		}
	}
	
    @Override
    public boolean updateModel(Intelligence intelligence) {
    	IntelligenceSourceType source = getSelectedSourceType();
    	if (source == null) {
    		IntelligencePlugIn.displayLog(ERROR_SOURCE_REQUIRED, null);
    		return false;
    	}
    	Patrol patrol = null;
    	if (IntelligenceSourceType.PATROL.equals(source)) {
    		patrol = getSelectedPatrol();
    		if (patrol == null) {
    			IntelligencePlugIn.displayLog(ERROR_PATROL_ID_REQUIRED, null);
    			return false;
    		}
    	}
    	intelligence.setSource(source);
		intelligence.setPatrol(patrol);
    	return true;
    }

	@Override
	public void initFromModel(Intelligence intelligence) {
	    if (intelligence.getSource() != null) {
	    	sourceType.setSelection(new StructuredSelection(intelligence.getSource()));
	    }
	    if (intelligence.getPatrol() != null) {
	    	patrolId.setSelection(intelligence.getPatrol());
	    }
	}
    
    @Override
    public boolean isDataValid() {
    	IntelligenceSourceType source = getSelectedSourceType();
		if (source == null) {
			return false;
		}
		if (IntelligenceSourceType.PATROL.equals(source)) {
			return getSelectedPatrol() != null;
		}
		return true;
    }
    
    private IntelligenceSourceType getSelectedSourceType() {
		ISelection sourceSelection = sourceType.getSelection();
		if (sourceSelection instanceof IStructuredSelection) {
			return (IntelligenceSourceType)((IStructuredSelection)sourceSelection).getFirstElement();
		}
		return null;
    }

    private Patrol getSelectedPatrol() {
    	return patrolId.getSelection();
    }
	
    /**
     * LabelProvider used to display enum values from {@link IntelligenceSourceType}
     * 
     * @author elitvin
     *
     */
    private class IntelligenceSourceLabelProvider extends LabelProvider {
    	@Override
    	public String getText(Object element) {
    		if (element instanceof IntelligenceSourceType) {
    			return ((IntelligenceSourceType)element).getName();
    		}
    		return super.getText(element);
    	}
    }

}
