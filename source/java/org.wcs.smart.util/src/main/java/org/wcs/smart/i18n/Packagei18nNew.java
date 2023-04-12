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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.derby.iapi.services.io.FileUtil;

/**
 * Packages all translations files for a given language into a single
 * folder, for zipping and providing to the users. 
 *  
 * Used for 6.2 and above
 * 
 * @author egouge
 *
 */
public class Packagei18nNew {

	public static final String SOURCE_DIR = "C:\\data\\SMART\\Source\\Version7.X\\svn\\source\\";

//	public static final String SOURCE_DIR = "C:\\data\\SMART\\Source\\Trunk\\svn\\source\\";
//	public static final String SOURCE_DIR = "C:\\data\\SMART\\Source\\Trunk\\udig\\udig-platform\\plugins";
	
	public static final String OUT_DIR = "C:\\temp\\smarti18n\\";
	
//	public static final String[] LANGUAGES =  new String[] {"ar", "es","fr", "hi","in","ka","kar","km","lo","mn","ms","ru","sw","th","vi","zh","pt", "uk"};
//	public static final String[] LANGUAGES = {"hr"};
//	public static final String[] LANGUAGES = {"my"};
	public static final String[] LANGUAGES = {"ur"};
	
	public void doWork() throws Exception {
		Path path = Paths.get(OUT_DIR);
		
		for (String lang : LANGUAGES) {
			System.out.println("Processing: " + lang);
			Path outputPath = path.resolve(lang);
			if (Files.exists(outputPath)) {
				FileUtils.deleteDirectory(outputPath.toFile());
			}
			Files.createDirectories(outputPath);
			
			processLanguage(lang, outputPath);
		}
	}
	
	
	private Path getToPath(Path sourceDir, Path sourcePath, Path outputPath) {
		Path root = sourceDir.relativize(sourcePath);
		//remove folders at the beginning up to something that starts with org.wcs.smart
		int index = 0;
		for (int i = 0; i < root.getNameCount(); i ++) {
			if (root.getName(i).toString().startsWith("org.wcs.smart")) {
//			if (root.getName(i).toString().startsWith("org.locationtech.udig")) {
				index = i;
				break;
			}
		}
		
		Path out = outputPath;
		for (int i = index; i < root.getNameCount(); i ++) {
			out = out.resolve(root.getName(i));
		}
		
		return out;
		
	}
	
	
	private void processLanguage(String lang, Path outputPath) throws IOException{
		//search for all files labelled plugin_XX.properties, messages_XX.properties or bundle_XX.properties
		//copy each file to the output location;
		//also include directories with XX
		
		Path sourceDir = Paths.get(SOURCE_DIR);
		
		SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.getFileName().toString().equalsIgnoreCase(lang)) {
					//copy entire directory
					Path toFile = getToPath(sourceDir,  dir, outputPath);					
					Files.createDirectories(toFile);
					copyDirectory(dir, toFile);
					return FileVisitResult.SKIP_SUBTREE;
				}
				if (dir.getFileName().toString().equalsIgnoreCase("bin")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				if (dir.getFileName().toString().equalsIgnoreCase("target")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String fileName = file.getFileName().toString();
				
				if (fileName.equals("plugin_" + lang + ".properties") ||
						fileName.equals("messages_" + lang + ".properties") ||
						fileName.equals("bundle_" + lang + ".properties") ||
						fileName.endsWith("_" + lang + ".html")) {
				
//				if (fileName.equals("plugin.properties") ||
//						fileName.equals("messages.properties") ||
//						fileName.equals("bundle.properties") ||) {
//					
					
					Path toFile = getToPath(sourceDir,  file, outputPath);
					
					boolean copy = true;
					//only copy files with changes
//					boolean copy = false;
//					System.out.println("Processing:" + file.toString());
//					List<String> data = Files.readAllLines(file, StandardCharsets.UTF_8);
//					for (String d : data) {
//						if (d.contains("**NEW**")) {
//							copy = true;
//							break;
//						}
//					}
					
					if (copy) {
						Files.createDirectories(toFile.getParent());
						System.out.println(file.toString() + " to " + toFile.toString());
						Files.copy(file, toFile);
					}
					
				}
						
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				if (exc != null) throw exc;
				throw new IOException("Failed to visit path: " + file.toString());
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		};
		
		Files.walkFileTree(sourceDir, visitor);
	}
	
	
	private void copyDirectory(Path from, Path to) throws IOException{
		FileUtils.copyDirectory(from.toFile(), to.toFile());
	}
	
	public static void main(String args[]) {
		Packagei18nNew util = new Packagei18nNew();
		try{
			util.doWork();
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}
	
	
}
