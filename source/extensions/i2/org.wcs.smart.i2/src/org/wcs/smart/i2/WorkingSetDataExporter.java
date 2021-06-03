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
package org.wcs.smart.i2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.entity.exporter.EntityRelationshipExporter;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetQuery;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.QueryManager;
import org.wcs.smart.i2.query.export.CsvEntitySummaryQueryExporter;
import org.wcs.smart.i2.query.export.CsvRecordQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter;
import org.wcs.smart.i2.query.export.IQueryExporter.ExportOption;
import org.wcs.smart.i2.query.export.ShpRecordQueryExporter;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.entity.exporter.RecordCsvExporter;
import org.wcs.smart.i2.xml.EntityToXml;
import org.wcs.smart.i2.xml.RecordXmlExporter;
import org.wcs.smart.util.UuidUtils;
import org.wcs.smart.util.ZipUtil;

public enum WorkingSetDataExporter {

	INSTANCE;
	
	private static final String CSV_FOLDER = "csv"; //$NON-NLS-1$
	private static final String XML_FOLDER = "xml"; //$NON-NLS-1$
	

	public void export(IntelWorkingSet ws, Path exportFile, char delimiter, Charset charset, Projection prj, IProgressMonitor monitor) throws Exception {
		if (exportFile.getParent() == null || !Files.isDirectory(exportFile.getParent())) throw new IllegalStateException("invalid output file"); //$NON-NLS-1$

		Files.createDirectories(exportFile.getParent());
		Path tempDir = Files.createTempDirectory("smartwsexport"); //$NON-NLS-1$	
		try {
			SubMonitor sub = SubMonitor.convert(monitor);
			sub.beginTask(Messages.WorkingSetDataExporter_ExportTask, 6);
			
			Path entities = tempDir.resolve("entities"); //$NON-NLS-1$
			Files.createDirectory(entities);
			Path entitiescsv = entities.resolve(CSV_FOLDER);
			Files.createDirectory(entitiescsv);
			Path entitiesxml = entities.resolve(XML_FOLDER);
			Files.createDirectory(entitiesxml);
			
			Path records = tempDir.resolve("records"); //$NON-NLS-1$
			Files.createDirectory(records);
			Path recordscsv = records.resolve(CSV_FOLDER);
			Files.createDirectory(recordscsv);
			Path recordsxml = records.resolve(XML_FOLDER);
			Files.createDirectory(recordsxml);
			Path queries = tempDir.resolve("queries"); //$NON-NLS-1$
			Files.createDirectory(queries);
			
			try(Session session = HibernateManager.openSession()){
				ws = session.get(IntelWorkingSet.class, ws.getUuid());
				
				exportEntitiesCsv(ws, entitiescsv, delimiter, charset, sub.split(1));
				exportEntitiesXml(ws, entitiesxml, session, sub.split(1));
				
				Path rfile = recordscsv.resolve("records.csv"); //$NON-NLS-1$
				exportRecordsCsv(ws, rfile, session, delimiter, charset, sub.split(1));
				exportRecordsXml(ws, recordsxml, session, sub.split(1));
				
				exportQueries(ws, queries, session, delimiter, charset, prj, sub.split(1));
			}
			
			//zip everything up
			ZipUtil.createZip(new Path[] {entities, records, queries}, exportFile, sub.split(1));
		}finally {
			try {
				FileUtils.deleteDirectory(tempDir.toAbsolutePath().toFile());
			}catch (IOException ex) {
				Intelligence2PlugIn.log(ex.getMessage(), ex);
			}
		}
	}
	
