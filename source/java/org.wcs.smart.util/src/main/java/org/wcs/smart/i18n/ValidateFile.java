package org.wcs.smart.i18n;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

public class ValidateFile {
	
	private static final String ROOT = "C:\\data\\SMART\\Source\\Version7.X\\";

    public static final String IN_DIR[] = {
    	ROOT + "svn\\source\\java",
    	ROOT + "svn\\source\\extensions\\asset",
		ROOT + "svn\\source\\extensions\\connect",
		ROOT + "svn\\source\\extensions\\cybertracker",
		ROOT + "svn\\source\\extensions\\entity",
		ROOT + "svn\\source\\extensions\\er",
		ROOT + "svn\\source\\extensions\\event",
		ROOT + "svn\\source\\extensions\\i2",
		ROOT + "svn\\source\\extensions\\paws",
		ROOT + "svn\\source\\extensions\\qa",
		ROOT + "svn\\source\\extensions\\r",
    };
    
    public static final String TRANS_DIR[] = {
   		ROOT + "svn\\source\\translations\\",
   		ROOT + "svn\\source\\extensions\\asset\\translations",
   		ROOT + "svn\\source\\extensions\\connect\\translations",
   		ROOT + "svn\\source\\extensions\\cybertracker\\translations",
   		ROOT + "svn\\source\\extensions\\entity\\translations",
   		ROOT + "svn\\source\\extensions\\er\\translations",
   		ROOT + "svn\\source\\extensions\\event\\translations",
   		ROOT + "svn\\source\\extensions\\i2\\translations",
   		ROOT + "svn\\source\\extensions\\paws\\translations",
   		ROOT + "svn\\source\\extensions\\qa\\translations",
   		ROOT + "svn\\source\\extensions\\r\\translations",
    };
    //public static final String[] LANGUAGES =  new String[] {"ar", "es","fr", "hi","in","ka","kar","km","lo","mn","ms","ru","sw","th","vi","zh","pt", "uk"};
//    public static final String[] LANGUAGES =  new String[] {"ar", "fr", "hi","in","ka","kar","km","lo","mn","ms","ru","sw","th","vi","zh"};
	//public static final String[] LANGUAGES = new String[] { "pt" };
    public static final String[] LANGUAGES = new String[] { "th" };

	public static final String LINE_SEP = "\n";

//	public static final String NATIVE2ASCII = "C:\\Java\\jdk1.8.0_201\\bin\\native2ascii.exe";

	/**
	 * find all plugin.properties, messages.properties or bundle.properties files
	 * and process each file.
	 *
	 * @param srcDir
	 * @return
	 * @throws Exception
	 */
	public List<Path> testFiles(String dir, String trans) throws Exception {

		Path start = Paths.get(dir);
		Path transdir = Paths.get(trans);

		List<Path> files = new ArrayList<>();

		Files.walkFileTree(start, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.getFileName().toString().equals("bin"))
					return FileVisitResult.SKIP_SUBTREE;
				if (dir.getFileName().toString().equals("target"))
					return FileVisitResult.SKIP_SUBTREE;
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String name = file.getFileName().toString();
				if (name.equals("plugin.properties") || name.equals("messages.properties")
						|| name.equals("bundle.properties")) {
					files.add(file);
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.TERMINATE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});

		int i = 1;
		for (Path f : files) {
			testParentFile(f, transdir);
		}

		return files;
	}

	private void testParentFile(Path file, Path transdir) throws Exception{
		
		int index = file.toString().indexOf("org.wcs.smart");
        String pluginName = file.toString().substring(index);
        index = pluginName.indexOf(File.separator);
        pluginName = pluginName.substring(0, index);
        String pathName = file.toString().substring(file.toString().indexOf(pluginName) + pluginName.length());
        
        Path transFile = Paths.get(pathName);
	    
	    final String matchDir = pluginName + ".nl";
        Path translationsPath = transdir.resolve(matchDir).resolve(pathName.substring(1)).getParent();
        
        int index2 = transFile.getFileName().toString().lastIndexOf('.');
        final String prefix = transFile.getFileName().toString().substring(0, index2);
        final String postfix = transFile.getFileName().toString().substring(index2 + 1);
           
        testFile(file);
        if (prefix.startsWith("bundle")) return;
        for (String langCode : LANGUAGES) {
        	Path toMerge = translationsPath.resolve(prefix + "_" + langCode + "." + postfix);
        	if (!Files.exists(toMerge)) {
        		System.err.println("File not found: " + toMerge.toString());
        	}else {
        		testFile(toMerge);
        	}
        }
	    
	        
	}
	/*
	 * Processes a base file, looking for matching i18n files and merging matched
	 * files.
	 */
	private void testFile(Path file) throws Exception {
//		System.out.println(file.toString());
		
		boolean print = false;
		int cnt = 1;
		for (String s : Files.readAllLines(file, StandardCharsets.UTF_8)) {

			if (s.contains("\"")) {
				if (!print) System.out.println(file.toString());
				print = true;
				System.out.println(cnt + ":" + "double quote: " + s);
			}
			if (s.contains("{")) {
				// if there is a single quote
				for (int i = 0; i < s.length() - 1; i++) {
					char x = s.charAt(i);
					if (x == '\'') {
						if (s.charAt(i + 1) != '\'') {
							if (!print) System.out.println(file.toString());
							print = true;
							System.out.println(cnt + ":" + "single quote error: " + s);
							break;
						} else {
							i++;
						}
					}
				}
			}
			cnt++;
		}
		if (print) System.out.println();
	}

	public static void main(String args[]) {
		ValidateFile util = new ValidateFile();
		try {
			for (int i = 0; i < IN_DIR.length; i++) {
				util.testFiles(IN_DIR[i], TRANS_DIR[i]);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}