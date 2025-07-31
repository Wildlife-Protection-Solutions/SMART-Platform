package org.wcs.smart.i18n;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class CreateNewLanguage {


	//CT and entity extensions don't match the file structure and this tool won't work for those 
	
//	public static final String ROOT_DIR = "C:\\data\\SMART\\Source\\trunk\\svn\\source";
	public static final String ROOT_DIR = "C:\\data\\SMART\\Source\\Trunk\\svn\\source";
	
	public static final String NEW_LANG = "hy";  
	
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
				if (dir.getFileName().toString().equalsIgnoreCase("bin") || dir.getFileName().toString().equals("target") || dir.getFileName().toString().equals("archive")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				if (dir.getFileName().startsWith("org.wcs.smart") && !dir.getFileName().endsWith(".nl")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				if (dir.getFileName().toString().equals(FROM)) {
					Path toDir = dir.getParent().resolve(NEW_LANG);
					if (!Files.exists(toDir)) {
						FileUtils.copyDirectory(dir.toFile(), toDir.toFile());
					}
				}
				return FileVisitResult.CONTINUE;
			}
	
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				
				String fileName = file.getFileName().toString();
	
				Path toCreate = null;
				Path copyFrom = null;
				if (fileName.equalsIgnoreCase("messages_" + FROM + ".properties")) {
					toCreate = file.getParent().resolve("messages_" + NEW_LANG + ".properties");					
				}else if (fileName.equalsIgnoreCase("bundle_" + FROM + ".properties")) {
					toCreate = file.getParent().resolve("bundle_" + NEW_LANG + ".properties");
				}else if (fileName.equalsIgnoreCase("plugin_" + FROM + ".properties")) {
					toCreate = file.getParent().resolve("bundle_" + NEW_LANG + ".properties");					
				}else if (fileName.endsWith("_" + FROM + ".html")) {
					copyFrom = file;
					String newfilename = fileName.substring(0, fileName.lastIndexOf("_" + FROM + ".html"));
					newfilename = newfilename + "_" + NEW_LANG +".html";					
					toCreate = file.getParent().resolve(newfilename);					
				}
				if (copyFrom != null) {
					Files.copy(copyFrom, toCreate, StandardCopyOption.REPLACE_EXISTING);
				}else if (toCreate != null) {
					if (!Files.exists(toCreate)) {
						Files.createFile(toCreate);					
					}
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
