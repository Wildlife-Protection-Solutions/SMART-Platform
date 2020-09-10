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
package org.wcs.smart.asset.ui.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.inout.AssetModelDataToXml;
import org.wcs.smart.asset.data.inout.AssetXmlToAssetData;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetType;
import org.wcs.smart.asset.ui.AssetTypeLabelProvider;
import org.wcs.smart.asset.ui.AttributeLabelProvider;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ConservationAreaLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for selecting which asset model data to export to xml file
 * or another conservation area.
 * 
 * @author Emily
 *
 */
public class ExportModelToXmlDialog extends SmartStyledTitleDialog {

	public static final String PREFERENCE_DIR_KEY = ExportModelToXmlDialog.class.getCanonicalName() + ".dir"; //$NON-NLS-1$

	public static final String EXPORT_DIALOG_TITLE = Messages.ExportModelToXmlDialog_DialogTitle;

	private Font boldFont;

	private CheckboxTableViewer lstAttributes;
	private CheckboxTableViewer lstAssetTypes;
	private CheckboxTableViewer lstMetadataMappings;
	
	private ComboViewer cmbCa;
	
	private Button btnOpFile, btnOpCa;
	private Button btnIncludeModuleSettings, btnIncludeStationAttributes, btnIncludeLocationAttributes;
	
	private Text txtOutputFile;

	public ExportModelToXmlDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	public void cancelPressed() {
		super.cancelPressed();
	}

	@Override
	public void okPressed() {

		List<UUID> attributes = getSelected(AssetAttribute.class, lstAttributes).stream().map(x -> x.getUuid())
				.collect(Collectors.toList());
		List<UUID> assetTypes = getSelected(AssetType.class, lstAssetTypes).stream().map(x -> x.getUuid())
				.collect(Collectors.toList());
		List<UUID> metadataMappings = getSelected(AssetMetadataMapping.class, lstMetadataMappings).stream().map(x -> x.getUuid())
				.collect(Collectors.toList());
		boolean includeModuleSettings = btnIncludeModuleSettings.getSelection();
		boolean includeStationAttributes = btnIncludeStationAttributes.getSelection();
		boolean includeStationLocationAttributes = btnIncludeLocationAttributes.getSelection();

		// check for data
		if (attributes.isEmpty() && assetTypes.isEmpty() && metadataMappings.isEmpty()  
				&& !includeModuleSettings && !includeStationAttributes && !includeStationLocationAttributes) {
			MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE,
					Messages.ExportModelToXmlDialog_NothingToExportMsg);
			return;
		}

		ConservationArea toCa = null;
		try {
			// configure output file
			Path outputFile = null;
			if (btnOpFile.getSelection()) {
				String name = txtOutputFile.getText();
				if (name.isEmpty()) {
					MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportModelToXmlDialog_FileRequired);
					return;
				}
				if (!name.endsWith(".xml")) { //$NON-NLS-1$
					name = name + ".xml"; //$NON-NLS-1$
				}
				outputFile = Paths.get(name).toAbsolutePath();
				if (Files.isDirectory(outputFile)) {
					MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE,Messages.ExportModelToXmlDialog_InvalidFile);
					return;
				}
				AssetPlugIn.getDefault().getPreferenceStore().setValue(PREFERENCE_DIR_KEY, outputFile.toString());

				if (!Files.exists(outputFile.getParent())) {
					Files.createDirectories(outputFile.getParent());
				}

