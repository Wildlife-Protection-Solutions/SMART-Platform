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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRelationshipType;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.RecordSourceLabelProvider;
import org.wcs.smart.i2.ui.RelationshipTypeLabelProvider;
import org.wcs.smart.i2.xml.IntelDataToXml;
import org.wcs.smart.i2.xml.XmlToIntelData;
import org.wcs.smart.ui.ConservationAreaLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for exporting intelligence model elements to file or 
 * another Conservation Area 
 * 
 * @author Emily
 *
 */
public class ExportModelElementsDialog extends TitleAreaDialog{

	public static final String PREFERENCE_DIR_KEY = ExportModelElementsDialog.class.getCanonicalName() + ".dir"; //$NON-NLS-1$
	
	public static final String EXPORT_DIALOG_TITLE = Messages.ExportModelElementsDialog_DialogTitle;
	
	private Font boldFont;
	
	private CheckboxTableViewer lstEntities;
	private CheckboxTableViewer lstRelations;
	private CheckboxTableViewer lstAttributes;
	private CheckboxTableViewer lstRecordSource;
	private ComboViewer cmbCa;
	private Button btnSrcTemplate, btnOpFile, btnOpCa;
	private Text txtOutputFile;
	
	public ExportModelElementsDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void cancelPressed(){
		super.cancelPressed();
	}
	
	@Override
	public void okPressed(){
		
		List<UUID> sources = getSelected(IntelRecordSource.class, lstRecordSource).stream().map(x->x.getUuid()).collect(Collectors.toList());
		List<UUID> entities = getSelected(IntelEntityType.class, lstEntities).stream().map(x->x.getUuid()).collect(Collectors.toList());
		List<UUID> relationships = getSelected(IntelRelationshipType.class, lstRelations).stream().map(x->x.getUuid()).collect(Collectors.toList());
		List<UUID> attributes = getSelected(IntelAttribute.class, lstAttributes).stream().map(x->x.getUuid()).collect(Collectors.toList());
		boolean includeRecordSrcTemplate = btnSrcTemplate.getSelection();
		
		//check for data
		if (sources.isEmpty() && entities.isEmpty() && relationships.isEmpty() && attributes.isEmpty() && !includeRecordSrcTemplate) {
			MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportModelElementsDialog_NothingToExport);
			return;
		}
		
