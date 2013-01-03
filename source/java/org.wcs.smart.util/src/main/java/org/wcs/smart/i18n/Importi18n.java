package org.wcs.smart.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import au.com.bytecode.opencsv.CSVReader;

/**
 * Tool for importing a multiple csv files that contain
 * various plugin i18n messages.  Each csv file represents
 * a single set of i18n files - one column for each language.
 * <br>
 * First line contains header.  Second line
 * location of base properties i18n file.
 * 
 * @author egouge
 *
 */
public class Importi18n {

	
	private void processCsv(File f) throws Exception{
		FileReader fr = new FileReader(f);
		BufferedReader tmp = new BufferedReader(fr);
		String baseLocation = "";
		try{
			tmp.readLine();
			baseLocation = tmp.readLine();
		}finally{
			fr.close();
		}
		InputStreamReader isr = new InputStreamReader(new FileInputStream(f), "UTF-8");
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
			String base = line[1].trim();
			baselang.put(key, base);
			for (int i = 0; i < headers.length; i ++){
				String value = line[i+2].trim();
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
		
		processData(baseLocation, baselang, headers, langs);
		
		
	}
	
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
		
		//verify that the number of entries is the same
		Properties existingData = new Properties();
		InputStreamReader isr = new InputStreamReader(new FileInputStream(baseFile));
		existingData.load(isr);
		isr.close();
		
		if (existingData.size() != baseLang.size()){
			throw new Exception("Base file " + baseFile.toString() + " has a different number of entries (" + existingData.size() + ") from new data file (" + baseLang.size() + "). Update not completed.");
		}
		
		writeProperties(baseFile, baseLang);		
		for (int i = 0; i < otherLangs.length; i ++){
			File output = new File(prefix + "_" + headers[i] + "." + suffix);
			writeProperties(output, otherLangs[i]);
		}
		
	}
	
	/*
	 * cannot use java Property class as it does not
	 * deal with the \ properly for ascii representation
	 * of native code
	 */
	private void writeProperties(File f, HashMap<String,String> map) throws Exception{
		System.out.println("Writing " + f.getPath());
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
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
	
	private Properties createSortedProperties(){
		return new Properties(){
			@Override
			public Set<Object> keySet(){
				return Collections.unmodifiableSet(new TreeSet<Object>(super.keySet()));
			}
			public synchronized Enumeration<Object> keys(){
				return Collections.enumeration(new TreeSet<Object>(super.keySet()));
			}
		};
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
