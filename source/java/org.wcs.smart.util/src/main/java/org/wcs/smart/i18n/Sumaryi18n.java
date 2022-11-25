package org.wcs.smart.i18n;

import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

public class Sumaryi18n {

	public enum Language {
		
		ARABIC("ar", "Arabic"),
		SPANISH("es", "Spanish"),
		FRENCH("fr", "French"),
		HINDI("hi", "Hindi"),
		INDONESIAN("in", "Indonesian"), // should be id
		Georgian("ka", "Georgian"),
		KAREN("kar", "Karen"),
		KHMER("km", "Khmer"),
		LAOS("lo", "Laos"),
		MONGOLIAN("mn", "Mongolian"),
		MALAY("ms", "Malay"),
		BURMESE("my", "Burmese"),
		PORTUGUESE("pt", "Portuguese"),
		RUSSIAN("ru", "Russian"),
		SWAHILI("sw", "Swahili"),
		THAI("th", "Thai"),
		UKAINIAN("uk", "Ukrainian"),
		VIETNAMESE("vi", "Vietnamese"),
		CHINESE("zh", "Chinese"),
		
		ENGLISH("en", "English");
		

		String code;
		String name;

		Language(String code, String name) {
			this.code = code;
			this.name = name;
		}
	}

	private static final String ROOT = "C:\\data\\SMART\\Source\\Version7.X\\";

	
	public static final String[] IN_DIRS = {
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

	public static final String[] TRANS_DIRS = {
			ROOT + "svn\\source\\translations",
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

	public static final String LINE_SEP = "\n";

	private HashMap<Language, Integer> totalCount = new HashMap<>();
	private HashMap<Path, HashMap<Language, Integer>> fileCount = new HashMap<>();

	/**
	 * find all plugin.properties, messages.properties or bundle.properties
	 * files and process each file.
	 * 
	 * @param srcDir
	 * @return
	 * @throws Exception
	 */
	public Path[] findFiles(String coreDir, String transDir) throws Exception {
		Path src = Paths.get(coreDir);
		
		List<Path> filesToVisit = new ArrayList<>();
		
		Files.walkFileTree(src, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.getFileName().toString().equals("bin") || dir.getFileName().toString().equals("target")) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				
				if  (file.getFileName().toString().equals("plugin.properties")
						|| file.getFileName().toString().equals("messages.properties") 
						|| file.getFileName().toString().equals("bundle.properties")) {
					filesToVisit.add(file);
				}
				
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				throw new IOException(exc);
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
		
		int i = 1;
		for (Path file : filesToVisit) {
			int x = countFile(file);
			Integer cnt = totalCount.get(Language.ENGLISH);
			if (cnt == null) {
				cnt = x;
			} else {
				cnt += x;
			}
			totalCount.put(Language.ENGLISH, cnt);

			HashMap<Language, Integer> lCount = fileCount.get(file);
			if (lCount == null) {
				lCount = new HashMap<Sumaryi18n.Language, Integer>();
				fileCount.put(file, lCount);
			}
			lCount.put(Language.ENGLISH, x);

			// System.out.println("Processing: " +f.getAbsolutePath() + "  " +
			// (i++) + "/" + files.size() );
			processFile(file, Paths.get(transDir));
		}

		return filesToVisit.toArray(new Path[filesToVisit.size()]);
	}

	/*
	 * Processes a base file, looking for matching i18n files and merging
	 * matched files.
	 */
	private void processFile(Path file, Path transDir) throws Exception {
		
		Path temp = file.getParent();
//		String pluginName = null;
		List<String> dirs = new ArrayList<>();
		
		Path searchDir = transDir;
		while(temp != null) {
			
			if (temp.getFileName().toString().startsWith("org.wcs.smart")) {
				searchDir = searchDir.resolve(temp.getFileName() + ".nl");
				for(String s : dirs) {
					searchDir = searchDir.resolve(s);
				}
//				pluginName = temp.getFileName().toString();
				break;
			}
			dirs.add(0, temp.getFileName().toString());
			temp = temp.getParent();
			
		}
		if (searchDir == null) throw new Exception("Could not determine plugin name for: " + file.toString());
		
		HashMap<Language, Integer> lCount = fileCount.get(file);
		if (lCount == null) {
			lCount = new HashMap<Sumaryi18n.Language, Integer>();
			fileCount.put(file, lCount);
		}

		String base = FilenameUtils.getBaseName(file.getFileName().toString());
		String ext = FilenameUtils.getExtension(file.getFileName().toString());
		
		for (Language l : Language.values()) {
			if (l == Language.ENGLISH) continue;

			String fileToFind = base + "_" + l.code + "." + ext;
			Path found = searchDir.resolve(fileToFind);
			
			if (!Files.exists(found)) {
//				throw new Exception("Could not find file: " + found.toString());
				return;
			}

			int x = countFile(found);
			Integer cnt = totalCount.get(l);
			if (cnt == null) {
				cnt = x;
			} else {
				cnt += x;
			}
			totalCount.put(l, cnt);

			Integer cnt2 = lCount.get(l);
			if (cnt2 == null) {
				cnt2 = x;
			} else {
				System.err.println("Should not get here; each file should only have one match");
				cnt2 += x;
			}
			lCount.put(l, cnt2);
		}
		
	}

	/*
	 * Processes a file and its matched i18n files
	 */
	private int countFile(Path sourceFile) throws Exception {
		Properties prop = new Properties();
		try(InputStream is = Files.newInputStream(sourceFile)){
			prop.load(is);
		}
		return prop.values().size();
	}

	public static void main(String args[]) {
		Sumaryi18n util = new Sumaryi18n();
		for (int i = 0; i < IN_DIRS.length; i++) {
			try {
				util.findFiles(IN_DIRS[i], TRANS_DIRS[i]);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		for (Language l : Language.values()) {
			System.out.print(l.name + ",");// + l.name + ",");
		}
		System.out.print("File");
		System.out.println();

		double encnt = util.totalCount.get(Language.ENGLISH);
		for (Language l : Language.values()) {
			System.out.print(util.totalCount.get(l));
//			System.out.print(",");
//			System.out.print(util.totalCount.get(l) / encnt);
			System.out.print(",");
		}
		System.out.print("All");
		System.out.println();

		for (Entry<Path, HashMap<Language, Integer>> values : util.fileCount.entrySet()) {

			double encnt2 = values.getValue().get(Language.ENGLISH);

			for (Language l : Language.values()) {
				System.out.print(values.getValue().get(l));
//				System.out.print(",");
//				if (values.getValue().get(l) != null){
//					System.out.print(values.getValue().get(l) / encnt2);
//				}
				System.out.print(",");
			}
			String value = values.getKey().toString();
			for (int i = 0; i < IN_DIRS.length ;  i++){
				value = value.replace(IN_DIRS[i], "");
			}
			value = value.replace("\\", ".");
			value = value.substring(1);
			System.out.print(value);
			System.out.println();
		}
	}

}
