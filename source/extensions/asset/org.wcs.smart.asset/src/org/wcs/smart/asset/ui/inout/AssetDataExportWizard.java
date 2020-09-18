/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.inout;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.inout.AssetCsvExporter;
import org.wcs.smart.asset.data.inout.AssetCsvImporter;
import org.wcs.smart.asset.data.inout.AssetLocationCsvExporter;
import org.wcs.smart.asset.data.inout.AssetLocationCsvImporter;
import org.wcs.smart.asset.data.inout.AssetModelDataToXml;
import org.wcs.smart.asset.data.inout.AssetStationCsvExporter;
import org.wcs.smart.asset.data.inout.AssetStationCsvImporter;
import org.wcs.smart.asset.data.inout.AssetXmlToAssetData;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.export.config.ICsvDataExporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard for exporting asset data to files or conservation areas
 * 
 * @author Emily
 *
 */
public class AssetDataExportWizard extends Wizard{

	@Inject
	private IEclipseContext context;
	
	public enum Type{
		
		XML(Messages.AssetDataExportWizard_ModelElementsOp),
		ASSET_CSV(Messages.AssetDataExportWizard_SensorsOp),
		STATION_CSV(Messages.AssetDataExportWizard_StationOp),
		LOCATION_CSV(Messages.AssetDataExportWizard_LocationsOp);
		
		String guiName;
		
		Type(String guiName) {
			this.guiName = guiName;
		}
		
		public boolean isCsv() {
			return this == ASSET_CSV || this == STATION_CSV || this == LOCATION_CSV;
		}
	
	}
	
	ExportTypePage typePage = null;
	ExportLocationWizardPage locationPage = null;
	ModelExportOptionsWizardPage xmlOpPage = null;
	CsvOptionsWizardPage csvOpPage = null;
	
	public AssetDataExportWizard() {
		super();
		setWindowTitle(Messages.AssetDataExportWizard_WizardTitle);
	}
	
	@Override
	public boolean canFinish() {
		if (getContainer().getCurrentPage() == null) return false;
		if (!getContainer().getCurrentPage().isPageComplete()) return false;

		IWizardPage current = getContainer().getCurrentPage();
		
		if (current == csvOpPage && !typePage.getTypes().contains(Type.XML)) return true;
		if (current == locationPage && locationPage.getConservationArea() != null && !typePage.getTypes().contains(Type.XML)) return true;
		if (current == xmlOpPage) return true;
		
		return false;
	}
	