		ConservationArea toCa = null;
		try {
			//configure output file
			Path outputFile = null;
			if (btnOpFile.getSelection()) {
				String name = txtOutputFile.getText();
				if (name.isEmpty()) {
					MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportModelElementsDialog_InvalidXmlFile);
					return;
				}
				if (!name.endsWith(".zip")) { //$NON-NLS-1$
					name = name + ".zip"; //$NON-NLS-1$
				}
				outputFile = Paths.get(name).toAbsolutePath();
				if (Files.isDirectory(outputFile)) {
					MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportModelElementsDialog_InvalidXmlFile);
					return;
				}	
				Intelligence2PlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_DIR_KEY, outputFile.toString());
	
				if (!Files.exists(outputFile.getParent())) {
					Files.createDirectories(outputFile.getParent());
				}
				
				if (Files.exists(outputFile)) {
					if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOG_TITLE, 
							MessageFormat.format(Messages.ExportModelElementsDialog_FileExists, outputFile.toString()))){
						return;
					}
				}
			}else if (btnOpCa.getSelection()) {
				Object x = ((IStructuredSelection)cmbCa.getSelection()).getFirstElement();
				if (x != null && x instanceof ConservationArea) {
					toCa = (ConservationArea)x;
				}
				if (toCa == null) {
					MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportModelElementsDialog_NoCaSelected);
					return;
				}
				outputFile = Files.createTempFile("smartintel." + System.nanoTime(), ".zip");  //$NON-NLS-1$ //$NON-NLS-2$
			}

			final String filename = outputFile.toString();
			final Path outFile = outputFile;
			final ConservationArea exportCa = toCa;
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor progress =  SubMonitor.convert(monitor, MessageFormat.format(Messages.ExportModelElementsDialog_TaskName, exportCa == null ? Messages.ExportModelElementsDialog_exportToFile : Messages.ExportModelElementsDialog_exportToCa), exportCa == null ? 1 : 2);
					try(Session s = HibernateManager.openSession()){
						IntelDataToXml hh = new IntelDataToXml(s);
						hh.export(outFile, attributes, sources, relationships, entities, includeRecordSrcTemplate, progress.split(1));
						if (exportCa == null) {
							//export to file complete
							getShell().getDisplay().syncExec(()->MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, MessageFormat.format(Messages.ExportModelElementsDialog_ExportComplete, filename)));
						}else {
							//import data
							XmlToIntelData importer = new XmlToIntelData(exportCa);
							importer.importXmlData(outFile, progress.split(1));
						}
					} catch (OperationCanceledException ex) {
						getShell().getDisplay().syncExec(()->MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportModelElementsDialog_ExportCanceled));
					} catch (Exception e) {
						Intelligence2PlugIn.displayLog(Messages.ExportModelElementsDialog_ExportError + e.getMessage(), e);
					}
				}
			});
		} catch (Exception e) {
			Intelligence2PlugIn.displayLog(Messages.ExportModelElementsDialog_ExportError + e.getMessage(), e);
		}
		super.okPressed();
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getSelected(Class<T> c, CheckboxTableViewer viewer){
		List<T> items = new ArrayList<>();
		for (Object x : viewer.getCheckedElements()) if (x.getClass().equals(c)) items.add((T)x);
		return items;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		parent.addListener(SWT.Dispose, e->boldFont.dispose());
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createHeader(outer, Messages.ExportModelElementsDialog_ExportToSection);
		
		Composite fileSection = new Composite(outer, SWT.NONE);
		fileSection.setLayout(new GridLayout(3, false));
		fileSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnOpFile = new Button(fileSection, SWT.RADIO);
		btnOpFile.setText(Messages.ExportModelElementsDialog_FileLabel);
		
		txtOutputFile = new Text(fileSection, SWT.BORDER);
		txtOutputFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String initDir = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
		if (initDir != null && initDir.length() != 0) {
			txtOutputFile.setText(initDir);
		}else {
			txtOutputFile.setText(System.getProperty("user.home") + File.separator + "intelligencemodel.zip"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Button btnBrowse = new Button(fileSection, SWT.NONE);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData)btnBrowse.getLayoutData()).heightHint = txtOutputFile.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

		btnBrowse.addListener(SWT.Selection, e->{
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterExtensions(new String[] {"*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
			dialog.setFilterNames(new String[] {Messages.ExportModelElementsDialog_zipFile, Messages.ExportModelElementsDialog_allFiles});
			String t = txtOutputFile.getText();
			if (t.length() > 0) {
				dialog.setFileName(t);
			}
			dialog.setText(Messages.ExportModelElementsDialog_FileDialogExportTitle);
			String file = dialog.open();
			if (file != null) txtOutputFile.setText(file);
		});
		
		btnOpCa = new Button(fileSection, SWT.RADIO);
		btnOpCa.setText(Messages.ExportModelElementsDialog_CaLabel);
		
		cmbCa = new ComboViewer(fileSection, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.DROP_DOWN);
		cmbCa.setContentProvider(ArrayContentProvider.getInstance());
		cmbCa.setLabelProvider(new ConservationAreaLabelProvider());
		cmbCa.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Listener enabledListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				cmbCa.getControl().setEnabled(btnOpCa.getSelection());
				
				btnBrowse.setEnabled(btnOpFile.getSelection());
				txtOutputFile.setEnabled(btnOpFile.getSelection());
			}
		};
		btnOpCa.setSelection(false);
		btnOpFile.setSelection(true);
		btnOpCa.addListener(SWT.Selection, enabledListener);
		btnOpFile.addListener(SWT.Selection, enabledListener);
		enabledListener.handleEvent(null);
		
		Composite itemsSection = new Composite(outer, SWT.NONE);
		itemsSection.setLayout(new GridLayout());
		itemsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//entity section
		Composite entitySection = new Composite(itemsSection, SWT.NONE);
		entitySection.setLayout(new GridLayout());
		((GridLayout)entitySection.getLayout()).marginWidth = 0;
		((GridLayout)entitySection.getLayout()).marginHeight = 0;
		entitySection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(entitySection, Messages.ExportModelElementsDialog_EntityTypesSection);
		lstEntities = createTableViewer(entitySection, new EntityTypeLabelProvider(), null);

		//relations section
		Composite relationsSection = new Composite(itemsSection, SWT.NONE);
		relationsSection.setLayout(new GridLayout());
		((GridLayout)relationsSection.getLayout()).marginWidth = 0;
		((GridLayout)relationsSection.getLayout()).marginHeight = 0;
		relationsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(relationsSection, Messages.ExportModelElementsDialog_RelationshipTypesSection);
		lstRelations = createTableViewer(relationsSection, new RelationshipTypeLabelProvider(), null);
		
		//source section
		Composite sourceSection = new Composite(itemsSection, SWT.NONE);
		sourceSection.setLayout(new GridLayout());
		((GridLayout)sourceSection.getLayout()).marginWidth = 0;
		((GridLayout)sourceSection.getLayout()).marginHeight = 0;
		sourceSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(sourceSection, Messages.ExportModelElementsDialog_RecordSourcesSection);
		ICheckStateListener listener = new ICheckStateListener() {			
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				btnSrcTemplate.setSelection(lstRecordSource.getCheckedElements().length != 0);
			}
		};
		lstRecordSource = createTableViewer(sourceSection, new RecordSourceLabelProvider(), listener);
		btnSrcTemplate = new Button(sourceSection, SWT.CHECK);
		btnSrcTemplate.setText(Messages.ExportModelElementsDialog_IncludeRecordSourceTemplate);
		lstRecordSource.addCheckStateListener(listener);
		
		//attribute section
		Composite attributeSection = new Composite(itemsSection, SWT.NONE);
		attributeSection.setLayout(new GridLayout());
		((GridLayout)attributeSection.getLayout()).marginWidth = 0;
		((GridLayout)attributeSection.getLayout()).marginHeight = 0;
		attributeSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(attributeSection, Messages.ExportModelElementsDialog_AttributesSection);
		Label l = new Label(attributeSection, SWT.WRAP);
		l.setText(Messages.ExportModelElementsDialog_AttributeInfo);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)l.getLayoutData()).widthHint = 100;
		lstAttributes = createTableViewer(attributeSection, new AttributeLabelProvider(), null);
				
		//load data
		initializeLists.schedule();
		
		setTitle(Messages.ExportModelElementsDialog_Title);
		setMessage(Messages.ExportModelElementsDialog_Message);
		getShell().setText(Messages.ExportModelElementsDialog_ShellTitle);

		return parent;
	}
	
	private CheckboxTableViewer createTableViewer(Composite parent, ILabelProvider lblProvider, ICheckStateListener listener) {
	
		Composite action = new Composite(parent, SWT.NONE);
		action.setLayout(new GridLayout(2, false));
		((GridLayout)action.getLayout()).marginWidth = 0;
		((GridLayout)action.getLayout()).marginHeight = 0;
		
		Link selectAll = new Link(action, SWT.NONE);
		selectAll.setText("<a>" + Messages.ExportModelElementsDialog_SelectAll + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		
		Link selectNone = new Link(action, SWT.NONE);
		selectNone.setText("<a>" + Messages.ExportModelElementsDialog_SelectNone + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		selectNone.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		
		CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)viewer.getControl().getLayoutData()).heightHint = 50;
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(lblProvider);
		viewer.setInput(new String[] { DialogConstants.LOADING_TEXT });
		
		selectAll.addListener(SWT.Selection, e->{viewer.setAllChecked(true); if (listener != null) listener.checkStateChanged(null);});
		selectNone.addListener(SWT.Selection, e->{viewer.setAllChecked(false); if (listener != null) listener.checkStateChanged(null);});
		
		return viewer;
	}
	
	
	
	private void createHeader(Composite parent, String text) {
		Composite header = new Composite(parent, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		Label l = new Label(header, SWT.NONE);
		l.setText(text);
		l.setFont(boldFont);
		
		Label sep = new Label(header, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}
	
	
	@Override
	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	Job initializeLists = new Job(Messages.ExportModelElementsDialog_40) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelEntityType> entityTypes;
			List<IntelRelationshipType> relationshipTypes;
			List<IntelRecordSource> sources;
			List<IntelAttribute> attributes;
			List<ConservationArea> cas;
			
			try (Session s = HibernateManager.openSession()){
				entityTypes = QueryFactory.buildQuery(s, IntelEntityType.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
				entityTypes.forEach(e->e.getName());
				
				relationshipTypes = QueryFactory.buildQuery(s, IntelRelationshipType.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
				relationshipTypes.forEach(r->{
					r.getName();
					if (r.getRelationshipGroup() != null) r.getRelationshipGroup().getName();
					
				});
			
				attributes = QueryFactory.buildQuery(s,  IntelAttribute.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
				attributes.forEach(a->a.getName());
				
				sources = QueryFactory.buildQuery(s,  IntelRecordSource.class, "conservationArea", SmartDB.getCurrentConservationArea()).list(); //$NON-NLS-1$
				sources.forEach(a->a.getName());
				
				cas = QueryFactory.buildQuery(s,  ConservationArea.class).list();
				cas.remove(SmartDB.getCurrentConservationArea());
				ConservationArea ccaa = null;
				for (ConservationArea c : cas) {
					if (c.getIsCcaa()) ccaa = c;
				}
				cas.remove(ccaa);
				cas.forEach(a->a.getName());
			}
			
			Display.getDefault().syncExec(()->{
				lstEntities.setInput(entityTypes);
				lstRelations.setInput(relationshipTypes);
				lstAttributes.setInput(attributes);
				lstRecordSource.setInput(sources);
				cmbCa.setInput(cas);
				if (!cas.isEmpty()) cmbCa.setSelection(new StructuredSelection(cas.get(0)));
			});
			
			return Status.OK_STATUS;
		}
		
	};
}
