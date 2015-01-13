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

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.model.IntelligenceSource;
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
	private static final int NO_SOURCE_ERR_IMAGE_SIZE = 32;
	
    private ComboViewer sourceType;
    private List<IntelligenceSource> sourceTypeList;
    private List<Informant> informantList;
    
    private PatrolFilteredComboViewer patrolId;

    private ComboViewer informantViewer;

    private Composite detailsComposite;
    private Composite emptyComposite;
    private Composite patrolComposite;
    private Composite informantComposite;
    
    private ControlDecoration patrolIdDecoration;

	/**
	 * @param parent
	 * @param style
	 */
	public IntelligenceSourceComposite(Composite parent, int style) {
		super(parent, style);
		Session s = HibernateManager.openSession();
		try {
			sourceTypeList = IntelligenceHibernateManager.getActiveSourceTypes(SmartDB.getCurrentConservationArea(), s);
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.IntelligenceSourceComposite_SourceLoad_Error, e);
			sourceTypeList = new ArrayList<IntelligenceSource>();
		}

		try {
			informantList = IntelligenceHibernateManager.getInformants(SmartDB.getCurrentConservationArea(), s, true);
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.IntelligenceSourceComposite_InformantLoad_Error, e);
			informantList = new ArrayList<Informant>();
		} finally {
			s.close();
		}
		
		setMessage(Messages.IntelligenceSource_Message);
		if (!sourceTypeList.isEmpty()) {
			createControls();
			validate();
		} else {
			createNoSourceControls();
		}
	}

	private void createNoSourceControls() {
        GridLayout layout = new GridLayout(1, false);
        layout.marginLeft = NO_SOURCE_ERR_IMAGE_SIZE;
		this.setLayout(layout);
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
        Label sourceLabel = new Label(this, SWT.NONE);
        sourceLabel.setText(Messages.IntelligenceSourceComposite_NoSources_Error);

        ControlDecoration labelDecoration = new ControlDecoration(sourceLabel, SWT.LEFT);
        labelDecoration.setImage(new Image(Display.getDefault(), Display.getDefault().getSystemImage(SWT.ICON_ERROR).getImageData().scaledTo(NO_SOURCE_ERR_IMAGE_SIZE, NO_SOURCE_ERR_IMAGE_SIZE)));
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
 
        sourceType.setInput(sourceTypeList);
        sourceType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (IntelligenceSource.isPatrolSource(getSelectedSourceType())) {
					((StackLayout)detailsComposite.getLayout()).topControl = patrolComposite;
				} else if (IntelligenceSource.isInformantSource(getSelectedSourceType())) {
					((StackLayout)detailsComposite.getLayout()).topControl = informantComposite;
				} else {
					((StackLayout)detailsComposite.getLayout()).topControl = emptyComposite;
				}
				detailsComposite.layout();
				fireInputChangeListeners();				
			}
		});
        
        detailsComposite = new Composite(this, SWT.NONE);
		StackLayout layout = new StackLayout();
		layout.marginHeight = 2;
		layout.marginWidth = 0;
		detailsComposite.setLayout(layout);
        detailsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		emptyComposite = new Composite(detailsComposite, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
		
		patrolComposite = new Composite(detailsComposite, SWT.NONE);
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		patrolComposite.setLayout(gd);
		patrolComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
		Label patrolLabel = new Label(patrolComposite, SWT.NONE);
        patrolLabel.setText(Messages.IntelligenceSource_PatrolId_Label);
        patrolLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
        
        patrolId = new PatrolFilteredComboViewer(patrolComposite);
        //below line is to fix decorator truncation issue
        ((GridLayout)patrolId.getLayout()).marginLeft = DECORATION_MARGIN;
        patrolId.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				refreshPatrolDecoration();
				fireInputChangeListeners();
			}
		});
        
        patrolIdDecoration = new ControlDecoration(patrolId.getControl(), SWT.LEFT);
        patrolIdDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        patrolIdDecoration.setShowHover(true);
        patrolIdDecoration.setDescriptionText(ERROR_PATROL_ID_REQUIRED);

        

		informantComposite = new Composite(detailsComposite, SWT.NONE);
		gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		informantComposite.setLayout(gd);
		informantComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
		Label informantLabel = new Label(informantComposite, SWT.NONE);
        informantLabel.setText(Messages.IntelligenceSourceComposite_InformantId);
        informantLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        informantViewer = new ComboViewer(informantComposite, SWT.READ_ONLY);
        informantViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        informantViewer.setContentProvider(ArrayContentProvider.getInstance());
        informantViewer.setLabelProvider(new InformantIdLabelProvider());
        informantViewer.addSelectionChangedListener(new ISelectionChangedListener() {
 			@Override
 			public void selectionChanged(SelectionChangedEvent event) {
 				fireInputChangeListeners();				
 			}
 		});
 
        informantViewer.setInput(informantList);
        
        if (!sourceTypeList.isEmpty())
        	sourceType.setSelection(new StructuredSelection(sourceTypeList.get(0)));
        
	}

	private void refreshPatrolDecoration() {
		if (patrolIdDecoration.getControl().isVisible() && getSelectedPatrol() == null) {
			patrolIdDecoration.show();
		} else {
			patrolIdDecoration.hide();
		}
	}
	
    @Override
    protected void updateModelInternal(Intelligence intelligence) {
    	IntelligenceSource source = getSelectedSourceType();
    	Patrol patrol = IntelligenceSource.isPatrolSource(source) ? getSelectedPatrol() : null;
       	intelligence.setSource(source);
		intelligence.setPatrol(patrol);
		Informant informant = IntelligenceSource.isInformantSource(source) ? getSelectedInformant() : null;
		intelligence.setInformant(informant);
    }

	@Override
	public void initFromModel(Intelligence intelligence) {
	    if (intelligence.getSource() != null) {
	    	if (sourceType != null){
	    		if (sourceTypeList.contains(intelligence.getSource())){
	    			sourceType.setSelection(new StructuredSelection(intelligence.getSource()));
	    		}else{
	    			sourceType.setSelection(null);
	    		}
	    	}
	    }
	    if (intelligence.getPatrol() != null) {
	    	if (patrolId != null) {
	    		patrolId.setSelection(intelligence.getPatrol());
	    	}
	    }
	    if (intelligence.getInformant() != null) {
	    	if (informantViewer != null) {
	    		informantViewer.setSelection(new StructuredSelection(intelligence.getInformant()));
	    	}
	    }
	}
    
    @Override
    protected void validate() {
    	IntelligenceSource source = getSelectedSourceType();
		if (source == null) {
			setErrorMessage(ERROR_SOURCE_REQUIRED);
			return;
		}
		if (IntelligenceSource.isPatrolSource(source) && getSelectedPatrol() == null) {
			setErrorMessage(ERROR_PATROL_ID_REQUIRED);
			return;
		}
		setErrorMessage(null);
    } 

    private IntelligenceSource getSelectedSourceType() {
    	if (sourceType != null) {
    		ISelection sourceSelection = sourceType.getSelection();
    		if (sourceSelection instanceof IStructuredSelection) {
    			return (IntelligenceSource)((IStructuredSelection)sourceSelection).getFirstElement();
    		}
    	}
		return null;
    }

    private Patrol getSelectedPatrol() {
    	return patrolId.getSelection();
    }

    private Informant getSelectedInformant() {
    	IStructuredSelection selection = (IStructuredSelection) informantViewer.getSelection();
    	return selection != null && !selection.isEmpty() ? (Informant)selection.getFirstElement() : null;
    }
    
    /**
     * LabelProvider used to display enum values from {@link IntelligenceSourceType}
     * 
     * @author elitvin
     */
    private class IntelligenceSourceLabelProvider extends LabelProvider {
    	@Override
    	public String getText(Object element) {
    		if (element instanceof IntelligenceSource) {
    			return ((IntelligenceSource)element).getName();
    		}
    		return super.getText(element);
    	}
    }

    /**
     * LabelProvider used to display informant IDs
     * 
     * @author elitvin
     */
    private class InformantIdLabelProvider extends LabelProvider {
    	@Override
    	public String getText(Object element) {
    		if (element instanceof Informant) {
    			return ((Informant)element).getId();
    		}
    		return super.getText(element);
    	}
    }
    
}
