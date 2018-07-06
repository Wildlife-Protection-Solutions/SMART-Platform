package org.wcs.smart.i18n;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;

public class CombineFeatures {

	private static String[] langs = new String[] {
			"es","fr","hi","in","ka","kar","km", "lo","mn","ms","ru","sw","th","vi","zh" 
	};
	
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\";
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\qa\\translations";
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\i2\\translations";
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\event\\translations";
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er\\translations";
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\asset\\translations";
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\connect\\translations";
//	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\entity\\translations";
	public static final String MAIN_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\cybertracker\\translations";
	public static final String TO_DIR = "nl";
	
	
	
	public static void main(String[] args) throws IOException {
	
		Path fromPath = Paths.get(MAIN_DIR).resolve("es");
		
		DirectoryStream<Path> stream = Files.newDirectoryStream(fromPath);
		for (Path dir : stream) {
			String fileName = dir.getFileName().toString();
			int index = fileName.indexOf(".nl_");
			fileName = fileName.substring(0, index+3);
			
			Path newFolder = Paths.get(MAIN_DIR).resolve(TO_DIR).resolve(fileName);
			
			//copy contents of dir to newFolder
			FileUtils.copyDirectory(dir.toFile(), newFolder.toFile());
		}
		
		for (String l : langs) {
			if (l.equals("es")) continue;
			
			Path from = Paths.get(MAIN_DIR).resolve(l);

			Files.walkFileTree(from, new FileVisitor<Path>(){

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (!file.getFileName().toString().endsWith(".properties")) return FileVisitResult.CONTINUE;
					if (file.getFileName().toString().startsWith("bundle_") || 
							file.getFileName().toString().startsWith("messages_") || 
							file.getFileName().toString().startsWith("plugin_")) {
						Path rel = from.relativize(file);
						
						String dirname = rel.getName(0).toString();
						int index = dirname.indexOf("_");
						dirname = dirname.substring(0, index);
						rel = rel.subpath(1, rel.getNameCount());
						Path toPath = Paths.get(MAIN_DIR).resolve(TO_DIR).resolve(dirname).resolve(rel);
						Files.copy(file, toPath);
						
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					throw exc;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
				
			});
		}
		
		System.out.println("done");
	}
}
