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

public class CreateTranslationPack {

	
	public static final String LANGUAGE = "sw";
	
	public static final String[] SOURCE_FOLDERS = new String[] {
			"C:\\data\\SMART\\Source\\trunk\\source\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\asset\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\connect\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\event\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\i2\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\qa\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\cybertracker\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\entity\\translations",
			
//			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\r\\translations",
	};
	
	
	public static final String OUT = "C:\\temp\\SMART\\" + LANGUAGE;
			
	public static void main(String[] args) throws Exception{
		Path out = Paths.get(OUT);
		Files.createDirectories(out);
		for (String folder : SOURCE_FOLDERS) {
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(folder))){
				for (Path p : stream) {
					if (!Files.isDirectory(p)) continue;
					//copy folder to temp dir
					copyDirectory(p, out.resolve(p.getFileName()));
				}
			}
		}
		cleanup(out);
	}

	private static void copyDirectory(Path source, Path target) throws Exception{
		Files.walkFileTree(source, new FileVisitor<Path>(){

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path newDir = target.resolve( source.relativize(dir) );
				Files.createDirectories(newDir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path toFile = target.resolve( source.relativize(file) );
				
				if (toFile.getFileName().toString().contains("_" + LANGUAGE + ".")) {
					Files.copy(file, toFile);
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
	
	private static void cleanup(Path source) throws Exception{
		Files.walkFileTree(source, new FileVisitor<Path>(){

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.getFileName().toString().equalsIgnoreCase(".settings") ||
						(dir.getFileName().toString().equalsIgnoreCase("bin")) ||
						(dir.getFileName().toString().equalsIgnoreCase("target")) ) {
						FileUtils.deleteDirectory(dir.toFile());
						return FileVisitResult.SKIP_SUBTREE;
					}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				boolean delete = false;
				if (file.getFileName().toString().equalsIgnoreCase(".project")) delete = true;
				if (file.getFileName().toString().equalsIgnoreCase(".classpath")) delete = true;
				if (file.getFileName().toString().equalsIgnoreCase("build.properties")) delete = true;
				if (file.getFileName().toString().equalsIgnoreCase("pom.xml")) delete = true;
				if (file.getFileName().toString().equalsIgnoreCase("MANIFEST.MF")) delete = true;
				if (file.getFileName().toString().equalsIgnoreCase("feature.xml")) delete = true;
				if (file.getFileName().toString().equalsIgnoreCase(".gitignore")) delete = true;
				
				if (delete) Files.delete(file);
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
		
		//delete empty directories
		Files.walkFileTree(source, new FileVisitor<Path>(){

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.toFile().list().length == 0) {
					Files.delete(dir);
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
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
