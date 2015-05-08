package org.wcs.smart;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A tool to scan directories and determine which i18n files and added that shouldn't be
 * and which should be.
 * 
 * @author Emily
 *
 */
public class Dependencies {

	
	
	public static void main(String[] args) throws Exception{
		
		File toSearch = new File("C:\\data\\SMART\\Exports\\win32.win32.x86\\smart\\plugins");
		String[] files2 = toSearch.list();
		
		List<String> files = new ArrayList<String>();
		
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\es\\org.wcs.smart.feature.nl.es\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\fr\\org.wcs.smart.feature.nl.fr\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\ru\\org.wcs.smart.feature.nl.ru\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\th\\org.wcs.smart.feature.nl.th\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\zh\\org.wcs.smart.feature.nl.zh\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\vi\\org.wcs.smart.feature.nl.vi\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\in\\org.wcs.smart.feature.nl.in\\feature.xml";
		
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\hi\\org.wcs.smart.feature.nl.hi\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\km\\org.wcs.smart.feature.nl.km\\feature.xml";
//		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\ms\\org.wcs.smart.feature.nl.ms\\feature.xml";
		String myfile = "C:\\data\\SMART\\Source\\trunk\\source\\translations\\lo\\org.wcs.smart.feature.nl.lo\\feature.xml";
		
		String file = (new Scanner(new File(myfile))).useDelimiter("\\Z").next();
		
		// <plugin id="org.wcs.smart.nl_fr"
		Pattern ptn = Pattern.compile("plugin\\s*id=\"(.*).nl_.*\"");
		
		Matcher matcher = ptn.matcher(file);
		while(matcher.find()){
			files.add(matcher.group(1));
			//System.out.println(matcher.group(1));
		}
		
		System.out.println("CONFIG TO BUILD");
		for (String f : files){
			boolean found = false;
			for (String f2 : files2){
				if (f2.startsWith(f  + "_")){
					found = true;
					//System.out.println("FOUND: " + f);
					break;
				}
			}
			if (!found){
				System.out.println("NOT FOUND: " + f);
			}
			
		}
		
		List<String> notfound = new ArrayList<String>();
		System.out.println("BUILD TO CONFIG");
		for (String f : files2){
			boolean found = false;
			for (String f2 : files){
				//System.out.println(f.substring(0, f.indexOf("_")));
				if (f2.startsWith(f.substring(0, f.indexOf("_")-1))){
					found = true;
					//System.out.println("FOUND: " + f);
					break;
				}
			}
			if (!found){
				//System.out.println("NOT FOUND: " + f);
				notfound.add(f);
			}
			
		}
		System.out.println("SEARCH FOR ITEMS TO ADD ");
		File allLocation = new File("C:\\data\\SMART\\Eclipse4\\smart-eclipe-target\\plugins");
		String[] mytoSearch = allLocation.list();
		for (String f : notfound){
			String search =f.substring(0, f.indexOf("_"));
			for (String f2 : mytoSearch){
				if (f2.startsWith(search + ".nl_")){
					
					System.out.println("<plugin id=\"" + search + ".nl_lo\" download-size=\"0\" install-size=\"0\" version=\"0.0.0\" fragment=\"true\" unpack=\"false\"/>");
					break;
				}
				
			}
			
		}
	}
	


}
