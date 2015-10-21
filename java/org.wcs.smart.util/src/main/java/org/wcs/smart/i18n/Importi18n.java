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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tool for importing multiple csv files that contain
 * various plugin i18n messages.  Each csv file represents
 * a single set of i18n files - one column for each language.
 * <br>
 * First line contains header.  Second line
 * location of base properties i18n file.
 * <br>
 * When importing the base language keys are merged.  If the 
 * current code has a new key it is added to the import.  If the base
 * language has keys removed these are also removed from the import.
 * 
 * @author egouge
 *
 */
public class Importi18n {

	/**
	 * Process the given csv file.  This will contain
	 * a set of i18n translations for a plugin translation
	 * file.
	 * 
	 * @param f
	 * @throws Exception
	 */
	private void processCsv(File f) throws Exception{
		
		// Read CSV File
		FileReader fr = new FileReader(f);
		BufferedReader tmp = new BufferedReader(fr);
		String baseLocation = "";
		try{
			tmp.readLine();
			baseLocation = tmp.readLine();
		}finally{
			fr.close();
		}
		InputStreamReader isr = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
		CSVReader reader = new CSVReader(isr);
	
		/* header line */
		String[] allheaders = reader.readNext();
		String[] headers = new String[allheaders.length - 2];
		for (int i = 2; i < allheaders.length;i++){
			headers[i-2] = allheaders[i];
		}
		
		/*base location line*/
		reader.readNext();
		
		String[] line = null;
		
		HashMap<String, String> baselang = new HashMap<String, String>();
		HashMap<String, String>[] langs = new HashMap[headers.length];
		for (int i = 0; i < langs.length; i ++){
			langs[i] = new HashMap<String, String>();
		}
		while ((line = reader.readNext()) != null){
			String key = line[0].trim();
			String base = line[1];
			baselang.put(key, base);
			for (int i = 0; i < headers.length; i ++){
				String value = line[i+2];
				if (value.length() > 0){
					value = ConversionUtils.nativeToAscii(value);
				}
				langs[i].put(key,  value);
			}
		}
		if (headers.length != langs.length){
			throw new Exception("Invalid match between headers and column data.");
		}
		reader.close();
		
		//process Data
		processData(baseLocation, baselang, headers, langs);
	}
	
	/**
	 * Processes the csv data
	 * @param root 
	 * @param baseLang
	 * @param headers
	 * @param otherLangs
	 * @throws Exception
	 */
	private void processData(String root, HashMap<String, String> baseLang, 
			String[] headers, 
			HashMap<String, String>[] otherLangs) throws Exception{
		
		File baseFile =  new File(Packagei18n.IN_DIR, root);
		if (!baseFile.exists()){
			throw new Exception("File " + baseFile.toString() + " does not exist.");
		}
		
		int index = baseFile.getCanonicalPath().lastIndexOf('.');
		String prefix = baseFile.getCanonicalPath().substring(0, index);
		String suffix = baseFile.getCanonicalPath().substring(index + 1);
		
		//read current properties file and merge data
		Properties existingData = new Properties();
		InputStreamReader isr = new InputStreamReader(new FileInputStream(baseFile));
		existingData.load(isr);
		isr.close();
		mergeValues(existingData, baseLang, otherLangs);
		
		//write results
		writeProperties(baseFile, baseLang);		
		for (int i = 0; i < otherLangs.length; i ++){
			File output = new File(prefix + "_" + headers[i] + "." + suffix);
			writeProperties(output, otherLangs[i]);
		}
		
	}
	
	/*
	 * Merges new values with existing values removing or adding keys 
	 * as required
	 */
	private void mergeValues(Properties existing, HashMap<String, String> base, 
			HashMap<String, String>[] other){
		
		/* check for keys added */
		for (Object x : existing.keySet()){
			String str = (String)x;
			if (!base.containsKey(str)){
				//new key added since extra; be sure to add to base
				//we don't have translations so we won't add to translations
				base.put(str, existing.getProperty(str));
			}
		}
		
		/* check for keys removed */
		List<String> keysToRemove = new ArrayList<String>();
		for (String key : base.keySet()){
			if (!existing.containsKey(key)){
				//key has been removed
				keysToRemove.add(key);
			}
		}
		for (String key : keysToRemove){
			base.remove(key);
			for (int i = 0; i < other.length; i ++){
				other[i].remove(key);
			}
		}
	}
	
	/*
	 * cannot use java Property class as it does not
	 * deal with the \ properly for ascii representation
	 * of native code
	 */
	private void writeProperties(File f, HashMap<String,String> map) throws Exception{
		System.out.println("Writing " + f.getPath());
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
		writer.write("#Auto generated from conversion files on " + DateFormat.getDateTimeInstance().format(new Date()));
		writer.write(Packagei18n.LINE_SEP);
		TreeSet<String> keys = new TreeSet<String>(map.keySet());
		for (String key : keys){
			String value = map.get(key);
			value = value.replaceAll("\\r\\n|\\r|\\n", "\\\\n");
			writer.write(key + "=" + value);
			writer.write(Packagei18n.LINE_SEP);
		}
		writer.close();
		
	}
	
	
	public static void main(String args[]){
		File f = new File(Packagei18n.OUT_DIR);
		File[] toProcess = f.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".csv");
			}
		});
		
		Importi18n importer = new Importi18n();
		for (int i = 0; i < toProcess.length; i ++){
			System.out.println("Processing " + toProcess[i] + "   " + i + "/" + toProcess.length);
			try{
				importer.processCsv(toProcess[i]);
			}catch (Exception ex){
				ex.printStackTrace();
			}
		}
	}
	
}
