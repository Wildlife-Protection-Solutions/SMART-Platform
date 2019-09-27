/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.dialogs;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.export.dialog.DelimiterCombo;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.entity.exporter.EntityRelationshipExporter;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.xml.EntityToXml;
import org.wcs.smart.ui.SmartStyledTitleDialog;

/**
 * Export entities to xml for csv dialog 
 * 
 * @author Emily
 *
 */
public class ExportEntityToFileDialog extends SmartStyledTitleDialog {

	public static final String PREFERENCE_DIR_KEY = ExportEntityToFileDialog.class.getCanonicalName() + ".dir"; //$NON-NLS-1$

	public static final String EXPORT_DIALOG_TITLE = Messages.ExportEntityXmlDialog_ExportDialogTitle;

	private Button btnIncludeRelationships;
	private Button btnIncludeRecords;
	private Button btnIncludeRecordXml;
	private Text txtOutputFile;

	private DelimiterCombo cmbDelimiters;
	private ComboViewer cmbCharset;
	private List<UUID> entityUuids;
	private ComboViewer cmbFormat;
	
	private static enum Format{
		CSV(Messages.ExportEntityXmlDialog_CSVFormat, "csv"), //$NON-NLS-1$
		XML(Messages.ExportEntityXmlDialog_XMLFormat, "zip"); //$NON-NLS-1$
		
		public String guiName;
		public String extension;
		
		Format(String name, String extension){
			this.guiName = name;	
			this.extension = extension;
		}
	}
	
	/**
	 * if no entity uuids are provided then all entities are exported.
	 * @param parentShell
	 * @param entityUuids
	 */
	public ExportEntityToFileDialog(Shell parentShell, List<UUID> entityUuids) {
		super(parentShell);
		this.entityUuids = entityUuids;
	}

	@Override
	public void cancelPressed() {
		super.cancelPressed();
	}

