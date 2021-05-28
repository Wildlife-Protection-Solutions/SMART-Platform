package org.wcs.smart.i2.ui;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.ui.ProjectionLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class WorkingSetExportDialog extends SmartStyledTitleDialog {

	private static final String LAST_DIR_KEY = WorkingSetExportDialog.class.getName() + ".dir"; //$NON-NLS-1$
	
	private DelimiterCombo cmbDelimiter;
	private ComboViewer cmbCharset, cmbProjection;
	private Text txtFile;
	private IntelWorkingSet wset;
	
	private char delimiter;
	private Projection prj;
	private String filename;
	private Charset charset;
	
	public WorkingSetExportDialog(Shell parent, IntelWorkingSet ws) {
		super(parent);
		this.wset = ws;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		
		getButton(IDialogConstants.OK_ID).setText(DialogConstants.EXPORT_BUTTON_TEXT);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	public char getDelimiter() {
		return this.delimiter;
	}
	public String getFilename() {
		return this.filename;
	}
	public Projection getProjection() {
		return this.prj;
	}
	public Charset getCharset() {
		return this.charset;
	}
	public void okPressed() {
		validate();
		if (!validate()) return; 
		
		try {
			this.delimiter = cmbDelimiter.getDelimiter();
		} catch (Exception e) {
		}
		
		this.prj = (Projection) cmbProjection.getStructuredSelection().getFirstElement();
		this.charset = (Charset) cmbCharset.getStructuredSelection().getFirstElement();
		this.filename = txtFile.getText();
		if (!this.filename.endsWith(".zip")) this.filename = this.filename + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
		Intelligence2PlugIn.getDefault().getDialogSettings().put(LAST_DIR_KEY, filename);
		
		super.okPressed();
	}
	
	private boolean validate() {
		String msg = null;
		if (txtFile.getText().trim().isEmpty()) {
			msg = Messages.WorkingSetExportDialog_outputrequired;
		}
		if (cmbCharset.getStructuredSelection().isEmpty()) {
			msg = Messages.WorkingSetExportDialog_charsetrequired;
		}
		if (cmbDelimiter.getStructuredSelection().isEmpty()) {
			msg = Messages.WorkingSetExportDialog_delimiterrequired;
		}
		if (cmbProjection.getStructuredSelection().isEmpty()) {
			msg = Messages.WorkingSetExportDialog_projectionrequired;
		}
		try {
			cmbDelimiter.getDelimiter();
		}catch (Exception ex) {
			msg = ex.getMessage();
		}
		Path temp = Paths.get(txtFile.getText());
		if (temp == null || temp.getParent() == null || Files.isDirectory(temp)) {
			msg = Messages.WorkingSetExportDialog_invalidoutputfile;
		}
		setErrorMessage(msg);
		Button btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) btn.setEnabled(msg == null);
		return msg == null;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lbl = new Label(main, SWT.NONE);
		lbl.setText(Messages.WorkingSetExportDialog_OutputLbl);
		
		
		String location = Intelligence2PlugIn.getDefault().getDialogSettings().get(LAST_DIR_KEY);
		if (location == null){
			location = System.getProperty("user.home"); //$NON-NLS-1$	
		}else {
			Path temp = Paths.get(location);
			if (temp != null && temp.getParent() != null) {
				location = temp.getParent().toString();
			}else {
				location = System.getProperty("user.home"); //$NON-NLS-1$	
			}
		}
		location = location + File.separator + URLUtils.cleanFilename(wset.getName()) + ".zip"; //$NON-NLS-1$
		
		txtFile = new Text(main, SWT.BORDER);
		txtFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtFile.setText(location);
		txtFile.addListener(SWT.Modify, e->validate());
		
		Button btnBrowse = new Button(main, SWT.NONE);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
			fd.setFilterExtensions(new String[] {"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] {Messages.WorkingSetView_ZipFiles, Messages.WorkingSetView_AllFiles});
			fd.setText(Messages.WorkingSetView_ExportFileTitle);
			fd.setFileName(txtFile.getText());
			String fname = fd.open();
			if (fname == null) return;
			
			if (!fname.endsWith(".zip")) fname = fname + ".zip"; //$NON-NLS-1$ //$NON-NLS-2$
			txtFile.setText(fname);
		});
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnBrowse.getLayoutData()).heightHint = txtFile.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		
		createDelimiterOption(main);
		createProjectionOption(main);
		createCharsetOption(main);
		
		validate();
		
		setTitle(Messages.WorkingSetExportDialog_Title);
		getShell().setText(Messages.WorkingSetExportDialog_Title);
		setMessage(Messages.WorkingSetExportDialog_Msg);
		
		loadData.schedule();
		
		return parent;
	}
	
	
	private void createCharsetOption(Composite main) {
		Label lblCharset = new Label(main, SWT.NONE);
		lblCharset.setText(Messages.WorkingSetExportDialog_CharsetLbl);
		lblCharset.setToolTipText(Messages.WorkingSetExportDialog_CharsetTooltip);
		
		cmbCharset = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbCharset.setContentProvider(ArrayContentProvider.getInstance());
		cmbCharset.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((Charset)element).displayName();
			}
		});
		cmbCharset.setInput( Charset.availableCharsets().values() );
		cmbCharset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Charset defaultcs = StandardCharsets.UTF_8;
		try {
			String cc = SmartPlugIn.getDefault().getDialogSettings().get(SmartPlugIn.DEFAULT_ENCODING_KEY);
			if (cc != null && !cc.isBlank()) defaultcs = Charset.forName(cc);
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		cmbCharset.setSelection(new StructuredSelection(defaultcs));
		cmbCharset.addSelectionChangedListener(e->{
			SmartPlugIn.getDefault().getDialogSettings().put(SmartPlugIn.DEFAULT_ENCODING_KEY, ((Charset)e.getStructuredSelection().getFirstElement()).name());
			validate();
		});
		
		new Label(main, SWT.NONE);
	}
	
	private void createDelimiterOption(Composite main){
		Label lblDelimiter = new Label(main, SWT.NONE);
		lblDelimiter.setText(Messages.WorkingSetExportDialog_DelimiterLbl);
		lblDelimiter.setToolTipText(Messages.WorkingSetExportDialog_DelmiterTooltip);
		
		cmbDelimiter = new DelimiterCombo(main,  SWT.DROP_DOWN);
		cmbDelimiter.addSelectionChangedListener(e->validate());
		
		new Label(main, SWT.NONE);
	}
	
	private void createProjectionOption(Composite main){
		
		Label lblProjection = new Label(main, SWT.NONE);
		lblProjection.setText(Messages.WorkingSetExportDialog_PrjLbl);
		lblProjection.setToolTipText(Messages.WorkingSetExportDialog_PrjTooltip);
		
		cmbProjection = new ComboViewer(main, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbProjection.setContentProvider(ArrayContentProvider.getInstance());
		cmbProjection.setLabelProvider(ProjectionLabelProvider.getInstance());
		cmbProjection.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbProjection.addSelectionChangedListener(e->validate());

		new Label(main, SWT.NONE);
	}
	
	private Job loadData = new Job("load data") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Projection> items = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				items.addAll(QueryFactory.buildQuery(session, Projection.class, 
						new Object[] {"conservationArea", wset.getConservationArea()}).list()); //$NON-NLS-1$
			}
			
			Projection defaultp = items.get(0);
			for (Projection item : items) {
				if (item.getIsDefault()) defaultp = item;
			}
			Projection fdefaultp = defaultp;
			Display.getDefault().syncExec(()->{
				cmbProjection.setInput(items);
				cmbProjection.setSelection(new StructuredSelection(fdefaultp));
				validate();
				
			});
			return Status.OK_STATUS;
		}
		
	};
}
