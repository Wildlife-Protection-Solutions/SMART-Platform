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
package org.wcs.smart;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Map;
import java.util.stream.Stream;

import org.wcs.smart.ca.DataStoreManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Generates the conservation area size report with reference
 * to wiki page for more information
 * 
 * @author Emily
 * @since 7.4.1
 *
 */
public class CaSizeReport {

	public static final String INFO_URL = "https://app.assembla.com/spaces/smart-cs/wiki/SMART_Conservation_Area_Size_Report"; //$NON-NLS-1$
	private static final String ERROR = Messages.CaSizeReport_ErrorLbl;
	

	
	public String generateReport() {
		
		Map<String,String> dirs = DataStoreManager.INSTANCE.getFilestoreDirectoriesToPlugin();
		
		//database size
		//sum of filesize
		String dbDir = SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB);
		
		StringBuilder sb = new StringBuilder();
		String dbSizeF = null;
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.CaSizeReport_CaFileSizeByFolder);
		sb.append("\n"); //$NON-NLS-1$
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		
		Path fileStore = Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation());
		
		long[] total = new long[] {0};
		try (Stream<Path> stream = Files.list(fileStore)){
			stream.forEach(file->{
				if (Files.isDirectory(file)) {
					
					String name = file.getFileName().toString();
					if (dirs.containsKey(name)) {
						name += " (" + dirs.get(name) + ")"; //$NON-NLS-1$  //$NON-NLS-2$
					}
					String dirSize = null;
					try {
						long size =sumSize(file);
						total[0] = total[0] + size;
						dirSize = formatSize(size);
					}catch (Exception ex) {
						dirSize = ERROR + " " + ex.getMessage(); //$NON-NLS-1$
						SmartPlugIn.log(ex.getMessage(), ex);
					}
					sb.append(name + ": " + dirSize); //$NON-NLS-1$
					sb.append("\n"); //$NON-NLS-1$
				}else {
					try {
						total[0] = total[0] + Files.size(file);
					}catch (Exception ex) {
						SmartPlugIn.log(ex.getMessage(), ex);
					}
				}
				
			});
		}catch (IOException ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			sb.append(ex.getMessage());
			sb.append("\n"); //$NON-NLS-1$
		}
		
		sb.append("\n"); //$NON-NLS-1$
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.CaSizeReport_CaFileSizeTotal);
		sb.append("\n"); //$NON-NLS-1$
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(formatSize(total[0]));
		
		sb.append("\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		long dbsize = 0;
		try {
			dbsize = sumSize(Paths.get(dbDir));
			dbSizeF = formatSize(dbsize);
		}catch (Exception ex) {
			dbSizeF = ERROR + " " + ex.getMessage(); //$NON-NLS-1$
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.CaSizeReport_DatabaseSize);
		sb.append("\n"); //$NON-NLS-1$
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(dbSizeF);
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.CaSizeReport_DbSizeInfo);
		
		
		sb.append("\n"); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(Messages.CaSizeReport_MoreInfoTitle);
		sb.append("\n"); //$NON-NLS-1$
		sb.append("==================================="); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(Messages.CaSizeReport_MoreInfo, INFO_URL));
		return sb.toString();
	}
	
	
	private String formatSize(long bytes) {
		if (bytes  < 1_000) return bytes + " B"; //$NON-NLS-1$
		if (bytes < 1_000_000) return (int)(bytes/1000) + " KB"; //$NON-NLS-1$
		if (bytes < 1_000_000_000) return (int)(bytes/1_000_000) + " MB"; //$NON-NLS-1$
		return (int)(bytes/1_000_000_000) + " GB"; //$NON-NLS-1$
	}
	
	/**
	 * return the total size of all files in the directory in bytes
	 * 
	 * @param dir
	 * @return
	 * @throws IOException 
	 */
	private long sumSize(Path dir) throws IOException {
		if (!Files.isDirectory(dir)) return Files.size(dir);
		
		SumSizeFVisitor sum = new SumSizeFVisitor();
		Files.walkFileTree(dir, sum);
		return sum.getSum();
	}
	
	private class SumSizeFVisitor extends SimpleFileVisitor<Path>{
		long size = 0;
		public long getSum() {
			return this.size;
		}
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			size += attrs.size();
			return super.visitFile(file, attrs);
		}
	};
}