	private void exportQueries(IntelWorkingSet ws, Path exportDir, Session session, char delimiter, Charset charset, Projection prj, IProgressMonitor monitor) throws Exception {
		
		SubMonitor toDo = SubMonitor.convert(monitor);
		toDo.beginTask(Messages.WorkingSetDataExporter_queriesTask, ws.getQueries().size());
		
		HashMap<String, Object> queryParams = new HashMap<>();
		queryParams.put(Locale.class.getName(), Locale.getDefault());
		queryParams.put(ConservationArea.class.getName(), Collections.singleton(ws.getConservationArea()));
		queryParams.put(LocalDate.class.getName(), WorkingSetManager.INSTANCE.parseEntityDateFilter(ws) );
		queryParams.put(Session.class.getName(), session);
		
		Set<String> pKeys = new HashSet<>();
		for (IntelProfile p : ProfilesManager.INSTANCE.getProfiles(session, false)) {
			if (IntelSecurityManager.INSTANCE.canViewQuery(p)) pKeys.add(p.getKeyId());
		}
		
		for (IntelWorkingSetQuery wsq : ws.getQueries()) {
			AbstractIntelQuery query = QueryManager.INSTANCE.findQuery(session, wsq.getQuery(), wsq.getQueryType());
			
			if (!query.queriesProfile(pKeys)) continue;
			
			IIntelQueryEngine engine = SmartContext.INSTANCE.getClass(IQueryEngineFactory.class).findQueryEngine(query.getTypeKey());
			
			queryParams.put(IProgressMonitor.class.getName(), toDo.split(1));
			IQueryResult result = null;
			
			session.beginTransaction();
			try {
				result = engine.executeQuery(query, queryParams);
			}finally {
				session.getTransaction().commit();
			}

			List<IQueryExporter> exports = new ArrayList<>();
			exports.add(new CsvRecordQueryExporter());
			exports.add(new ShpRecordQueryExporter());
			exports.add(new CsvEntitySummaryQueryExporter());
			
			HashMap<ExportOption, Object> exportOptions = new HashMap<>();
			exportOptions.put(ExportOption.DELIMITER, delimiter);
			exportOptions.put(ExportOption.ENCODING, charset);
			exportOptions.put(ExportOption.LOCALE, Locale.getDefault());
			exportOptions.put(ExportOption.PROJECTION, prj);
			
			for (IQueryExporter exporter : exports) {
				if (exporter.canExport(query.getTypeKey())) {
					Path file = exportDir.resolve(URLUtils.cleanFilename(query.getName()) + "_" + UuidUtils.uuidToString(query.getUuid()) + "." + exporter.getExtension() ); //$NON-NLS-1$ //$NON-NLS-2$
					exporter.exportQuery(session, result, file, exportOptions);
				}
			}
		}
	}
	
	private void exportEntitiesCsv(IntelWorkingSet ws, Path exportDir, char delimiter, Charset cs, IProgressMonitor monitor) throws Exception {
		Set<UUID> toexport = new HashSet<>();
		for (IntelWorkingSetEntity entity : ws.getEntities()) {
			if (IntelSecurityManager.INSTANCE.canViewEntities(entity.getEntity().getProfile())) {
				toexport.add(entity.getEntity().getUuid());
			}
		}
		if (toexport.isEmpty()) return;
		
		EntityRelationshipExporter erexporter = new EntityRelationshipExporter();
		Path entityFile = exportDir.resolve("entities.csv"); //$NON-NLS-1$
		Path relationshipFile = exportDir.resolve("relationships.csv"); //$NON-NLS-1$
		erexporter.doExport(toexport, 0, entityFile, relationshipFile, delimiter, cs, monitor);
	}
	
	private void exportEntitiesXml(IntelWorkingSet ws, Path exportDir, Session session, IProgressMonitor monitor) throws Exception {
		List<UUID> toexport = new ArrayList<>();
		for (IntelWorkingSetEntity entity : ws.getEntities()) {
			if (IntelSecurityManager.INSTANCE.canViewEntities(entity.getEntity().getProfile())) {
				if (!toexport.contains(entity.getEntity().getUuid())) toexport.add(entity.getEntity().getUuid());
			}
		}
		if (toexport.isEmpty()) return;
		
		EntityToXml exporter = new EntityToXml(session);
		exporter.exportToDirectory(exportDir, toexport, true, true, true, monitor);
	}
	
	
	private void exportRecordsCsv(IntelWorkingSet ws, Path exportFile, Session session, char delimiter, Charset cs, IProgressMonitor monitor) throws Exception {
		Set<UUID> toexport = new HashSet<>();
		for (IntelWorkingSetRecord record : ws.getRecords()) {
			if (IntelSecurityManager.INSTANCE.canViewRecords(record.getRecord().getProfile())) {
				toexport.add(record.getRecord().getUuid());
			}
		}
		if (toexport.isEmpty()) return;
		
		RecordCsvExporter exporter = new RecordCsvExporter(new ArrayList<>(toexport));
		exporter.exportCsvFile(exportFile, delimiter, ws.getConservationArea(), true, cs, monitor, session);
	}
	
	private void exportRecordsXml(IntelWorkingSet ws, Path exportDir, Session session, IProgressMonitor monitor) throws Exception {	
		RecordXmlExporter exporter = new RecordXmlExporter(exportDir);
		for (IntelWorkingSetRecord wsr : ws.getRecords()) {
			if (IntelSecurityManager.INSTANCE.canViewRecords(wsr.getRecord().getProfile())) {
				String fname = URLUtils.cleanFilename(wsr.getRecord().getTitle());
				fname += "_" + UuidUtils.uuidToString(wsr.getRecord().getUuid()); //$NON-NLS-1$
				
				Path dir = exportDir.resolve(fname);
				Files.createDirectories(dir);
				
				exporter.exportToDirectory(wsr.getRecord().getUuid(), dir, monitor);
			}
		}
	}
}
