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
package org.wcs.smart.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Converts all files in the directory from utf8 to ascii unicode
 *  
 * Used for 6.2 and above
 * 
 * @author egouge
 *
 */
public class ConvertToUnicode {

    public static final String NATIVE2ASCII = "C:\\Java\\jdk1.8.0_201\\bin\\native2ascii.exe";


	public static final String OUT_DIR = "C:\\temp\\smarti18n\\km_20240419.fromuser\\km_20240419";
	
	public void doWork() throws Exception {
		Path path = Paths.get(OUT_DIR);
	
		SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				
				if (file.getFileName().toString().endsWith(".properties")) {
					convertfile(file);
				}
						
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				if (exc != null) throw exc;
				throw new IOException("Failed to visit path: " + file.toString());
			}

		};
		
		Files.walkFileTree(path, visitor);
	}
	
	private void convertfile(Path file) throws IOException {
		StringBuilder cmd = new StringBuilder();
    	cmd.append(NATIVE2ASCII);
    	cmd.append(" -encoding UTF-8 ");
    	cmd.append(file.toString());
    	cmd.append(" " );
    	cmd.append(file.toString());
    	
    	System.out.println(cmd);
    	
    	Process pr = Runtime.getRuntime().exec(cmd.toString());
        InputStream is = pr.getInputStream();
        while(is.read() != -1){}
        is = pr.getErrorStream();
        while(is.read() != -1){}
	}
	
	public static void main(String args[]) {
		ConvertToUnicode util = new ConvertToUnicode();
		try{
			util.doWork();
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}
	
	
}
