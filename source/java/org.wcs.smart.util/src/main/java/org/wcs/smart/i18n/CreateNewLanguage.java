package org.wcs.smart.i18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class CreateNewLanguage {


	//CT and entity extensions don't match the file structure and this tool won't work for those 
	
	public static final String ROOT_DIR = "C:\\data\\SMART\\Source\\trunk\\source";
	
	public static final String NEW_LANG = "ar";  //arabic
	
	public static final String FROM = "es";
	
	
	public static void main(String[] args) throws Exception{
		CreateNewLanguage newLang = new CreateNewLanguage();
		newLang.copyFile();
		
	}
	
	private void copyFile() throws Exception{
		Path searchPath = Paths.get(ROOT_DIR);
		
		Files.walkFileTree(searchPath, new FileVisitor<Path>(){
	
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.getFileName().toString().equalsIgnoreCase("bin") || dir.getFileName().toString().equals("target")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				if (dir.getFileName().startsWith("org.wcs.smart") && !dir.getFileName().endsWith(".nl")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				if (dir.getFileName().equals("es")) {
					Path toDir = dir.getParent().resolve(NEW_LANG);
					FileUtils.copyDirectory(dir.toFile(), toDir.toFile());
				}
				return FileVisitResult.CONTINUE;
			}
	
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				
				String fileName = file.getFileName().toString();
				
				if (fileName.equalsIgnoreCase("messages_es.properties")) {
					Files.createFile(file.getParent().resolve("messages_" + NEW_LANG + ".properties"));
				}else if (fileName.equalsIgnoreCase("bundle_es.properties")) {
					Files.createFile(file.getParent().resolve("bundle_" + NEW_LANG + ".properties"));
				}else if (fileName.equalsIgnoreCase("plugin_es.properties")) {
					Files.createFile(file.getParent().resolve("bundle_" + NEW_LANG + ".properties"));
				}else if (fileName.endsWith("_es.html")) {
					String newfilename = fileName.substring(0, fileName.lastIndexOf("_es.html"));
					newfilename = newfilename + "_" + NEW_LANG +".html";
					Files.createFile(file.getParent().resolve(newfilename));
				}
				
				return FileVisitResult.CONTINUE;
			}
	
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				if (exc != null) throw exc;
				return FileVisitResult.CONTINUE;
			}
	
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (exc != null) throw exc;
				return FileVisitResult.CONTINUE;
			}
				
		});
		
		
	}
	
	
}
