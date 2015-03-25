package org.wcs.smart.i18n;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

public class Sumaryi18n {

	public enum Language {
		ENGLISH("en", "English"),
		SPANISH("es", "Spanish"),
		FRENCH("fr", "French"),
		RUSSIAN("ru", "Russian"),
		THAI("th", "Thai"),
		CHINESE("zh", "Chinese"),
		VIETNAMESE("vi", "Vietnamese"),
		MALAY("ms", "Malay"),
		LAOS("lo", "Laos"),
		HINDI("hi", "Hindi"),
		INDONESIAN("in", "Indonesian"), // should be id
		KHMER("km", "Khmer");

		String code;
		String name;

		Language(String code, String name) {
			this.code = code;
			this.name = name;
		}
	}

	public static final String[] IN_DIRS = {
			"C:\\data\\SMART\\Source\\trunk\\source\\java",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er"
		};

	public static final String[] TRANS_DIRS = {
			"C:\\data\\SMART\\Source\\trunk\\source\\translations",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions",
			"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er\\translations" 
		};

	public static final String LINE_SEP = "\n";

	private HashMap<Language, Integer> totalCount = new HashMap<Sumaryi18n.Language, Integer>();
	private HashMap<File, HashMap<Language, Integer>> fileCount = new HashMap<File, HashMap<Language, Integer>>();

	/**
	 * find all plugin.properties, messages.properties or bundle.properties
	 * files and process each file.
	 * 
	 * @param srcDir
	 * @return
	 * @throws Exception
	 */
	public File[] findFiles(String coreDir, String transDir) throws Exception {
		File src = new File(coreDir);
		IOFileFilter filter = new IOFileFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return accept(new File(dir, name));
			}

			@Override
			public boolean accept(File file) {
				/* exclude bin dir */
				if (checkBin(file)) {
					return false;
				}

				return (file.getName().equals("plugin.properties")
						|| file.getName().equals("messages.properties") || file
						.getName().equals("bundle.properties"));

			}

			private boolean checkBin(File f) {
				if (f == null) {
					return false;
				}
				if (f.getName().equals("bin")) {
					return true;
				}
				return checkBin(f.getParentFile());
			}
		};

		Collection<File> files = FileUtils.listFiles(src, filter,
				DirectoryFileFilter.DIRECTORY);
		int i = 1;
		for (File f : files) {
			int x = countFile(f);
			Integer cnt = totalCount.get(Language.ENGLISH);
			if (cnt == null) {
				cnt = x;
			} else {
				cnt += x;
			}
			totalCount.put(Language.ENGLISH, cnt);

			HashMap<Language, Integer> lCount = fileCount.get(f);
			if (lCount == null) {
				lCount = new HashMap<Sumaryi18n.Language, Integer>();
				fileCount.put(f, lCount);
			}
			lCount.put(Language.ENGLISH, x);

			// System.out.println("Processing: " +f.getAbsolutePath() + "  " +
			// (i++) + "/" + files.size() );
			processFile(f, transDir);
		}

		return files.toArray(new File[files.size()]);
	}

	/*
	 * Processes a base file, looking for matching i18n files and merging
	 * matched files.
	 */
	private void processFile(File f, String stransDir) throws Exception {
		File transDir = new File(stransDir);
		int index = f.getCanonicalPath().indexOf("org.wcs.smart");
		String pluginName = f.getCanonicalPath().substring(index);
		index = pluginName.indexOf(File.separator);
		pluginName = pluginName.substring(0, index);
		String pathName = f.getCanonicalPath().substring(
				f.getCanonicalPath().indexOf(pluginName) + pluginName.length());
		File transFile = new File(pathName);

		HashMap<Language, Integer> lCount = fileCount.get(f);
		if (lCount == null) {
			lCount = new HashMap<Sumaryi18n.Language, Integer>();
			fileCount.put(f, lCount);
		}

		for (Language l : Language.values()) {
			if (l == Language.ENGLISH)
				continue;
			List<File> filesList = new ArrayList<File>();

			final String matchDir = pluginName + ".nl_" + l.code; /*
																 * ADD _XX if
																 * you want to
																 * search for a
																 * specific
																 * language
																 */
			for (File flangDir : transDir.listFiles()) {
				if (!flangDir.isDirectory()) {
					continue;
				}
				File[] transToMerge = flangDir.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File arg0, String name) {
						return name.startsWith(matchDir);
					}
				});
				for (File tmp : transToMerge) {
					filesList.add(tmp);
				}
				if (flangDir.getName().startsWith(matchDir)){
					filesList.add(flangDir);
				}

			}
			if (filesList.size() > 1) {
				System.err.println("ERROR: " + filesList.size());
			}

			for (File ft : filesList) {
				String langCode = ft.getName().substring(
						ft.getName().indexOf('_') + 1);
				int index2 = transFile.getName().lastIndexOf('.');
				final String prefix = transFile.getName().substring(0, index2);
				final String postfix = transFile.getName()
						.substring(index2 + 1);

				File toMerge = new File(ft.getCanonicalPath()
						+ transFile.getParent() + File.separator + prefix + "_"
						+ langCode + "." + postfix);

				// System.out.println(f.toString());
				// System.out.println(toMerge.toString());

				if (!f.exists() || !toMerge.exists()) {
					System.err
							.println("Error either source or target file does not exists. "
									+ f.toString()
									+ "  |  "
									+ toMerge.toString());
				} else {
					int x = countFile(toMerge);
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
						System.err
								.println("Should not get here; each file should only have one match");
						cnt2 += x;
					}
					lCount.put(l, cnt2);

				}
			}
		}
	}

	/*
	 * Processes a file and its matched i18n files
	 */
	private int countFile(File sourceFile) throws Exception {
		Properties prop = new Properties();
		FileReader fr = new FileReader(sourceFile);
		prop.load(fr);
		fr.close();

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

		for (Entry<File, HashMap<Language, Integer>> values : util.fileCount.entrySet()) {

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
