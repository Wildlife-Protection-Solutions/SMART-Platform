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
package org.wcs.smart.observation.common.importwp;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.observation.common.importwp.GPSDataImport.ImportType;
import org.wcs.smart.observation.internal.Messages;

/**
 * Option composite for importing tracks and waypoints.  Three
 * options types are availabe: all, date and select.
 * 
 * @author Emily
 *
 */
public class ImportOptionsComposite extends Composite {

	public enum ImportOption{
		ALL, DATE, SELECT;
	}
	private Button opAll;
	private Button opDate;
	private Button opSelect;
	
	private ImportType importType;
	private String warnMessage;
	
	private ImportOption[] validOptions = {ImportOption.ALL, ImportOption.DATE, ImportOption.SELECT};
	private String[] labels = {Messages.ImportGpxWizardPage_ImportAllOp,
			Messages.ImportGpxWizardPage_ImportSingleDayOp,
			Messages.ImportGpxWizardPage_ImportSelectionOp};
			

//	private Date date;
	
	private Listener selectionListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			ImportOptionsComposite.this.notifyListeners(SWT.Selection,event);
		}
	};
	
//	public ImportOptionsComposite(Composite parent, Date date, ImportType importType) {
//		super(parent, SWT.NONE);
//		this.importType = importType;
//		String dateStr = date != null ? DateFormat.getDateInstance(DateFormat.MEDIUM).format(date) : ""; //$NON-NLS-1$
//		validOptions = new ImportOption[]{ImportOption.ALL, ImportOption.DATE, ImportOption.SELECT};
//		labels = new String[]{Messages.ImportGpxWizardPage_ImportAllOp,
//				MessageFormat.format(Messages.ImportGpxWizardPage_ImportSingleDayOp, new Object[]{importType.guiName.toLowerCase(), dateStr}),
//				MessageFormat.format(Messages.ImportGpxWizardPage_ImportSelectionOp, new Object[]{importType.guiName.toLowerCase(), dateStr})
//		};
//
//		createContent();
//	}

//	public ImportOptionsComposite(Composite parent, Date date, ImportType importType) {
//		super(parent, SWT.NONE);
//		this.date = date;
//		this.importType = importType;
//		createContent();
//	}
	
	/**
	 * 
	 * @param parent
	 * @param pld
	 * @param importType
	 * @param validOptions
	 * @param opLabels labels much contain the same place holders as existing labels
	 * @param warnUser if import All should include verification warning
	 */
	public ImportOptionsComposite(Composite parent,
			ImportType importType, 
			ImportOption[] validOptions,
			String[] opLabels,
			String warnMessage) {
		super(parent, SWT.NONE);
		this.importType = importType;
		this.validOptions = validOptions;
		this.labels = opLabels;
		this.warnMessage = warnMessage;
		createContent();
	}
	
	protected ImportOption[] getValidOptions() {
		return validOptions;
	}
	
	protected ImportType getImportType() {
		return importType;
	}
	
	protected void createContent() {
		setLayout(new GridLayout());
		Composite ops = new Composite(this, SWT.NONE);
		ops.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		ops.setLayout(new GridLayout(1, false));
		
		for (int i = 0; i < validOptions.length;i++){
		 ImportOption op = validOptions[i];
			switch (op) {
			case ALL:
				opAll = createOptionButton(ops, labels[i]);
				opAll.setSelection(true);
				break;
			case DATE:
				opDate = createOptionButton(ops, labels[i]);
				break;
			case SELECT:
				opSelect = createOptionButton(ops, labels[i]);
				break;
			}	
		}
		
		if (warnMessage != null){
			Label lbl1 = new Label(this, SWT.WRAP);
			lbl1.setText(warnMessage);
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
			gd.widthHint = 500;
			gd.verticalIndent = 10;
			lbl1.setLayoutData(gd);
		}

		//TODO: fix
//		if (all && warnUser && pld.getPatrolLeg().getPatrol().getLegs().size() > 1){
//			Label lbl1 = new Label(this, SWT.WRAP);
//			lbl1.setText(MessageFormat.format(Messages.ImportOptionsComposite_ImportAllWarning , new Object[]{importType.guiName}));
//			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
//			gd.widthHint = 500;
//			gd.verticalIndent = 10;
//			lbl1.setLayoutData(gd);
//		}
	}
	
//	protected String getDateString() {
//		if (date == null)
//			return ""; //$NON-NLS-1$
//		String op = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
//		//TODO: fix
////		if (pld.getPatrolLeg().getPatrol().getLegs().size() > 1){
////			//more than one patrol; include leg id in import option
////			op += " (" + Messages.ImportOptionsComposite_LegPrefix + ": " + pld.getPatrolLeg().getId() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
////		}
//		return op;
//	}

	private Button createOptionButton(Composite parent, String label){
		Button btn = new Button(parent, SWT.RADIO);
		btn.setText(label);
		btn.addListener(SWT.Selection, selectionListener);
		return btn;
	}
	
	
//	private void createOpSelect(Composite parent, String label){
//		opSelect = new Button(parent, SWT.RADIO);
//		opSelect.setText(MessageFormat.format(label, new Object[]{importType.guiName.toLowerCase(), getDateString()}));
//		opSelect.addListener(SWT.Selection, selectionListener);
//	}
//	private void createOpDate(Composite parent, String label){
//		opDate = new Button(parent, SWT.RADIO);
//		opDate.setText(MessageFormat.format(label, new Object[]{importType.guiName.toLowerCase(), getDateString()}) );
//		opDate.setSelection(false);
//		opDate.addListener(SWT.Selection, selectionListener);
//	}
//	
//	private void createOpAll(Composite parent, String label){
//		opAll = new Button(parent, SWT.RADIO);
//		String message = label;
//		//TODO: fix
////		if (pld.getPatrolLeg().getPatrol().getLegs().size() > 1 && warnUser){
////			message += "*"; //$NON-NLS-1$
////		}
//		opAll.setText(message);
//		opAll.setSelection(true);
//		opAll.addListener(SWT.Selection, selectionListener);
//	}
	
	/**
	 * 
	 * @return the selected import option
	 */
	public ImportOption getImportOption(){
		if (opAll.getSelection()){
			return ImportOption.ALL;
		}else if (opDate.getSelection()){
			return ImportOption.DATE;
		}else if (opSelect.getSelection()){
			return ImportOption.SELECT;
		}
		return null;
	}
}
