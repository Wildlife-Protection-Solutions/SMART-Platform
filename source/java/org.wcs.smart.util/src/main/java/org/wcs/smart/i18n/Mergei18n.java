package org.wcs.smart.i18n;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
/**
 * Matches i18n resource property files in fragments with the 
 * default property file and removes any no longer used key/value pairs.  It 
 * will also add missing key/value pairs (with the value using the
 * English language). 
 * 
 * @author Emily
 *
 */
@SuppressWarnings("nls")
public class Mergei18n {

	public static final String IN_DIR = "C:\\data\\SMART\\Source\\Version1\\trunk\\source\\java\\";
	
	public static final String TRANS_DIR = "C:\\data\\SMART\\Source\\Version1\\trunk\\source\\translations\\";
	
	public static final String LINE_SEP = "\n";
	
	/**
	 * find all plugin.properties, messages.properties or bundle.properties
	 * files and process each file.
	 * 
	 * @param srcDir
	 * @return
	 * @throws Exception
	 */
	public File[] findFiles(String srcDir) throws Exception {
		File src = new File(srcDir);

		IOFileFilter filter = new IOFileFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return accept(new File(dir, name));
			}

			@Override
			public boolean accept(File file) {
				/* exclude bin dir */
				if (checkBin(file)){
					return false;
				}
				
				return (file.getName().equals("plugin.properties")
						|| file.getName().equals("messages.properties") || file
						.getName().equals("bundle.properties"));

			}
			
			private boolean checkBin(File f){
				if (f == null){
					return false;
				}
				if (f.getName().equals("bin")){
					return true;
				}
				return checkBin(f.getParentFile());
			}
		};

		Collection<File> files = FileUtils.listFiles(src, filter,
				DirectoryFileFilter.DIRECTORY);
		int i = 1;
		for (File f : files) {
			System.out.println("Processing: " +f.getAbsolutePath() + "  " + (i++) + "/" + files.size() );
			processFile(f);
		}

		return files.toArray(new File[files.size()]);
	}

	/*
	 * Processes a base file, looking for matching i18n files
	 * and merging matched files.
	 */
	private void processFile(File f) throws Exception {
		File transDir = new File(TRANS_DIR);

		int index = f.getCanonicalPath().indexOf("org.wcs.smart");
		String pluginName = f.getCanonicalPath().substring(index);
		index = pluginName.indexOf(File.separator);
		pluginName = pluginName.substring(0, index);
		String pathName = f.getCanonicalPath().substring(f.getCanonicalPath().indexOf(pluginName) + pluginName.length());
		File transFile = new File(pathName);
		
		
		List<File> filesList = new ArrayList<File>();
		
		final String matchDir = pluginName + ".nl_";
		for (File flangDir : transDir.listFiles()){
			
			
			if (!flangDir.isDirectory()){
				continue;
			}
			File[] transToMerge = flangDir.listFiles(new FilenameFilter(){

				@Override
				public boolean accept(File arg0, String name) {
					return name.startsWith(matchDir);
				}});
			for(File tmp : transToMerge){
				filesList.add(tmp);
			}
			
		}
		
		for (File ft : filesList){
			String langCode = ft.getName().substring(ft.getName().indexOf('_')+1);
			int index2 = transFile.getName().lastIndexOf('.');
			final String prefix = transFile.getName().substring(0, index2);
			final String postfix = transFile.getName().substring(index2 + 1);
			File toMerge = new File(ft.getCanonicalPath()+  transFile.getParent()  + File.separator + prefix + "_" + langCode + "." + postfix);
			
//			System.out.println(f.toString());
//			System.out.println(toMerge.toString());
			
			if (!f.exists() || !toMerge.exists()){
				System.err.println("Error either source or target file does not exists. " + f.toString() + "  |  " + toMerge.toString());
			}else{
				System.out.println(" merging: " + toMerge);
				mergeFile(f, toMerge);
			}	
		}
	}

	/*
	 * Processes a file and its matched i18n files
	 */
	private void mergeFile(File sourceFile, File targetFile) throws Exception {
		boolean changes = false;
		
		HashMap<String, String> source = readFile(sourceFile);
		HashMap<String, String> target = readFile(targetFile);
		
		for (Entry<String, String> e : source.entrySet()){
			if (!target.containsKey(e.getKey())){
				System.out.println("add: " + e.getKey());
				target.put(e.getKey(), e.getValue());
				changes = true;
			}	
		}
		
		List<String> toRemove = new ArrayList<String>();
		for (Entry<String, String> e : target.entrySet()){
			if (!source.containsKey(e.getKey())){
				//this key no longer exists so we can remove it
				System.err.println("Remove: " + e.getKey());
				toRemove.add(e.getKey());
			}
			
		}
		for (String key : toRemove){
			target.remove(key);
			changes = true;
		}
		
		if (changes){
			System.out.println("writing file: " + targetFile.toString());
			writeFile(targetFile, target);
		}
	}

	
	/*
	 * reads i18n properties file
	 */
	private HashMap<String, String> readFile(File f) throws Exception {
		Properties prop = new Properties();
		FileReader fr = new FileReader(f);
		prop.load(fr);
		fr.close();
		
		HashMap<String, String> results = new HashMap<String, String>();
		for (Object s : prop.keySet()){
			results.put(s.toString(), prop.getProperty(s.toString()));
		}
		
		return results;
	}
	
	/*
	 * reads i18n properties file
	 */
	private void writeFile(File f, HashMap<String, String> values) throws Exception {
//		Properties prop = new Properties();
//		FileWriter fr = new FileWriter(f);
//		prop.putAll(values);
//		prop.store(fr, null);
//		fr.close();
//		
		System.out.println("Writing " + f.getPath());
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
		writer.write("#Auto generated from conversion files on " + DateFormat.getDateTimeInstance().format(new Date()));
		writer.write(Packagei18n.LINE_SEP);
		TreeSet<String> keys = new TreeSet<String>(values.keySet());
		for (String key : keys){
			String value = values.get(key);
			value = value.replaceAll("\\r\\n|\\r|\\n", "\\\\n");
			writer.write(key + "=" + value);
			writer.write(Packagei18n.LINE_SEP);
		}
		writer.close();
	}

	public static void main(String args[]) {
		Mergei18n util = new Mergei18n();
		try{
			util.findFiles(IN_DIR);
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}
}
