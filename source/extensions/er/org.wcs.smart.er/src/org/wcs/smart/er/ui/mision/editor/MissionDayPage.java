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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.model.ObservationOptions;

/**
 * Mission Day Page. It represents one day within a mission.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionDayPage extends EditorPart {

	private MissionEditor editor;
	private Date day;
	
	private ScrolledForm frmSummary; 
	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	
	private MissionDayComposite dayComposite;
	
	public MissionDayPage(MissionEditor editor) {
		this.editor = editor;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		//not supported
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		day = ((MissionDayPageEditorInput)input).getDay();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {

		Session session = HibernateManager.openSession();	
		ObservationOptions observationOptions = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), session);
		if (observationOptions.getViewProjection() != null) {
			observationOptions.getViewProjection().getDefinition(); //load lazy items
		}

		frmSummary = toolkit.createScrolledForm(parent);
		
		frmSummary.getBody().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE"); //$NON-NLS-1$
		StringBuilder text = new StringBuilder("Mission:");
		text.append(" "); //$NON-NLS-1$
		text.append(dayFormat.format(day));
		text.append(", "); //$NON-NLS-1$
		text.append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(day));
		frmSummary.setText(text.toString());
		frmSummary.getBody().setLayout(new GridLayout(1, false));
		
		dayComposite = new MissionDayComposite(this, observationOptions);
		dayComposite.createComposite(frmSummary.getBody(), toolkit);
		dayComposite.setData(editor.getMission());
		
	}

	@Override
	public void setFocus() {
		frmSummary.setFocus();
	}

	@Override
	public void dispose() {
		dayComposite.dispose();
		super.dispose();
	}
	
	public MissionEditor getMissionEditor() {
		return editor;
	}
	
	public Date getDay() {
		return day;
	}
}
