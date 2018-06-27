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

public class CreateNewLanguage {


	//CT and entity extensions don't match the file structure and this tool won't work for those 
	
	public static final String COPY_FROM = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\es";
	public static final String COPY_TO = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\sw";
	
//	public static final String COPY_FROM = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\asset\\translations\\es";
//	public static final String COPY_TO = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\asset\\translations\\sw";
	
//	public static final String COPY_FROM = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\connect\\translations\\es";
//	public static final String COPY_TO = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\connect\\translations\\sw";
	
//	public static final String COPY_FROM = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er\\translations\\es";
//	public static final String COPY_TO = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er\\translations\\sw";
	
//	public static final String COPY_FROM = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\event\\translations\\es";
//	public static final String COPY_TO = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\event\\translations\\sw";
	
//	public static final String COPY_FROM = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\i2\\translations\\es";
//	public static final String COPY_TO = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\i2\\translations\\sw";
	
//	public static final String COPY_FROM = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\qa\\translations\\es";
//	public static final String COPY_TO = "C:\\data\\SMART\\Source\\trunk\\source\\extensions\\qa\\translations\\sw";
	
	public static final String FROM = "es";
	public static final String FROM_TEXT = "Spanish";
	public static final String TO = "sw";
	public static final String TO_TEXT = "Swahili";
	
	
	public static void main(String[] args) throws Exception{
		
		CreateNewLanguage newLang = new CreateNewLanguage();
		newLang.copyFile();
		
	}
	
	private void copyFile() throws Exception{
		Path toPath = Paths.get(COPY_TO);
		Path fromPath = Paths.get(COPY_FROM);
		
		Files.walkFileTree(fromPath, new FileVisitor<Path>(){
	
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path newDir = toPath.resolve( fromPath.relativize(dir) );
				
				if (newDir.getFileName().toString().equalsIgnoreCase("bin") || newDir.getFileName().toString().equals("target")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				Files.createDirectories(newDir);
				return FileVisitResult.CONTINUE;
			}
	
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path toFile = toPath.resolve( fromPath.relativize(file) );
				Files.copy(file, toFile);
				
				if (toFile.getFileName().toString().equalsIgnoreCase("pom.xml") ||
						toFile.getFileName().toString().equalsIgnoreCase(".project")
						) {
					updateFile(toFile);
				}else if (toFile.getFileName().toString().equalsIgnoreCase("MANIFEST.MF") ||
						toFile.getFileName().toString().equalsIgnoreCase("feature.xml")) {
					updateFile(toFile);
					updateFileText(toFile);
					if (toFile.getFileName().toString().equalsIgnoreCase("feature.xml")) {
						updateFeature(toFile);
					}
				}else if (toFile.getFileName().toString().startsWith("messages_" + FROM + ".properties")) {
					Files.createFile(toFile.getParent().resolve("messages_" + TO + ".properties"));
					Files.delete(toFile);
				}else if (toFile.getFileName().toString().startsWith("plugin_" + FROM + ".properties")) {
					Files.createFile(toFile.getParent().resolve("plugin_" + TO + ".properties"));
					Files.delete(toFile);
				}else if(toFile.getFileName().toString().startsWith("bundle_" + FROM + ".properties")) {
					Files.createFile(toFile.getParent().resolve("bundle_" + TO + ".properties"));
					Files.delete(toFile);
				}else if(toFile.getFileName().toString().equalsIgnoreCase("build.properties")){
					updateBuild(toFile);
				}else {
					String name = toFile.getFileName().toString();
					if (name.contains("_" + FROM + ".")) {
						name = name.replaceAll("_" + FROM + "\\.", "_" + TO + "\\.");
						Files.move(toFile, toFile.getParent().resolve(name));
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
		
		//rename directories
		Files.walkFileTree(toPath, new FileVisitor<Path>(){
			
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.toString().endsWith("nl_" + FROM)) {
					Path newDir = dir.getParent().resolve(dir.getFileName().toString().replaceAll("nl_" + FROM, "nl_" + TO));
					Files.move(dir, newDir);
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}
	
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				//TODO: html files
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

		//specifically find help directory
		Path help = toPath.resolve("org.wcs.smart.help.nl_" + TO).resolve("nl").resolve(FROM);
		if (Files.exists(help)) {
			Files.move(help, help.getParent().resolve(TO));
		}
	}
	
	private void updateFile(Path pomFile) throws IOException{
		//read file and remove nl_es with nl_xx
		List<String> lines = Files.readAllLines(pomFile);
		
		ArrayList<String> toWrite = new ArrayList<>();
		for (String line : lines) {
			//replace nl_es with nl_xx
			line = line.replaceAll("nl_" + FROM, "nl_" + TO);
			toWrite.add(line);
		}
		 Files.write(pomFile, toWrite, StandardCharsets.UTF_8);
	}
	
	private void updateFileText(Path pomFile) throws IOException{
		//read file and remove nl_es with nl_xx
		List<String> lines = Files.readAllLines(pomFile);
		
		ArrayList<String> toWrite = new ArrayList<>();
		for (String line : lines) {
			//replace nl_es with nl_xx
			line = line.replaceAll(FROM_TEXT, TO_TEXT);
			toWrite.add(line);
		}
		 Files.write(pomFile, toWrite, StandardCharsets.UTF_8);
	}
	
	private void updateBuild(Path buildFile) throws IOException{
		//read file and remove nl_es with nl_xx
		List<String> lines = Files.readAllLines(buildFile);
		
		ArrayList<String> toWrite = new ArrayList<>();
		for (String line : lines) {
			//replace nl_es with nl_xx
			line = line.replaceAll("plugin_" + FROM, "plugin_" + TO);
			toWrite.add(line);
		}
		 Files.write(buildFile, toWrite, StandardCharsets.UTF_8);
	}

	private void updateFeature(Path buildFile) throws IOException{
		//read file and remove nl_es with nl_xx
		List<String> lines = Files.readAllLines(buildFile);
		
		ArrayList<String> toWrite = new ArrayList<>();
		for (String line : lines) {
			//replace nl_es with nl_xx
			line = line.replaceAll("nls_birt_" + FROM, "nls_birt_" + TO);
			line = line.replaceAll("nls_eclipse_" + FROM, "nls_eclipse_" + TO);
			toWrite.add(line);
		}
		 Files.write(buildFile, toWrite, StandardCharsets.UTF_8);
	}
}
