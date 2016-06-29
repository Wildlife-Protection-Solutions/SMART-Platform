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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyWaypoint;

/**
 * Mission Day Page. It represents one day within a mission.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class MissionDayPage extends EditorPart {

	private MissionEditor editor;
	private Date date;
	
	private ScrolledForm frmSummary; 
	private FormToolkit toolkit;
	
	private MissionDayComposite dayComposite;
	
	public MissionDayPage(MissionEditor editor) {
		this.editor = editor;
	}
		
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		date = ((MissionDayPageEditorInput)input).getDay();
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
		toolkit = new FormToolkit(parent.getDisplay());
		toolkit.setBorderStyle(SWT.BORDER);
		
		frmSummary = toolkit.createScrolledForm(parent);

		String errorMsg = editor.canEdit();
		boolean canEdit = errorMsg == null;
		if (!canEdit){
			editor.createEditWarning(errorMsg, frmSummary.getBody(), toolkit);
		}
		
		frmSummary.getBody().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE"); //$NON-NLS-1$
		StringBuilder text = new StringBuilder(Messages.MissionDayPage_Mission);
		text.append(" "); //$NON-NLS-1$
		text.append(dayFormat.format(date));
		text.append(", "); //$NON-NLS-1$
		text.append(DateFormat.getDateInstance(DateFormat.MEDIUM).format(date));
		frmSummary.setText(text.toString());
		frmSummary.getBody().setLayout(new GridLayout(1, false));
		
		dayComposite = new MissionDayComposite(this);
		dayComposite.createComposite(frmSummary.getBody(), toolkit);
		dayComposite.initData();
	}

	public void findAndGoTo(SurveyWaypoint pw){
		dayComposite.selectWaypoint(pw);
	}
	
	public void refresh(){
		dayComposite.initData();
	}
	
	@Override
	public void setFocus() {
		frmSummary.setFocus();
	}

	@Override
	public void dispose() {
		super.dispose();
		dayComposite.dispose();
	}
	
	public MissionEditor getMissionEditor() {
		return editor;
	}
	
	public Date getDay() {
		return date;
	}
}
