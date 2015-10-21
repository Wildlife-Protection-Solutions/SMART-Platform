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
package org.wcs.smart.intelligence;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.backup.IBackupContributor;
import org.wcs.smart.intelligence.internal.Messages;

/**
 * Backup creation contributor that allows to exclude informant secure data from backups.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantBackupContributor implements IBackupContributor {
	
	private static final String AES_FILE_PATTERN = "filestore/[0-9a-fA-F]{32}+/intelligence/aes/[0-9a-fA-F]{32}+\\.dat"; //$NON-NLS-1$

	@Override
	public void process(File backupZipFile, IProgressMonitor monitor) {
		monitor.subTask(Messages.InformantBackupContributor_Task_Scan);
		List<String> dataFiles = getInformantSecureData(backupZipFile);
		if (!dataFiles.isEmpty()) {
			final boolean[] result = {false};
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					result[0] = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), Messages.InformantBackupContributor_Dialog_Title, Messages.InformantBackupContributor_Dialog_Message);
				}
			});
			if (result[0]) {
				monitor.subTask(Messages.InformantBackupContributor_Task_Remove);
				removeInformantData(backupZipFile, dataFiles);
			}
		}
		monitor.done();
	}

	private List<String> getInformantSecureData(File backupZipFile) {
		List<String> dataFiles = new ArrayList<>();
		try(ZipFile zipFile = new ZipFile(backupZipFile)) {
			Enumeration<? extends ZipEntry> e = zipFile.entries();
			while (e.hasMoreElements()) {
				ZipEntry entry = e.nextElement();
				String entryName = entry.getName();
				if (entryName.matches(AES_FILE_PATTERN)) {
					dataFiles.add(entryName);
				}
			}
		}
		catch (IOException e) {
			SmartPlugIn.displayLog(Messages.InformantBackupContributor_OpenFileError, e);
		}
		return dataFiles;
	}

	private void removeInformantData(File backupZipFile, List<String> dataFiles) {
		/* Define ZIP File System Properties in HashMap */    
        Map<String, String> zip_properties = new HashMap<>(); 
        /* We want to read an existing ZIP File, so we set this to False */
        zip_properties.put("create", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        /* Specify the encoding as UTF-8 */
        zip_properties.put("encoding", StandardCharsets.UTF_8.name());   //$NON-NLS-1$
        /* Specify the path to the ZIP File that you want to read as a File System */
        URI zip_disk = URI.create("jar:" + backupZipFile.toURI().toString()); //$NON-NLS-1$
        
        try(FileSystem zipfs = FileSystems.newFileSystem(zip_disk, zip_properties)) {
        	for (String path : dataFiles) {
                Path pathInZipfile = zipfs.getPath(path);
                Files.delete(pathInZipfile);
			}
		} catch (IOException e) {
			SmartPlugIn.displayLog(Messages.InformantBackupContributor_DeleteDataError, e);
		}
	}
	
}
