/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.ca.in;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Export icon files from a csv file the just list files
 * or a zip that includes icon files 
 * 
 * @author Emily
 *
 */
public class IconExporter {

	public IconExporter() {
	}

	
	public void exportIconKeys(ConservationArea ca, Path csvFile, Session session, IProgressMonitor monitor) throws Exception {
		exportInternal(ca, csvFile, session, false, monitor);
	}
	
	public void exportIconFiles(ConservationArea ca, Path zipFile, Session session, IProgressMonitor monitor) throws Exception {
		exportInternal(ca, zipFile, session, true, monitor);
	}
	
	private void exportInternal(ConservationArea ca, Path outFile, Session session, boolean isZip, IProgressMonitor monitor) throws Exception {
		
		SubMonitor sub = SubMonitor.convert(monitor);
		
		sub.beginTask(Messages.IconExporter_TaskName, isZip ? 5 : 1);
		List<Path> iconFiles = new ArrayList<>();
		List<String> toFile = new ArrayList<>();
		
		Path csvFile = outFile;
		if (isZip) {
			csvFile = Files.createTempFile("smarticons", "csv"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		sub.subTask(Messages.IconExporter_SubTaskA);
		try(FileWriter fwriter = new FileWriter(csvFile.toString(), StandardCharsets.UTF_8);
			CSVWriter writer = new CSVWriter(fwriter)){
			
			ca = session.get(ConservationArea.class, ca.getUuid());
			
			List<Icon> icons = QueryFactory.buildQuery(session, Icon.class, 
					new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			icons.sort((a,b)->Collator.getInstance().compare(a.getKeyId(), b.getKeyId()));
			
			List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, 
					new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
			
			List<Language> languages = new ArrayList<>(ca.getLanguages());
			
			String[] fields = new String[languages.size() + sets.size() + 1];
			fields[0] = IconImporter.ICON_KEY_COLUMN;
			for (int i = 0; i < languages.size(); i ++) {
				fields[i+1] = IconImporter.NAME_COLUMN + languages.get(i).getCode();
			}
			
			for (int i = 0; i < sets.size(); i ++) {
				fields[i + languages.size() + 1] = IconImporter.ICONSET_COLUMN + sets.get(i).getKeyId(); 
			}
			
			writer.writeNext(fields);
			
			for (Icon icon : icons) {
				fields[0] = icon.getKeyId();
				for (int i = 0; i < languages.size(); i ++) {
					fields[i+1] =  icon.findName(languages.get(i));
				}
				
				for (int i = 0; i < sets.size(); i ++) {
					IconFile iconFile = icon.getIconFile(sets.get(i));
					
					iconFiles.add(iconFile.getAttachmentFile());
					
					String fname = iconFile.getAttachmentFile().getFileName().toString();
					String ext = FilenameUtils.getExtension(fname);
					fname = icon.getKeyId() + "." + ext; //$NON-NLS-1$
					String zipitem = sets.get(i).getKeyId() + "/" + fname; //$NON-NLS-1$
					if (toFile.contains(zipitem)) {
						//find a unique name
						fname = UuidUtils.uuidToString(iconFile.getUuid()) + "_" + icon.getKeyId() + "." + ext; //$NON-NLS-1$ //$NON-NLS-2$
						zipitem = sets.get(i).getKeyId() + "/" + fname; //$NON-NLS-1$
						
					}
					toFile.add(zipitem);
					
					fields[i + languages.size() + 1] = zipitem;
				}
				
				writer.writeNext(fields);
			}
		
		}
		
		if (isZip) {
			SubMonitor mon = sub.split(4);
			
	        try (FileOutputStream fOut = new FileOutputStream(outFile.toFile());
	        		BufferedOutputStream bOut = new BufferedOutputStream(fOut);
	        		ZipArchiveOutputStream zOut = new ZipArchiveOutputStream(bOut)){
	           
	        	mon.beginTask(Messages.IconExporter_SubTaskB, iconFiles.size() + 1);
	        	mon.subTask(Messages.IconExporter_SubTaskB);
	        	ZipArchiveEntry zipEntry = new ZipArchiveEntry(csvFile.toFile(), "icons.csv");  //$NON-NLS-1$
	            zOut.putArchiveEntry(zipEntry);
	            try(FileInputStream in = new FileInputStream(csvFile.toFile())){
	            	IOUtils.copy(in, zOut);
	            }
	            zOut.closeArchiveEntry();           
	            mon.worked(1);
	            for (int i = 0; i < iconFiles.size(); i ++) {
	            	
	            	zipEntry = new ZipArchiveEntry(iconFiles.get(i).toFile(), toFile.get(i)); 
		            zOut.putArchiveEntry(zipEntry);
		            try(FileInputStream in = new FileInputStream(iconFiles.get(i).toFile())){
		            	IOUtils.copy(in, zOut);
		            }
		            zOut.closeArchiveEntry();
		            mon.worked(1);
	            }
	            
	        }
	        try {
	        	Files.delete(csvFile);
	        }catch (Exception ex) {
	        	SmartPlugIn.log(ex.getMessage(), ex);
	        }

		}
		monitor.done();
	}
}
