package org.wcs.smart.connect.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;


public class Mergei18n {

	public static final String[] languages = new String[]{"in"};
	
	private static final String WEB_MESSAGES = "C:\\data\\SMART\\Source\\Version5.0.0\\connect\\org.wcs.smart.connect.server\\src\\main\\resources\\org\\wcs\\smart\\connect\\i18n\\";
	private static final String MESSAGES = "C:\\data\\SMART\\Source\\Version5.0.0\\connect\\org.wcs.smart.connect.server\\src\\main\\resources\\org\\wcs\\smart\\connect\\i18n\\";
	private static final String JAVASCRIPT = "C:\\data\\SMART\\Source\\Version5.0.0\\connect\\org.wcs.smart.connect.server\\src\\main\\webapp\\javascript\\i18n\\";
	
	private void processWebMessages() throws IOException{
		System.out.println("Web Processing Messages");
		String path = WEB_MESSAGES;
		String enFile = path + "web_messages_en.properties";
		String[] tFiles = new String[languages.length];
		for (int i = 0; i < languages.length; i ++){
			tFiles[i] = path + "web_messages_" + languages[i] + ".properties";
		}
		
		processFiles(enFile, tFiles);
	}
	
	private void processMessages() throws IOException{
		System.out.println("Processing Messages");
		String path = MESSAGES;
		String enFile = path + "messages.properties";
		String[] tFiles = new String[languages.length];
		for (int i = 0; i < languages.length; i ++){
			tFiles[i] = path + "messages_" + languages[i] + ".properties";
		}
		
		processFiles(enFile, tFiles);
	}
	
	private void processFiles(String enFile, String[] translationFiles) throws IOException{
		HashMap<String, String> values = new HashMap<>();
		
		Path p = Paths.get(enFile);
		try(BufferedReader reader = Files.newBufferedReader(p)){
			String line = null;
			while((line = reader.readLine()) != null){
				if (line.trim().isEmpty()) continue;
				int index = line.indexOf("=");
				if (index < 0) {
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					continue;
				}
				String key = line.substring(0, index);
				String value = line.substring(index + 1);
				values.put(key, value);
			}
		}
		
		for (String langFile: translationFiles){
			HashMap<String, String> writevalues = new HashMap<>();
			
			Path langpath = Paths.get(langFile);
			try(BufferedReader reader = Files.newBufferedReader(langpath)){
				String line = null;
				while((line = reader.readLine()) != null){
					if (line.trim().isEmpty()) continue;
					int index = line.indexOf("=");
					if (index < 0) {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						continue;
					}
					String key = line.substring(0, index);
					String value = line.substring(index + 1);
					
					//if key doesn't exist then we remove it
					if (values.containsKey(key)){
						writevalues.put(key, value);
					}else{
						System.out.println("removing: " + key);
					}
				}
			}
			for (String key : values.keySet()){
				if (!writevalues.containsKey(key)){
					System.out.println("adding:" + key);
					writevalues.put(key, "**NEW**" + values.get(key));
				}
			}
				
				
			//write file
			ArrayList<String> keys = new ArrayList<String>();
			keys.addAll(writevalues.keySet());
			keys.sort((a,b)->a.compareTo(b));
			try(BufferedWriter writer = Files.newBufferedWriter(langpath)){
				for (String key : keys){
					String value = writevalues.get(key);
					writer.append(key);
					writer.append("=");
					writer.append(value);
					writer.append("\n");
				}
			}
			
		}
		
	}
	
	private void processJavascript() throws IOException{
		System.out.println("Javascript Messages");
		String path = JAVASCRIPT;
		String enFile = path + "labels_en.js";
		String[] tFiles = new String[languages.length];
		for (int i = 0; i < languages.length; i ++){
			tFiles[i] = path + "labels_" + languages[i] + ".js";
		}
		
		//read en file
		Path p = Paths.get(enFile);
		StringBuilder json = new StringBuilder();
		
		HashMap<String, String> values = new HashMap<>();
		try(BufferedReader reader = Files.newBufferedReader(p)){
			String line = null;
			reader.readLine();	//skip the first labels_en line
			
			while((line = reader.readLine()) != null){
				if (line.trim().isEmpty()) continue;
				if (line.trim().equalsIgnoreCase("}")) continue;
				int index = line.indexOf(":");
				if (index < 0) {
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					continue;
				}
				String key = line.substring(0, index).trim();
				String value = line.substring(index + 1).trim();
				if (value.charAt(value.length() - 1) == ','){
					value = value.substring(0, value.length() - 1);
					value = value.trim();
				}
				//trim quotes
				if (key.charAt(0) == '"' && key.charAt(key.length()-1) == '"'){
						key = key.substring(1,  key.length() - 1);
				}else{
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					continue;
				}
				if (value.charAt(0) == '"' && value.charAt(value.length()-1) == '"'){
					value = value.substring(1,  value.length() - 1);
				}else{
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					continue;
				}
				if (key.isEmpty()) continue;
				
				values.put(key, value);
			}
		}
		
		for (int i = 0; i < languages.length; i ++){
			String tFile = tFiles[i];
		
			HashMap<String, String> newvalues = new HashMap<>();
			Path langPath = Paths.get(tFile);
			try(BufferedReader reader = Files.newBufferedReader(langPath)){
				String line = null;
				reader.readLine();	//skip the first labels_en line
				
				while((line = reader.readLine()) != null){
					if (line.trim().isEmpty()) continue;
					if (line.trim().equalsIgnoreCase("}")) continue;
					int index = line.indexOf(":");
					if (index < 0) {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						continue;
					}
					String key = line.substring(0, index).trim();
					String value = line.substring(index + 1).trim();
					if (value.charAt(value.length() - 1) == ','){
						value = value.substring(0, value.length() - 1);
						value = value.trim();
					}
					//trim quotes
					if (key.charAt(0) == '"' && key.charAt(key.length()-1) == '"'){
							key = key.substring(1,  key.length() - 1);
					}else{
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						continue;
					}
					if (value.charAt(0) == '"' && value.charAt(value.length()-1) == '"'){
						value = value.substring(1,  value.length() - 1);
					}else{
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						continue;
					}
					if (key.isEmpty()) continue;
					//if key doesn't exist then we remove it
					if (values.containsKey(key)){
						newvalues.put(key, value);
					}else{
						System.out.println("removing: " + key);
					}
				}
			}
			for (String key : values.keySet()){
				if (!newvalues.containsKey(key)){
					System.out.println("adding:" + key);
					newvalues.put(key, "**NEW**" + values.get(key));
				}
			}
				
				
			//write file
			ArrayList<String> keys = new ArrayList<String>();
			keys.addAll(newvalues.keySet());
			keys.sort((a,b)->a.compareTo(b));
			try(BufferedWriter writer = Files.newBufferedWriter(langPath)){
				writer.append("labels_" + languages[i] + " = {\n");
				for (String key : keys){
					String value = newvalues.get(key);
					writer.append(" \"");
					writer.append(key);
					writer.append("\"");
					writer.append(": ");
					writer.append("\"");
					writer.append(value);
					writer.append("\",");
					writer.append("\n");
				}
				writer.append("}\\n");
			}
		}
	}
	
	public static void main(String args[]) throws IOException{
		Mergei18n merger = new Mergei18n();
		merger.processMessages();
		merger.processWebMessages();
		merger.processJavascript();
	}
}