	@Override
	public boolean performFinish() {
		
		String location = locationPage.getDirectory();
		Path exportDir = null;
		boolean delete = false;
		ConservationArea lca = null;
		
		if (location != null) {
			exportDir= Paths.get(location);
			SmartUtils.createDirectory(exportDir);
			
			AssetPlugIn.getDefault().getPreferenceStore().setValue(ExportLocationWizardPage.MODELEXPORT_DIR_KEY, location);

		}else {
			lca = locationPage.getConservationArea();
			delete = true;
			try {
				exportDir = Files.createTempDirectory("smart_assetexport"); //$NON-NLS-1$
			}catch (IOException ex) {
				AssetPlugIn.displayLog(ex.getMessage(), ex);
				return false;
			}
		}
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		List<String> ok = new ArrayList<>();

		final Path fexportDir = exportDir;
		final ConservationArea toCa = lca;
		
		

		try {
			final Collection<Type> types = typePage.getTypes();
			final char delim = csvOpPage.getDelimiter();
			Charset cs = csvOpPage.getCharSet();
			
			
			List<UUID> attributes = xmlOpPage.getSelectedAttributes().stream().map(x -> x.getUuid())
					.collect(Collectors.toList());
			List<UUID> assetTypes = xmlOpPage.getSelectedAssetTypes().stream().map(x -> x.getUuid())
					.collect(Collectors.toList());
			List<UUID> metadataMappings = xmlOpPage.getSelectedMetadata().stream().map(x -> x.getUuid())
					.collect(Collectors.toList());
			boolean includeModuleSettings = xmlOpPage.getIncludeModuleSettings();
			boolean includeStationAttributes = xmlOpPage.getIncludeStationAttributes();
			boolean includeStationLocationAttributes = xmlOpPage.getIncludeStationAttributes();


			pmd.run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					SubMonitor sub = SubMonitor.convert(monitor);

					sub.beginTask(Messages.AssetDataExportWizard_Task1, types.size());

					try (Session session = HibernateManager.openSession()) {
						// do xml first so objects can be imported correctly
						if (types.contains(Type.XML)) {
							Path file = fexportDir.resolve("assetmodel.xml"); //$NON-NLS-1$

							if (exportModel(file, toCa, attributes, assetTypes, metadataMappings, includeModuleSettings,
									includeStationAttributes, includeStationLocationAttributes, sub.split(1))) {

								String out = file.toAbsolutePath().normalize().toString();
								if (toCa != null) out = toCa.getId();
								ok.add(MessageFormat.format(Messages.AssetDataExportWizard_AssetModelExportOk, out));

							}
						}
						if (types.contains(Type.STATION_CSV)) {
							Path file = fexportDir.resolve("stations.csv"); //$NON-NLS-1$
							AssetStationCsvExporter sexporter = new AssetStationCsvExporter();

							if (exportCsv(sexporter, file, Messages.AssetDataExportWizard_StationsLabel, delim, cs, session, sub.split(1))) {
								if (toCa == null) {
									ok.add(MessageFormat.format(Messages.AssetDataExportWizard_StationFileMsg,
											file.toAbsolutePath().normalize().toString()));
								} else {
									HashMap<AssetAttribute,Integer> am = mapAttributes(sexporter.getAttribute2ColumnMapping(), toCa, session);
									if (am != null) {
										if (importStations(file, toCa, am, delim)) {
											ok.add(MessageFormat.format(Messages.AssetDataExportWizard_StationCaMsg, toCa.getId()));
										}
									}
								}
							}
						}
						if (types.contains(Type.LOCATION_CSV)) {
							Path file = fexportDir.resolve("locations.csv"); //$NON-NLS-1$
							AssetLocationCsvExporter lexporter = new AssetLocationCsvExporter();

							if (exportCsv(lexporter, file, Messages.AssetDataExportWizard_LocationsLabel, delim, cs, session, sub.split(1))) {
								if (toCa == null) {
									ok.add(MessageFormat.format(Messages.AssetDataExportWizard_LocationFileMsg,
											file.toAbsolutePath().normalize().toString()));
								} else {
									HashMap<AssetAttribute,Integer> am = mapAttributes(lexporter.getAttribute2ColumnMapping(), toCa, session);
									if (am != null) {
										if (importLocations(file, toCa, am, delim)) {
											ok.add(MessageFormat.format(Messages.AssetDataExportWizard_LocationsCaMsg, toCa.getId()));
										}
									}
								}
							}

						}

						if (types.contains(Type.ASSET_CSV)) {
							Path file = fexportDir.resolve("fieldsensors.csv"); //$NON-NLS-1$
							AssetCsvExporter exporter = new AssetCsvExporter();
							if (exportCsv(exporter, file, Messages.AssetDataExportWizard_AssetsLabel, delim, cs, session, sub.split(1))) {

								if (toCa == null) {
									ok.add(MessageFormat.format(Messages.AssetDataExportWizard_AssetFileMsg,
											file.toAbsolutePath().normalize().toString()));
								} else {
									HashMap<AssetAttribute,Integer> am = mapAttributes(exporter.getAttribute2ColumnMapping(), toCa, session);
									if (am != null) {
										if (importAssets(file, toCa, am, delim)) {
											ok.add(MessageFormat.format(Messages.AssetDataExportWizard_AssetCaMsg, toCa.getId()));
										}
									}
								}
							}
						}
					}
				}
			});
			if (ok.isEmpty()) {
				MessageDialog.openInformation(getShell(), Messages.AssetDataExportWizard_CompleteTitle, Messages.AssetDataExportWizard_NothingExported);
				return false;
			}else {
				StringBuilder sb = new StringBuilder();
				sb.append(Messages.AssetDataExportWizard_CompleteMsg);
				sb.append("\n"); //$NON-NLS-1$
				ok.forEach(w->{sb.append(w); sb.append("\n");}); //$NON-NLS-1$
				
				MessageDialog.openInformation(getShell(), Messages.AssetDataExportWizard_CompleteTitle, sb.toString());
			}
			
		}catch (Exception ex) {
			AssetPlugIn.displayLog(ex.getMessage(), ex);
			return false;
			 
		}finally {
			if (delete) {
				try {
					SmartUtils.deleteDirectory(exportDir);
				}catch (Exception ex) {
					AssetPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		return true;		
	}
	
	private boolean importAssets(Path file, ConservationArea ca,
			HashMap<AssetAttribute,Integer> attributeMappings,
			char delim) {
		try {
		
			AssetCsvImporter importer = new AssetCsvImporter(ca, file, delim, true, 
					AssetCsvExporter.FixedField.ID.ordinal(),  
					AssetCsvExporter.FixedField.TYPE.ordinal(),
					attributeMappings, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) );
			ContextInjectionFactory.inject(importer, context);
			return importer.processFile();
		}catch (Exception ex) {
			AssetPlugIn.displayLog(MessageFormat.format(Messages.AssetDataExportWizard_AssetLoadError,  ca, ex.getMessage()), ex);
			return false;
		}
	}
	
	private HashMap<AssetAttribute,Integer> mapAttributes(HashMap<AssetAttribute, Integer> currentCa, 
			ConservationArea toCa, Session session){
		//map the assetattributes from the currentCa to the new Ca
		HashMap<AssetAttribute,Integer> newmapping = new HashMap<>();
		
		Map<String, AssetAttribute> toAttribute = QueryFactory.buildQuery(session, AssetAttribute.class, 
				new Object[] {"conservationArea", toCa}) //$NON-NLS-1$
				.list().stream().collect(Collectors.toMap(e->e.getKeyId(), e->e));
				
		List<AssetAttribute> notfound = new ArrayList<>();
		for (Entry<AssetAttribute,Integer> e :currentCa.entrySet()) {
			
			if (toAttribute.containsKey(e.getKey().getKeyId())) {
				newmapping.put(toAttribute.get(e.getKey().getKeyId()), e.getValue());
			}else {
				notfound.add(e.getKey());
			}
		}
		
		if (!notfound.isEmpty()) {
			
			StringBuilder sb = new StringBuilder();
			sb.append(MessageFormat.format(Messages.AssetDataExportWizard_AttributesNotFound, toCa.getName()));
			sb.append("\n\n"); //$NON-NLS-1$
			for (AssetAttribute nf : notfound) {
				sb.append(nf.getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			
			final boolean[] ret = new boolean[1];
			Display.getDefault().syncExec(()->{
				ret[0] = MessageDialog.openQuestion(getShell(), Messages.AssetDataExportWizard_ImportTitle, sb.toString());
			});
			if (!ret[0]) return null;
		}
		
		return newmapping;
	}

	private boolean importStations(Path file, ConservationArea ca, 
			HashMap<AssetAttribute,Integer> attributeMappings, char delim) {
		try {		
			Projection p = new Projection();
			p.setConservationArea(ca);
			p.setParsedCoordinateReferenceSystem(SmartDB.DATABASE_CRS);
			p.setDefinition(SmartDB.DATABASE_CRS.toWKT());
			AssetStationCsvImporter importer = new AssetStationCsvImporter(ca, file, delim, true, 
					AssetStationCsvExporter.FixedField.ID.ordinal(),
					AssetStationCsvExporter.FixedField.X.ordinal(),
					AssetStationCsvExporter.FixedField.Y.ordinal(),
					AssetStationCsvExporter.FixedField.BUFFER.ordinal(),
					attributeMappings,DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
					p);
			ContextInjectionFactory.inject(importer, context);
			
			return importer.processFile();
		}catch (Exception ex) {
			AssetPlugIn.displayLog(MessageFormat.format(Messages.AssetDataExportWizard_StationImportError,  ca, ex.getMessage()), ex);
			return false;
		}
	}
	
	private boolean importLocations(Path file, ConservationArea ca, 
			HashMap<AssetAttribute,Integer> attributeMappings, char delim) {
		try {	
			Projection p = new Projection();
			p.setConservationArea(ca);
			p.setParsedCoordinateReferenceSystem(SmartDB.DATABASE_CRS);
			p.setDefinition(SmartDB.DATABASE_CRS.toWKT());

			AssetLocationCsvImporter importer = new AssetLocationCsvImporter(ca, file, delim, true, 
					AssetLocationCsvExporter.FixedField.ID.ordinal(),
					AssetLocationCsvExporter.FixedField.STATION_ID.ordinal(),
					AssetLocationCsvExporter.FixedField.X.ordinal(),
					AssetLocationCsvExporter.FixedField.Y.ordinal(),
					AssetLocationCsvExporter.FixedField.BUFFER.ordinal(),
					attributeMappings,DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM),
					p);
			ContextInjectionFactory.inject(importer, context);
			
			return importer.processFile();
		}catch (Exception ex) {
			AssetPlugIn.displayLog(MessageFormat.format(Messages.AssetDataExportWizard_LocationImportError,  ca, ex.getMessage()), ex);
			return false;
		}
	}
	
	private boolean exportCsv(ICsvDataExporter exporter,
			Path file, String type, char delim, Charset cs,
			Session session, SubMonitor sub) {
		
		sub.beginTask(MessageFormat.format(Messages.AssetDataExportWizard_Task2, type), 1);
		try {
			//TODO: Warn file overwrite?
			return exporter.exportCsvFile(file, delim, SmartDB.getCurrentConservationArea(), 
				true, cs, new NullProgressMonitor(), session);
			
		}catch (Exception ex) {
			AssetPlugIn.displayLog(MessageFormat.format(Messages.AssetDataExportWizard_ExportError, type, file.toString(), ex.getMessage()), ex);
			return false;
		}
		
		
	}

	private boolean exportModel(Path file, ConservationArea toCa, 
			
			List<UUID> attributes, List<UUID> assetTypes, List<UUID> metadataMappings,
			boolean includeModuleSettings,
			boolean includeStationAttributes,
			boolean includeStationLocationAttributes,
			SubMonitor progress) {
				// check for data
		
		if (attributes.isEmpty() && assetTypes.isEmpty() && metadataMappings.isEmpty()  
				&& !includeModuleSettings && !includeStationAttributes && !includeStationLocationAttributes) {
			AssetPlugIn.displayLog(Messages.AssetDataExportWizard_NoElementsToExport,  null);
			return false;
		}

		
		try {	
			AssetModelDataToXml hh = new AssetModelDataToXml();
			hh.export(file, attributes, assetTypes,metadataMappings, includeModuleSettings, includeStationAttributes, includeStationLocationAttributes, progress.split(1));
		}catch (Exception ex) {
			AssetPlugIn.displayLog(MessageFormat.format(Messages.AssetDataExportWizard_ModelExportError,  ex.getMessage()), ex);
			return false;
		}
		
		if (toCa == null) return true;

		// import data
		try {
			AssetXmlToAssetData importer = new AssetXmlToAssetData(toCa);
			importer.importXmlData(file, progress.split(1));
		}catch (Exception ex) {
			AssetPlugIn.displayLog(MessageFormat.format(Messages.AssetDataExportWizard_ModelImportError,  ex.getMessage()), ex);
			return false;
		}
		return true;
		
	}
	
	
	public void addPages() {
		typePage = new ExportTypePage();
		locationPage = new ExportLocationWizardPage();
		csvOpPage = new CsvOptionsWizardPage();
		xmlOpPage = new ModelExportOptionsWizardPage();
		
		ContextInjectionFactory.inject(typePage, context);
		ContextInjectionFactory.inject(locationPage, context);
		ContextInjectionFactory.inject(xmlOpPage, context);
		ContextInjectionFactory.inject(csvOpPage, context);
		
		addPage(typePage);
		addPage(locationPage);
		addPage(xmlOpPage);
		addPage(csvOpPage);
		
//		((WizardDialog)getContainer()).addPageChangedListener(new IPageChangedListener() {
//			
//			@Override
//			public void pageChanged(PageChangedEvent event) {
//				if (event.getSelectedPage() == filePage) {
//					filePage.pageShown();
//				}
//			}
//		});
	}
	
	
}
