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
package org.wcs.smart.patrol.internal.ui.editor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.util.SmartUtils;

/**
 * Patrol Day editor.  This consists of 
 * a collection of patrollegdayinputcomposites, one
 * for each leg that spans the day.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolDayEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.patrol.ui.PatrolDayEditor"; //$NON-NLS-1$

	private PatrolEditor editor = null;
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	private PatrolLegDayInputComposite[] children ;
	private ScrolledForm frmSummary; 
	
	public PatrolDayEditor(PatrolEditor editor) {
		super.setPartName("");
		this.editor = editor;
		
	}
	
	@Override
	public void dispose(){		
		super.dispose();
		if (children != null){
			for (int i = 0; i < children.length; i ++){
				children[i].dispose();
			}
		}
	}
	
	/**
	 * Create contents of the editor part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		Session session = HibernateManager.openSession();	
		session.update(editor.getPatrol());
		frmSummary = toolkit.createScrolledForm(parent);
		
		frmSummary.getBody().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		String canEdit = editor.canEdit();
		if (canEdit != null){
			Composite warning = toolkit.createComposite(frmSummary.getBody());
			warning.setLayout(new GridLayout(2, false));
			Label lblImage = toolkit.createLabel(warning, null, SWT.NONE);
			Image x = editor.getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
			lblImage.setImage(x);
			Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE);
			lblWarning.setText("This patrol cannot be modified: " + canEdit + ". Please contact administrator if editing is required.");
			
		}
		
		SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
		StringBuilder text = new StringBuilder("Patrol Day: ");
		text.append(dayFormat.format(((PatrolDayEditorInput)getEditorInput()).getPatrolDay()));
		text.append(", ");
		text.append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(((PatrolDayEditorInput)getEditorInput()).getPatrolDay()));
		frmSummary.setText(text.toString());
		frmSummary.getBody().setLayout(new GridLayout(1, false));
		
		try{
			//find all patrol legs for this day
			List<PatrolLeg> legs = editor.getPatrol().getLegs();
			ArrayList<PatrolLegDay> plds = new ArrayList<PatrolLegDay>();
			for (PatrolLeg leg: legs){
				for (PatrolLegDay day : leg.getPatrolLegDays()){
					if (SmartUtils.getDatePart(day.getDate(), false).equals(  SmartUtils.getDatePart( ((PatrolDayEditorInput)getEditorInput()).getPatrolDay(), false))){
						plds.add(day);
					}
					
					//load waypoints and attach to session; for performance reasons
					//waypoints are not cascaded (otherwise saves are cascaded too)
					if (day.getWaypoints() != null){
						for (Waypoint wp : day.getWaypoints()) {
							session.update(wp);
						}
					}
				}
			}
			
			if (plds.size() == 0){
				Label lbl = toolkit.createLabel(frmSummary.getBody(), "", SWT.WRAP);
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.widthHint =lbl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				lbl.setLayoutData(gd);
				lbl.setText("Error locating data.  Please ensure that the patrol legs dates are correctly defined for the patrol.  The last day must have at least one leg that is greater than one second in length.");
				
			}else if (plds.size() == 1){
				children = new PatrolLegDayInputComposite[1];
				PatrolLegDayInputComposite comp = new PatrolLegDayInputComposite(this);
				comp.createComposite(frmSummary.getBody(), toolkit);
				comp.setData((PatrolLegDay)plds.get(0));
				children[0] = comp;
			}else{
				children = new PatrolLegDayInputComposite[plds.size()];
				Composite mainComp = toolkit.createComposite(frmSummary.getBody());
				mainComp.setLayout(new GridLayout(1, false));
				mainComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
				
				for (int i = 0; i < plds.size(); i ++){
					PatrolLegDay pld = (PatrolLegDay)plds.get(i);
					final Section sec = toolkit.createSection(mainComp, Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
					sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					sec.addExpansionListener(new ExpansionAdapter() {
						
						@Override
						public void expansionStateChanged(ExpansionEvent e) {
							if (sec.isExpanded()){
								sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
							}else{
								sec.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
							}
							sec.getParent().layout();
							
						}
					});
					
					sec.setText("Leg: " + pld.getPatrolLeg().getId());
					PatrolLegDayInputComposite comp = new PatrolLegDayInputComposite(this);
					Composite comp2 = comp.createComposite(sec, toolkit);
					comp.setData(pld);
					sec.setClient(comp2);
					children[i] = comp;
					
				}
			}
			
		}finally{
			session.close();
		}
		
		frmSummary.getBody().getParent().layout();
		
	}

	/**
	 * @return the patrol editor associated with this editor parge
	 */
	public PatrolEditor getPatrolEditor(){
		return this.editor;
	}
	
	@Override
	public void setFocus() {
		frmSummary.setFocus();
	}

	/**
	 * Updates the information for each child composite
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		if (children != null){
			for (int i = 0; i < children.length; i ++){
				children[i].updateLegDay();
			}
		}
	}

	/**
	 * Not supported
	 */
	@Override
	public void doSaveAs() {
		// Do the Save As operation
	}
	
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}


}