	@Override
	public void okPressed() {
		// check for data
		if (entityUuids != null && entityUuids.isEmpty()) {
			MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportEntityXmlDialog_NothingToExportMsg);
			return;
		}
		try {
			// configure output file
			Path outputFile = null;
			final Format format = getFormat();
			String name = txtOutputFile.getText();
			if (name.isEmpty()) {
				MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportEntityXmlDialog_InvalidFile);
				return;
			}
			if (!name.endsWith("." + format.extension)) { //$NON-NLS-1$
				name = name + "." + format.extension; //$NON-NLS-1$
			}
			outputFile = Paths.get(name).toAbsolutePath();
			if (Files.isDirectory(outputFile)) {
				MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportEntityXmlDialog_InvalidFile);
				return;
			}
			Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_DIR_KEY, outputFile.toString());

			if (!Files.exists(outputFile.getParent())) {
				Files.createDirectories(outputFile.getParent());
			}

			if (Files.exists(outputFile)) {
				if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOG_TITLE,
						MessageFormat.format(Messages.ExportEntityXmlDialog_FileExists,
								outputFile.toString()))) {
					return;
				}
			}

			final String filename = outputFile.toString();
			final Path outFile = outputFile;
			final boolean includeRecords = btnIncludeRecords.getSelection();
			final boolean includeRelationships = btnIncludeRelationships.getSelection();
			final boolean includeRecordXml = btnIncludeRecordXml.getSelection();
			final char delimiter = cmbDelimiters.getDelimiter();
			final Charset cs = (Charset) cmbCharset.getStructuredSelection().getFirstElement();
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {

				@Override
				@SuppressWarnings("unchecked")
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor progress = SubMonitor.convert(monitor, Messages.ExportEntityXmlDialog_Task, 1);
					try (Session s = HibernateManager.openSession()) {
						if (entityUuids == null) {
							entityUuids = new ArrayList<>();
							List<Object> items = s.createQuery("SELECT uuid FROM IntelEntity WHERE conservationArea = :ca") //$NON-NLS-1$
							.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
							.list();
							items.forEach(item-> entityUuids.add((UUID)item));
						}
						if (format == Format.XML) {
							EntityToXml hh = new EntityToXml(s);
							hh.export(outFile, entityUuids, includeRecords, includeRelationships, includeRecordXml, progress.split(1));
						}else if (format == Format.CSV) {
							
							EntityRelationshipExporter exporter = new EntityRelationshipExporter();
							exporter.exportEntities(entityUuids, 0, outFile, delimiter, cs, progress.split(1));
						}
						// export to file complete
						getShell().getDisplay().syncExec(() -> MessageDialog.openInformation(getShell(),
								EXPORT_DIALOG_TITLE, MessageFormat.format(Messages.ExportEntityXmlDialog_CompleteMsg, filename)));
					} catch (OperationCanceledException ex) {
						getShell().getDisplay().syncExec(() -> MessageDialog.openInformation(getShell(),
								EXPORT_DIALOG_TITLE, Messages.ExportEntityXmlDialog_CanceledMsg));
					} catch (Exception e) {
						Intelligence2PlugIn.displayLog(Messages.ExportEntityXmlDialog_ExportError + e.getMessage(), e);
					}
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.displayLog(Messages.ExportEntityXmlDialog_ExportError + e.getMessage(), e);
		}
		super.okPressed();
	}

	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		Composite mainSection = new Composite(outer, SWT.NONE);
		mainSection.setLayout(new GridLayout(3, false));
		mainSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label ll = new Label(mainSection, SWT.NONE);
		ll.setText(Messages.ExportEntityXmlDialog_FormatLabel);
		
		cmbFormat = new ComboViewer(mainSection, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		cmbFormat.setContentProvider(ArrayContentProvider.getInstance());
		cmbFormat.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbFormat.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object x) {
				return ((Format)x).guiName;
			}
		});
		cmbFormat.setInput(Format.values());
		cmbFormat.setSelection(new StructuredSelection(Format.CSV));
		cmbFormat.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fixOutputFileExtension();
			}
		});
		
		Label l = new Label(mainSection, SWT.RADIO);
		l.setText(Messages.ExportEntityXmlDialog_FileLabel);

		txtOutputFile = new Text(mainSection, SWT.BORDER);
		txtOutputFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnBrowse = new Button(mainSection, SWT.NONE);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData) btnBrowse.getLayoutData()).heightHint = txtOutputFile.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

		btnBrowse.addListener(SWT.Selection, e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterExtensions(getFileOptions(true)); 
			dialog.setFilterNames(getFileOptions(false));
			String t = txtOutputFile.getText();
			if (t.length() > 0) {
				dialog.setFileName(t);
			}
			dialog.setText(Messages.ExportEntityXmlDialog_FileDialogTitle);
			String file = dialog.open();
			if (file != null){
				txtOutputFile.setText(file);
				fixOutputFileExtension();
			}
		});
		
		l = new Label(mainSection, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		

		Composite optionsComposite = new Composite(mainSection, SWT.NONE);
		optionsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		optionsComposite.setLayout(new StackLayout());
		
		Composite csvOp = new Composite(optionsComposite, SWT.NONE);
		csvOp.setLayout(new GridLayout(2, false));
		
		Label delLabel = new Label(csvOp, SWT.NONE);
		delLabel.setText(Messages.ExportEntityXmlDialog_csvDelimoption);
		
		cmbDelimiters = new DelimiterCombo (csvOp, SWT.DROP_DOWN);
		cmbDelimiters.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblCharset = new Label(csvOp, SWT.NONE);
		lblCharset.setText("Character Set:");
		lblCharset.setToolTipText("The character encoding of the file.  If unsure try UTF-8.");
		lblCharset.setBackground(csvOp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		cmbCharset = new ComboViewer(csvOp, SWT.DROP_DOWN | SWT.READ_ONLY);
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
		});
		cmbCharset.getControl().setBackground(csvOp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		Composite xmlOp = new Composite(optionsComposite, SWT.NONE);
		xmlOp.setLayout(new GridLayout());
		
		btnIncludeRelationships = new Button(xmlOp, SWT.CHECK);
		btnIncludeRelationships.setText(Messages.ExportEntityXmlDialog_IncludeRelationships);
		btnIncludeRelationships.setSelection(true);
		btnIncludeRelationships.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIncludeRecords = new Button(xmlOp, SWT.CHECK);
		btnIncludeRecords.setText(Messages.ExportEntityXmlDialog_IncludeRecordLinks);
		btnIncludeRecords.setSelection(true);
		btnIncludeRecords.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnIncludeRecordXml = new Button(xmlOp, SWT.CHECK);
		btnIncludeRecordXml.setText(Messages.ExportEntityToFileDialog_includeRecordXmls);
		btnIncludeRecordXml.setSelection(true);
		btnIncludeRecordXml.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		((StackLayout)optionsComposite.getLayout()).topControl = csvOp;
		
		cmbFormat.getControl().addListener(SWT.Selection, e->{
			if (getFormat() == Format.XML) {
				((StackLayout)optionsComposite.getLayout()).topControl = xmlOp;
			}else {
				((StackLayout)optionsComposite.getLayout()).topControl = csvOp;
			}
			optionsComposite.layout();
		});

		/* initialize fields */
		String initDir = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
		if (initDir != null) {
			txtOutputFile.setText(initDir);
		}
		cmbFormat.setSelection(new StructuredSelection(Format.CSV));
		
		setTitle(Messages.ExportEntityXmlDialog_Title);
		setMessage(MessageFormat.format(Messages.ExportEntityXmlDialog_Message, entityUuids.size()));
		getShell().setText(Messages.ExportEntityXmlDialog_ShellTitle);
		return parent;
	}

	private void fixOutputFileExtension(){
		if (txtOutputFile.getText().isEmpty()) return;
		String ext = getFormat().extension;
		if (!txtOutputFile.getText().endsWith("." + ext)){ //$NON-NLS-1$
			int lastIndex = txtOutputFile.getText().lastIndexOf("."); //$NON-NLS-1$
			if (lastIndex <= 0){
				txtOutputFile.setText(txtOutputFile.getText() + "." + ext); //$NON-NLS-1$
			}else{
				txtOutputFile.setText(txtOutputFile.getText().substring(0, lastIndex+1) + ext);
			}
		}
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}

	@Override
	public boolean isResizable() {
		return true;
	}
	
	public Format getFormat() {
		return (Format) ((StructuredSelection)cmbFormat.getSelection()).getFirstElement();
	}

	public String[] getFileOptions(boolean extensions) {
		switch (getFormat()) {
		case CSV:
			if (extensions) return new String[] { "*.csv", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$
			return new String[] { Messages.ExportEntityXmlDialog_CSVFileType, Messages.ExportEntityXmlDialog_AllFiles };
		case XML:
			if (extensions) return new String[] { "*.zip", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$
			return new String[] { Messages.ExportEntityXmlDialog_ZipFiles, Messages.ExportEntityXmlDialog_AllFiles };
		}
		return new String[] {};
	}
}
