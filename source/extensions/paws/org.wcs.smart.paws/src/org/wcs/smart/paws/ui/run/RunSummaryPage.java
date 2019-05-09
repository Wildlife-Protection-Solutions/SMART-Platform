package org.wcs.smart.paws.ui.run;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.ui.HeaderComposite;

public class RunSummaryPage extends EditorPart {

	private HeaderComposite header;
	private Label lblStatus, lblStatusMsg;
	private Composite detailsComp ;
	
	private RunEditor parent;
	private FormToolkit toolkit;

	
	public RunSummaryPage(RunEditor parent) {
		this.parent = parent;
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void dispose() {
		super.dispose();
		toolkit.dispose();
	}
	
	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		// TODO Auto-generated method stub
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form main = toolkit.createForm(parent);
		main.getBody().setLayout(new GridLayout());
		
		
		header = new HeaderComposite(main.getBody(), toolkit, main.getFont(), main.getForeground());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SmartUiUtils.createHeaderLabel(main.getBody(), "Status");
		
		Composite scomp = toolkit.createComposite(main.getBody());
		scomp.setLayout(new GridLayout(2, false));
		scomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		
		lblStatus = toolkit.createLabel(scomp, "");
		lblStatusMsg = toolkit.createLabel(scomp, "");
				
		
		SmartUiUtils.createHeaderLabel(main.getBody(), "Details");
		
		detailsComp = toolkit.createComposite(main.getBody());
		detailsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	}

	public void init(PawsRun run) {
		lblStatus.setText(run.getStatus().name());
		if (run.getStatusMessage() != null) lblStatusMsg.setText(run.getStatusMessage());
		
		lblStatus.getParent().layout(true);
		
		header.setText(run.getId());
		
		for (Control kid : detailsComp.getChildren()) kid.dispose();
		
		detailsComp.setLayout(new GridLayout(2, false));
		
		toolkit.createLabel(detailsComp, "PAWS Run Id:");
		toolkit.createLabel(detailsComp, run.getId());
		
		toolkit.createLabel(detailsComp, "Executed On:");
		toolkit.createLabel(detailsComp, run.getRunDate() == null ? "" : run.getRunDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)));
		
		toolkit.createLabel(detailsComp, "Data Dates:");
		toolkit.createLabel(detailsComp, "TODO");
		
		toolkit.createLabel(detailsComp, "Settings:");
		toolkit.createLabel(detailsComp, run.getConfiguration().getName());
		
		toolkit.createLabel(detailsComp, "Local Package File:");
		toolkit.createLabel(detailsComp, run.getPackageFile());
		
		toolkit.createLabel(detailsComp, "Local Results File:");
		toolkit.createLabel(detailsComp, run.getResultLocation());
		
		detailsComp.layout(true);
	}
	@Override
	public void setFocus() {
		lblStatus.setFocus();
	}

}