				if (Files.exists(outputFile)) {
					if (!MessageDialog.openQuestion(getShell(), EXPORT_DIALOG_TITLE, MessageFormat
							.format(Messages.ExportModelToXmlDialog_Overwrite, outputFile.toString()))) {
						return;
					}
				}
			} else if (btnOpCa.getSelection()) {
				Object x = ((IStructuredSelection) cmbCa.getSelection()).getFirstElement();
				if (x != null && x instanceof ConservationArea) {
					toCa = (ConservationArea) x;
				}
				if (toCa == null) {
					MessageDialog.openInformation(getShell(), EXPORT_DIALOG_TITLE, Messages.ExportModelToXmlDialog_CaRequired);
					return;
				}
				outputFile = Files.createTempFile("smartasset." + System.nanoTime(), ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
				
			}else {
				//not output option selected
				return;
			}

			final String filename = outputFile.toString();
			final Path outFile = outputFile;
			final ConservationArea exportCa = toCa;

			final boolean[] error = new boolean[] {false};
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
			pmd.run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor progress = SubMonitor
							.convert(monitor,
									MessageFormat.format(Messages.ExportModelToXmlDialog_SubTaskName, exportCa == null ? outFile.getFileName().toString() : exportCa.getNameLabel()),
									exportCa == null ? 1 : 2);
					
					
					try {
						AssetModelDataToXml hh = new AssetModelDataToXml();
						hh.export(outFile, attributes, assetTypes,metadataMappings, includeModuleSettings, includeStationAttributes, includeStationLocationAttributes, progress.split(1));
					
						if (exportCa == null) {
							// export to file complete
							getShell().getDisplay().syncExec(() -> MessageDialog.openInformation(getShell(),
									EXPORT_DIALOG_TITLE,
									MessageFormat.format(Messages.ExportModelToXmlDialog_Exportcomplete, filename)));
						
						} else {
							// import data
							try {
								AssetXmlToAssetData importer = new AssetXmlToAssetData(exportCa);
								importer.importXmlData(outFile, progress.split(1));
							}finally {
								try {
									Files.deleteIfExists(outFile);
								} catch (IOException e) {
									AssetPlugIn.log(e.getMessage(),e);
								}
							}
						}
					
					} catch (OperationCanceledException ex) {
						getShell().getDisplay().syncExec(() -> MessageDialog.openInformation(getShell(),
								EXPORT_DIALOG_TITLE, Messages.ExportModelToXmlDialog_ExportCanceled));
					} catch (Exception e) {
						AssetPlugIn.displayLog(Messages.ExportModelToXmlDialog_ExportAssetError + e.getMessage(),
								e);
					}
				}
			});
			if (error[0]) return;
		} catch (Exception e) {
			AssetPlugIn.displayLog(Messages.ExportModelToXmlDialog_ExportModelError + e.getMessage(), e);
			return;
		}
		
		
		super.okPressed();
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getSelected(Class<T> c, CheckboxTableViewer viewer) {
		List<T> items = new ArrayList<>();
		for (Object x : viewer.getCheckedElements())
			if (x.getClass().equals(c))
				items.add((T) x);
		return items;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);

		FontData fd = parent.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(parent.getDisplay(), fd);
		parent.addListener(SWT.Dispose, e -> boldFont.dispose());

		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createHeader(outer, Messages.ExportModelToXmlDialog_ExportToLabel);

		Composite fileSection = new Composite(outer, SWT.NONE);
		fileSection.setLayout(new GridLayout(3, false));
		fileSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnOpFile = new Button(fileSection, SWT.RADIO);
		btnOpFile.setText(Messages.ExportModelToXmlDialog_FileLabel);

		txtOutputFile = new Text(fileSection, SWT.BORDER);
		txtOutputFile.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String initDir = AssetPlugIn.getDefault().getPreferenceStore().getString(PREFERENCE_DIR_KEY);
		if (initDir != null && initDir.length() != 0) {
			txtOutputFile.setText(initDir);
		} else {
			txtOutputFile.setText(System.getProperty("user.home") + File.separator + "assetmodel.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Button btnBrowse = new Button(fileSection, SWT.NONE);
		btnBrowse.setText("..."); //$NON-NLS-1$
		btnBrowse.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData) btnBrowse.getLayoutData()).heightHint = txtOutputFile.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

		btnBrowse.addListener(SWT.Selection, e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterExtensions(new String[] { "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
			dialog.setFilterNames(new String[] { Messages.ExportModelToXmlDialog_XMLFile, Messages.ExportModelToXmlDialog_AllFiles});
			String t = txtOutputFile.getText();
			if (t.length() > 0) {
				dialog.setFileName(t);
			}
			dialog.setText(Messages.ExportModelToXmlDialog_FileExportTitle);
			String file = dialog.open();
			if (file != null)
				txtOutputFile.setText(file);
		});

		btnOpCa = new Button(fileSection, SWT.RADIO);
		btnOpCa.setText(Messages.ExportModelToXmlDialog_CaOption);

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

		// asset attributes
		Composite attributeSection = new Composite(itemsSection, SWT.NONE);
		attributeSection.setLayout(new GridLayout());
		((GridLayout) attributeSection.getLayout()).marginWidth = 0;
		((GridLayout) attributeSection.getLayout()).marginHeight = 0;
		attributeSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(attributeSection, Messages.ExportModelToXmlDialog_AttributesLabel);
		lstAttributes = createTableViewer(attributeSection, new AttributeLabelProvider(), null);
				
		// asset types
		Composite assetTypeSection = new Composite(itemsSection, SWT.NONE);
		assetTypeSection.setLayout(new GridLayout());
		((GridLayout) assetTypeSection.getLayout()).marginWidth = 0;
		((GridLayout) assetTypeSection.getLayout()).marginHeight = 0;
		assetTypeSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(assetTypeSection, Messages.ExportModelToXmlDialog_AssetTypesLabel);
		lstAssetTypes = createTableViewer(assetTypeSection, new AssetTypeLabelProvider(), null);

		// metadata mappings
		Composite metadataSection = new Composite(itemsSection, SWT.NONE);
		metadataSection.setLayout(new GridLayout());
		((GridLayout) metadataSection.getLayout()).marginWidth = 0;
		((GridLayout) metadataSection.getLayout()).marginHeight = 0;
		metadataSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(metadataSection, Messages.ExportModelToXmlDialog_MetadataMappingsLabel);
		lstMetadataMappings = createTableViewer(metadataSection, new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMetadataMapping) {
					AssetMetadataMapping mm = (AssetMetadataMapping)element;
					StringBuilder sb = new StringBuilder();
					sb.append(mm.getMetadataType().name());
					sb.append(" ("); //$NON-NLS-1$
					sb.append(mm.getMetadataKey());
					sb.append(" ) "); //$NON-NLS-1$
					if (mm.getMappedAssetProperty() != null) sb.append(mm.getMappedAssetProperty().name());
					if (mm.getMappedCategory() != null) {
						sb.append(mm.getMappedCategory().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					if (mm.getMappedAttribute() != null) {
						sb.append(mm.getMappedAttribute().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					if (mm.getMappedListItem() != null) {
						sb.append(mm.getMappedListItem().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					if (mm.getMappedTreeNode() != null) {
						sb.append(mm.getMappedTreeNode().getName());
						sb.append(" "); //$NON-NLS-1$
					}
					return sb.toString();
				}
				return super.getText(element);
			}
		}, null);

		//configuration section
		Composite configSection = new Composite(itemsSection, SWT.NONE);
		configSection.setLayout(new GridLayout());
		((GridLayout) configSection.getLayout()).marginWidth = 0;
		((GridLayout) configSection.getLayout()).marginHeight = 0;
		configSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createHeader(configSection, Messages.ExportModelToXmlDialog_ConfigurationLabel);
		
		btnIncludeModuleSettings= new Button(configSection, SWT.CHECK);
		btnIncludeModuleSettings.setText(Messages.ExportModelToXmlDialog_IncludeSettiongsOp);
		btnIncludeModuleSettings.setToolTipText(Messages.ExportModelToXmlDialog_settingstooltip);
		btnIncludeModuleSettings.setSelection(true);
		
		btnIncludeStationAttributes= new Button(configSection, SWT.CHECK);
		btnIncludeStationAttributes.setText(Messages.ExportModelToXmlDialog_IncludeStationsOption);
		btnIncludeStationAttributes.setToolTipText(Messages.ExportModelToXmlDialog_IncludeStationsTooltips);
		btnIncludeStationAttributes.setSelection(true);
		
		btnIncludeLocationAttributes= new Button(configSection, SWT.CHECK);
		btnIncludeLocationAttributes.setText(Messages.ExportModelToXmlDialog_IncludeLocations);
		btnIncludeLocationAttributes.setToolTipText(Messages.ExportModelToXmlDialog_IncludeLocationsTooltip);
		btnIncludeLocationAttributes.setSelection(true);
		
		// load data
		initializeLists.schedule();

		setTitle(Messages.ExportModelToXmlDialog_Title);
		setMessage(Messages.ExportModelToXmlDialog_Message);
		getShell().setText(Messages.ExportModelToXmlDialog_Title);

		return parent;
	}

	private CheckboxTableViewer createTableViewer(Composite parent, ILabelProvider lblProvider,
			ICheckStateListener listener) {

		Composite action = new Composite(parent, SWT.NONE);
		action.setLayout(new GridLayout(2, false));
		((GridLayout) action.getLayout()).marginWidth = 0;
		((GridLayout) action.getLayout()).marginHeight = 0;

		Link selectAll = new Link(action, SWT.NONE);
		selectAll.setText("<a>" + Messages.ExportModelToXmlDialog_AllOption + "</a>");  //$NON-NLS-1$ //$NON-NLS-2$

		Link selectNone = new Link(action, SWT.NONE);
		selectNone.setText("<a>" + Messages.ExportModelToXmlDialog_NoneOption + "</a>");  //$NON-NLS-1$ //$NON-NLS-2$
		selectNone.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

		CheckboxTableViewer viewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData) viewer.getControl().getLayoutData()).heightHint = 50;
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		viewer.setLabelProvider(lblProvider);
		viewer.setInput(new String[] { DialogConstants.LOADING_TEXT });

		selectAll.addListener(SWT.Selection, e -> {
			viewer.setAllChecked(true);
			if (listener != null)
				listener.checkStateChanged(null);
		});
		selectNone.addListener(SWT.Selection, e -> {
			viewer.setAllChecked(false);
			if (listener != null)
				listener.checkStateChanged(null);
		});

		return viewer;
	}

	private void createHeader(Composite parent, String text) {
		Composite header = new Composite(parent, SWT.NONE);
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.setLayout(new GridLayout(2, false));
		((GridLayout) header.getLayout()).marginWidth = 0;
		((GridLayout) header.getLayout()).marginHeight = 0;

		Label l = new Label(header, SWT.NONE);
		l.setText(text);
		l.setFont(boldFont);

		Label sep = new Label(header, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	@Override
	public void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}

	@Override
	public boolean isResizable() {
		return true;
	}

	Job initializeLists = new Job(Messages.ExportModelToXmlDialog_initJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<AssetAttribute> attributes;
			List<AssetType> assetTypes;
			List<AssetMetadataMapping> mappings;
			
			List<ConservationArea> cas;

			try (Session s = HibernateManager.openSession()) {
				attributes = QueryFactory
						.buildQuery(s, AssetAttribute.class, "conservationArea", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
				attributes.forEach(e -> e.getName());
				
				assetTypes = QueryFactory
						.buildQuery(s, AssetType.class, "conservationArea", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
				assetTypes.forEach(e -> e.getName());

				mappings = QueryFactory
						.buildQuery(s, AssetMetadataMapping.class, "conservationArea", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.list();
				mappings.forEach(e -> {
					e.getMetadataKey();
					if (e.getMappedAttribute() != null) e.getMappedAttribute().getName();
					if (e.getMappedCategory() != null) e.getMappedCategory().getName();
					if (e.getMappedListItem() != null) e.getMappedListItem().getName();
					if (e.getMappedTreeNode() != null) e.getMappedTreeNode().getName();
				});
				
				cas = QueryFactory.buildQuery(s, ConservationArea.class).list();
				for (Iterator<ConservationArea> iterator = cas.iterator(); iterator.hasNext();) {
					ConservationArea conservationArea = iterator.next();
					if (conservationArea.getIsCcaa())
						iterator.remove();
				}
				cas.remove(SmartDB.getCurrentConservationArea());
				ConservationArea ccaa = null;
				for (ConservationArea c : cas) {
					if (c.getIsCcaa())
						ccaa = c;
				}
				cas.remove(ccaa);
				cas.forEach(a -> a.getName());
			}

			Display.getDefault().syncExec(() -> {
				lstAttributes.setInput(attributes);
				lstAssetTypes.setInput(assetTypes);
				lstMetadataMappings.setInput(mappings);
				cmbCa.setInput(cas);
				if (!cas.isEmpty()) cmbCa.setSelection(new StructuredSelection(cas.get(0)));
				
				lstAttributes.setAllChecked(true);
				lstAssetTypes.setAllChecked(true);
				lstMetadataMappings.setAllChecked(true);
			});

			return Status.OK_STATUS;
		}

	};
}
