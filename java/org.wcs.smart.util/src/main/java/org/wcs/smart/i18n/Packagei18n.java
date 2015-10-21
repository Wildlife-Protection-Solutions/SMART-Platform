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
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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

/**
 * A utility for ready all i18n files from the SMART 
 * software and writing the results to a single utf-8 encoded
 * output file.
 * <br>
 * It searches through all files in the IN_DIR location for files
 * with the name messages.properties, plugin.properties or bundle.properties.
 * <br>
 * For each of the above files it finds, it searches for corresponding 
 * i18n files (ie messages_fr.properties)
 * and merges all corresponding i18n files with the base file (messages.properties)
 * to produce a single output file that contains all translations.
 * <br>
 * Note: The first line of the output file is the header line, the second
 * contains the path to the base file (excluding the IN_DIR part).  This
 * is to allow import back into the system.
 *  
 * 
 * @author egouge
 *
 */
public class Packagei18n {

	public static final String IN_DIR = "C:\\data\\SMART\\Source\\Version1\\trunk\\source\\java\\";
	public static final String OUT_DIR = "C:\\temp\\smarti18n\\";
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
		
		File dir = f.getParentFile();
		int index = f.getName().lastIndexOf('.');
		final String prefix = f.getName().substring(0, index);
		final String postfix = f.getName().substring(index + 1);

		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(prefix + "_.+\\." + postfix);

			}
		});

		processFile(f, files);
	}

	/*
	 * Processes a file and its matched i18n files
	 */
	private void processFile(File f, File[] matched) throws Exception {
		List<File> sortedMatched = new ArrayList<File>();
		for (int i = 0; i < matched.length; i ++){
			sortedMatched.add(matched[i]);	
		}
		
		Collections.sort(sortedMatched, new Comparator<File>() {
			@Override
			public int compare(File arg0, File arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}
		});
		String[] codes = new String[matched.length];
		
		HashMap<String, String>[] langs = new HashMap[matched.length];
		HashMap<String, String> main = readFile(f);
		int cnt = 0;
		for (File s : sortedMatched) {
			String name = s.getName();
			int i1 = name.lastIndexOf('_');
			int i2 = name.lastIndexOf('.');
			codes[cnt] = s.getName().substring(i1+1, i2);
			langs[cnt++] = readFile(s);
		}
		writeFile(f, main,codes, langs);
	}

	/*
	 * determines the name of the output csv file based on the file input name.
	 * Searches for the "org.wcs.smart" package
	 */
	private String determineOutFileName(File f) throws Exception{
		int index = f.getCanonicalPath().indexOf("org.wcs.smart");
		String part = f.getCanonicalPath().substring(index);
		index = part.indexOf(File.separator);
		part = part.substring(0, index);
		
		String key = f.getName().substring(0, f.getName().lastIndexOf('.'));
		
		return part + "." + key + ".csv";
	}
	
	/*
	 * writes the output csv file
	 */
	private void writeFile(File inf, HashMap<String, String> main, String[] langCodes,
			HashMap<String, String>[] others) throws Exception {
		
		//File f = new File("C:\\temp\\test_" + inf.getPath().replace(File.separator, ".") + "_out.csv");
		File f = new File(OUT_DIR, determineOutFileName(inf));
		//System.out.println("writing " + f.toString());
		FileOutputStream fs = new FileOutputStream(f);
		OutputStreamWriter writer = new OutputStreamWriter(fs, StandardCharsets.UTF_8);
		
		/* write header line */
		StringBuilder s = new StringBuilder();
		s.append("Key,Default");
		for (int i = 0; i < langCodes.length; i ++){
			s.append(",");
			s.append(langCodes[i]);
		}
		writer.write(s.toString());
		writer.write(LINE_SEP);
		
		/* write base file name */
		writer.write(inf.getPath().replace( (new File(IN_DIR)).getPath(), ""));
		writer.write(LINE_SEP);
		
		/* write data */
		try {
			TreeSet<String> keys = new TreeSet<String>(main.keySet());
			for (String key : keys) {
				StringBuilder sb = new StringBuilder();
				sb.append(key);
				sb.append(",");
				String value = main.get(key);
				if (value == null){
					value = "";
				}
				sb.append("\"" + main.get(key) + "\"");
				if (others.length > 0) {
					sb.append(",");
				}
				for (int i = 0; i < others.length; i++) {
					value = others[i].get(key);
					if (value == null) {
						value = "";
					}else{
						value = ConversionUtils.asciiToNative(value);
					}
					sb.append("\"" + value + "\"");
					if (i < others.length - 1) {
						sb.append(",");
					}
				}
				writer.write(sb.toString());
				writer.write(LINE_SEP);
			}
		} finally {
			//writer.flush();
			//fs.close();
			writer.close();
			//fs.close();
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

	public static void main(String args[]) {
		Packagei18n util = new Packagei18n();
		try{
			util.findFiles(IN_DIR);
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}
}
