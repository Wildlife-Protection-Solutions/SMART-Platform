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
package org.wcs.smart.er.ui.mision.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;

/**
 * Mission Day Page. It represents one day within a mission.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionDayPage extends EditorPart {

	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Composite mainComposite;

	private DateTime dtStartTime;
	private DateTime dtEndTime;
	private Text restMinutes;
	private Label lblTotalHours;

	private TableViewer observationTable;
	private Text txtDistance;
	
	private Font okayFont;
	private Font errorFont;
	private Hyperlink lnkImportWaypoints;
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		mainComposite = toolkit.createComposite(parent);
		mainComposite.setLayout(new GridLayout(1, false));
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite timeInfo = toolkit.createComposite(mainComposite);
		timeInfo.setLayout(new GridLayout(4, false));
		((GridLayout) timeInfo.getLayout()).horizontalSpacing = 15;
		 ((GridLayout)timeInfo.getLayout()).marginWidth = 0;
		 ((GridLayout)timeInfo.getLayout()).marginLeft = 5;
		 ((GridLayout)timeInfo.getLayout()).marginHeight = 5;
		timeInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, "StartTime:");
		dtStartTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtStartTime);
		dtStartTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
			
		});
//		dtStartTime.addFocusListener(new FocusAdapter() {			
//			@Override
//			public void focusLost(FocusEvent e) {
//				if (timeEqual(SmartUtils.getTime(dtStartTime).getTime(), patrolLegDate.getStartTime().getTime())){
//					//no changes made
//					return;
//				}
//				editor.getPatrolEditor().save(patrolLegDate);
//				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
//			}
//		});

		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, "EndTime:");
		dtEndTime = new DateTime(c, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
		toolkit.adapt(dtEndTime);
		dtEndTime.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateTotalHours();
			}
			
		});
//		dtEndTime.addFocusListener(new FocusAdapter() {			
//			@Override
//			public void focusLost(FocusEvent e) {
//				if (timeEqual(SmartUtils.getTime(dtEndTime).getTime(), patrolLegDate.getEndTime().getTime())){
//					//no changes made
//					return;
//				}
//				editor.getPatrolEditor().save(patrolLegDate);
//				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
//			}
//		});

		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, "RestMinutes:");
		restMinutes = toolkit.createText(c, "0", SWT.BORDER); //$NON-NLS-1$
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 30;
		restMinutes.setLayoutData(gd);
//		restMinutes.addFocusListener(new FocusListener() {
//			private int oldValue; 
//			
//			@Override
//			public void focusLost(FocusEvent e) {
//				try{
//					int x = Integer.parseInt(restMinutes.getText());
//					if (patrolLegDate.getRestMinutes() != null && x == patrolLegDate.getRestMinutes()){
//						return;
//					}
//					if (x < 0){
//						throw new Exception("Rest minutes cannot be negative."); //$NON-NLS-1$
//					}
//				}catch (Exception ex){
//					restMinutes.setText(String.valueOf(oldValue));
//					MessageDialog.openWarning(Display.getCurrent().getActiveShell(), Messages.PatrolLegDayInputComposite_Error_DialogTitle, Messages.PatrolLegDayInputComposite_InvalidRestMinutes_DialogMessage1);
//					Display.getCurrent().asyncExec(new Runnable() {
//						@Override
//						public void run() {
//							restMinutes.setFocus();
//						}
//					});
//					
//				}
//				updateTotalHours();
//				editor.getPatrolEditor().save(patrolLegDate);
//				PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_DATES_LEG, patrolLegDate);
//			}
//			
//			@Override
//			public void focusGained(FocusEvent e) {
//				oldValue = Integer.parseInt(restMinutes.getText());
//			}
//		});
		
		
		c = toolkit.createComposite(timeInfo);
		c.setLayout(new GridLayout(2, false));
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		toolkit.createLabel(c, "Total Hours:");
		lblTotalHours = toolkit.createLabel(c, "InvalidTotalHours");
		okayFont = lblTotalHours.getFont();
		
		FontData fd = okayFont.getFontData()[0];
		fd.setStyle(SWT.BOLD);
		errorFont = new Font(Display.getDefault(), fd);
		
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 30;
		lblTotalHours.setLayoutData(gd);

		Composite trackComp = toolkit.createComposite(mainComposite);
		trackComp.setLayout(new GridLayout(4, false));
		
		
		toolkit.createLabel(trackComp, "Distance Travelled (km):");
		txtDistance = toolkit.createText(trackComp, "0", SWT.NONE); //$NON-NLS-1$
		txtDistance.setEditable(false);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 50;
		txtDistance.setLayoutData(gd);
		
		Composite observationHcomp = toolkit.createComposite(mainComposite);
		observationHcomp.setLayout(new GridLayout(2, false));
		toolkit.createLabel(observationHcomp, "Observations/Waypoints");
		lnkImportWaypoints = toolkit.createHyperlink(observationHcomp, "Import Waypoints", SWT.NONE);
		lnkImportWaypoints.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				showImportWaypointWizard();
			}
		});
	}

	protected void showImportWaypointWizard() {
		// TODO Auto-generated method stub
		
	}

	protected void updateTotalHours() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFocus() {
		dtStartTime.setFocus();
	}

}
