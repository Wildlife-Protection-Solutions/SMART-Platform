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

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
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
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.util.SharedUtils;

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
	
	private PatrolLegDayInputComposite[] children ;
	private ScrolledForm frmSummary; 
	private FormToolkit toolkit;
	
	
	public PatrolDayEditor(PatrolEditor editor) {
		super.setPartName(""); //$NON-NLS-1$
		this.editor = editor;
		
	}
	
	@Override
	public void dispose(){
		if (toolkit != null){
			toolkit.dispose();
			toolkit = null;
		}
		
		if (children != null){
			for (int i = 0; i < children.length; i ++){
				children[i].dispose();
			}
		}
		super.dispose();
	}
	
	/**
	 * Create contents of the editor part.
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		Session session = HibernateManager.openSession();	
		ObservationOptions observationOptions = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session);
		if (observationOptions.getViewProjection() != null) {
			observationOptions.getViewProjection().getDefinition(); //load lazy items
		}

		session.beginTransaction();
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
			Label lblWarning = toolkit.createLabel(warning, "", SWT.NONE); //$NON-NLS-1$
			lblWarning.setText(MessageFormat.format(Messages.PatrolDayEditor_CanNotEditPatrol, new Object[]{canEdit})) ;
		}
		
		SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE"); //$NON-NLS-1$
		StringBuilder text = new StringBuilder(Messages.PatrolDayEditor_PatrolDayTitle);
		text.append(" "); //$NON-NLS-1$
		text.append(dayFormat.format(((PatrolDayEditorInput)getEditorInput()).getPatrolDay()));
		text.append(", "); //$NON-NLS-1$
		text.append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(((PatrolDayEditorInput)getEditorInput()).getPatrolDay()));
		frmSummary.setText(text.toString());
		frmSummary.getBody().setLayout(new GridLayout(1, false));
		
		try{
			//find all patrol legs for this day
			List<PatrolLeg> legs = editor.getPatrol().getLegs();		
			ArrayList<PatrolLegDay> plds = new ArrayList<PatrolLegDay>();

			for (PatrolLeg leg: legs){
				for (PatrolLegDay day : leg.getPatrolLegDays()){
					if (SharedUtils.getDatePart(day.getDate(), false).equals(  SharedUtils.getDatePart( ((PatrolDayEditorInput)getEditorInput()).getPatrolDay(), false))){
						plds.add(day);
					}
					
					//load waypoints and attach to session; for performance reasons
					//waypoints are not cascaded (otherwise saves are cascaded too)
					if (day.getWaypoints() != null){
						for (PatrolWaypoint wp : day.getWaypoints()) {
							session.update(wp.getWaypoint());
							if (wp.getWaypoint().getObservations() != null){
								wp.getWaypoint().getObservations().size();
							}
						}
					}
				}
			}
			
			if (plds.size() == 0){
				Label lbl = toolkit.createLabel(frmSummary.getBody(), "", SWT.WRAP); //$NON-NLS-1$
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				gd.widthHint =lbl.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				lbl.setLayoutData(gd);
				lbl.setText(Messages.PatrolDayEditor_Error_LoadingPatrolData);
				
			}else if (plds.size() == 1){
				children = new PatrolLegDayInputComposite[1];
				PatrolLegDayInputComposite comp = new PatrolLegDayInputComposite(this, observationOptions);
				comp.createComposite(frmSummary.getBody(), toolkit);
				comp.setData((PatrolLegDay)plds.get(0));
				children[0] = comp;
				
			}else{
				//sort legs by start date
				Collections.sort(plds, new Comparator<PatrolLegDay>(){

					@Override
					public int compare(PatrolLegDay o1, PatrolLegDay o2) {
						if (o1.getStartTime().before(o2.getStartTime())){
							return -1;
						}else if (o1.getStartTime().after(o2.getStartTime())){
							return 1;
						}else{
							return Collator.getInstance().compare(o1.getPatrolLeg().getId(),o2.getPatrolLeg().getId());
						}
					}});
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
					
					sec.setText(Messages.PatrolDayEditor_LegSectionNamePrefix + pld.getPatrolLeg().getId());
					PatrolLegDayInputComposite comp = new PatrolLegDayInputComposite(this, observationOptions);
					Composite comp2 = comp.createComposite(sec, toolkit);
					comp.setData(pld);
					sec.setClient(comp2);
					children[i] = comp;					
				}
				
			}
			
		}finally{
			session.getTransaction().rollback();
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
