package org.wcs.smart.connect.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Mergei18n {

//	public static final String[] languages = new String[]{"ar", "es", "ka", "pt", "th"};
	public static final String[] languages = new String[] { "ar", "es", "fr", "hi", "id", "ka", "kar", "km", "lo", "mn",
			"ms", "my", "pt", "ru", "sw", "tg", "th", "uk", "vi", "zh" };

	public static final String ROOT = "C:\\data\\SMART\\Source\\Trunk\\svn\\connect\\org.wcs.smart.connect.server";

	public static final String ZIP_OUT = "C:\\temp\\smarti18n\\";

	private void processWebMessages() throws IOException {
		System.out.println("Web Processing Messages");
		String path = ROOT + "\\src\\main\\java\\org\\wcs\\smart\\connect\\i18n\\";
		String enFile = path + "web_messages_en.properties";
		String[] tFiles = new String[languages.length];
		for (int i = 0; i < languages.length; i++) {
			tFiles[i] = path + "web_messages_" + languages[i] + ".properties";
		}

		processFiles(enFile, tFiles);
	}

	private void processMessages() throws IOException {
		System.out.println("Processing Messages");
		String path = ROOT + "\\src\\main\\java\\org\\wcs\\smart\\connect\\i18n\\";
		String enFile = path + "messages.properties";
		String[] tFiles = new String[languages.length];
		for (int i = 0; i < languages.length; i++) {
			tFiles[i] = path + "messages_" + languages[i] + ".properties";
		}

		processFiles(enFile, tFiles);
	}

	private void processFiles(String enFile, String[] translationFiles) throws IOException {
		HashMap<String, String> values = new HashMap<>();

		Path p = Paths.get(enFile);
		try (BufferedReader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				if (line.trim().isEmpty())
					continue;
				int index = line.indexOf("=");
				if (index < 0) {
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					System.exit(-999);
					continue;
				}
				String key = line.substring(0, index);
				String value = line.substring(index + 1);
				values.put(key, value);
				System.out.println(line);
			}
		}

		for (String langFile : translationFiles) {
			HashMap<String, String> writevalues = new HashMap<>();

			Path langpath = Paths.get(langFile);
			if (!Files.exists(langpath)) {
				System.out.println("FILE NOT FOUND: " + langpath.toString());
				Files.createFile(langpath);
				// continue;
			}
			try (BufferedReader reader = Files.newBufferedReader(langpath)) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.trim().isEmpty())
						continue;
					int index = line.indexOf("=");
					if (index < 0) {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						System.exit(-999);
						continue;
					}
					String key = line.substring(0, index);
					String value = line.substring(index + 1);

					// if key doesn't exist then we remove it
					if (values.containsKey(key)) {
						writevalues.put(key, value);
					} else {
						System.out.println("removing: " + key);
					}
				}
			}

			for (String key : values.keySet()) {
				if (!writevalues.containsKey(key)) {
					System.out.println("adding:" + key);
					// writevalues.put(key, "**NEW**" + values.get(key));
					writevalues.put(key, values.get(key));
				}
			}

			// write file
			ArrayList<String> keys = new ArrayList<String>();
			keys.addAll(writevalues.keySet());
			keys.sort((a, b) -> a.compareTo(b));
			try (BufferedWriter writer = Files.newBufferedWriter(langpath)) {
				for (String key : keys) {
					String value = writevalues.get(key);
					writer.append(key);
					writer.append("=");
					writer.append(value);
					writer.append("\n");
				}
			}

		}

	}

	private void processJavascript() throws IOException {
		System.out.println("Javascript Messages");
		String path = ROOT + "\\src\\main\\webapp\\javascript\\i18n\\";
		String enFile = path + "labels_en.js";
		String[] tFiles = new String[languages.length];
		for (int i = 0; i < languages.length; i++) {
			tFiles[i] = path + "labels_" + languages[i] + ".js";
		}

		// read en file
		Path p = Paths.get(enFile);
//		StringBuilder json = new StringBuilder();

		HashMap<String, String> values = new HashMap<>();
		try (BufferedReader reader = Files.newBufferedReader(p)) {
			String line = null;
			reader.readLine(); // skip the first labels_en line

			while ((line = reader.readLine()) != null) {
				if (line.trim().isEmpty())
					continue;
				if (line.trim().equalsIgnoreCase("}"))
					continue;
				int index = line.indexOf(":");
				if (index < 0) {
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					System.exit(-999);
					continue;
				}
				String key = line.substring(0, index).trim();
				String value = line.substring(index + 1).trim();
				if (value.charAt(value.length() - 1) == ',') {
					value = value.substring(0, value.length() - 1);
					value = value.trim();
				}
				// trim quotes
				if (key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"') {
					key = key.substring(1, key.length() - 1);
				} else {
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					System.exit(-999);
					continue;
				}
				if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
					value = value.substring(1, value.length() - 1);
				} else {
					System.err.println("ERROR: invalid line: " + index + ":" + line);
					System.exit(-999);
					continue;
				}
				if (key.isEmpty())
					continue;

				values.put(key, value);
			}
		}

		for (int i = 0; i < languages.length; i++) {
			String tFile = tFiles[i];

			HashMap<String, String> newvalues = new HashMap<>();
			Path langPath = Paths.get(tFile);

			if (!Files.exists(langPath)) {
				System.out.println("FILE NOT FOUND: " + langPath.toString());
				Files.createFile(langPath);
				// continue;
			}

			try (BufferedReader reader = Files.newBufferedReader(langPath)) {
				String line = null;
				reader.readLine(); // skip the first labels_en line

				while ((line = reader.readLine()) != null) {
					if (line.trim().isEmpty())
						continue;
					if (line.trim().equalsIgnoreCase("}"))
						continue;
					int index = line.indexOf(":");
					if (index < 0) {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						System.exit(-999);
						continue;
					}
					String key = line.substring(0, index).trim();
					String value = line.substring(index + 1).trim();
					if (value.charAt(value.length() - 1) == ',') {
						value = value.substring(0, value.length() - 1);
						value = value.trim();
					}
					// trim quotes
					if (key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"') {
						key = key.substring(1, key.length() - 1);
					} else {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						System.exit(-999);
						continue;
					}
					if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
						value = value.substring(1, value.length() - 1);
					} else {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						System.exit(-999);
						continue;
					}
					if (key.isEmpty())
						continue;
					// if key doesn't exist then we remove it
					if (values.containsKey(key)) {
						newvalues.put(key, value);
					} else {
						System.out.println("removing: " + key);
					}
				}
			}

			for (String key : values.keySet()) {
				if (!newvalues.containsKey(key)) {
					System.out.println("adding:" + key);
					// newvalues.put(key, "**NEW**" + values.get(key));
					newvalues.put(key, values.get(key));
				}
			}

			// write file
			ArrayList<String> keys = new ArrayList<String>();
			keys.addAll(newvalues.keySet());
			keys.sort((a, b) -> a.compareTo(b));
			boolean first = true;
			try (BufferedWriter writer = Files.newBufferedWriter(langPath)) {
				writer.append("labels_" + languages[i] + " = {\n");
				for (String key : keys) {

					String value = newvalues.get(key);
					if (!first) {
						writer.append(",");
						writer.append("\n");
					}
					first = false;
					writer.append(" \"");
					writer.append(key);
					writer.append("\"");
					writer.append(": ");
					writer.append("\"");
					writer.append(value);
					writer.append("\"");
				}
				writer.append("\n");
				writer.append("}");
			}
		}
	}

	private void convertAndZipByLanguage() throws IOException {

		// we want to convert the javascript from json to properties

		String path = ROOT + "\\src\\main\\webapp\\javascript\\i18n\\";
		String enFile = path + "labels_en.js";
		String[] tFiles = new String[languages.length];
		for (int i = 0; i < languages.length; i++) {
			tFiles[i] = path + "labels_" + languages[i] + ".js";
		}

		HashMap<String, Path[]> filesbylang = new HashMap<>();

		Path tempDir = Files.createTempDirectory("connecti18n");

		for (int i = 0; i < languages.length; i++) {
			String lang = languages[i];
			filesbylang.put(lang, new Path[3]);

			Path langPath = Paths.get(tFiles[i]);

			SortedProperties prop = new SortedProperties();

			try (BufferedReader reader = Files.newBufferedReader(langPath)) {
				String line = null;
				reader.readLine(); // skip the first labels_en line

				while ((line = reader.readLine()) != null) {
					if (line.trim().isEmpty())
						continue;
					if (line.trim().equalsIgnoreCase("}"))
						continue;
					int index = line.indexOf(":");
					if (index < 0) {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						System.exit(-999);
						continue;
					}
					String key = line.substring(0, index).trim();
					String value = line.substring(index + 1).trim();
					if (value.charAt(value.length() - 1) == ',') {
						value = value.substring(0, value.length() - 1);
						value = value.trim();
					}
					// trim quotes
					if (key.charAt(0) == '"' && key.charAt(key.length() - 1) == '"') {
						key = key.substring(1, key.length() - 1);
					} else {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						System.exit(-999);
						continue;
					}
					if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
						value = value.substring(1, value.length() - 1);
					} else {
						System.err.println("ERROR: invalid line: " + index + ":" + line);
						System.exit(-999);
						continue;
					}
					if (key.isEmpty())
						continue;

					prop.setProperty(key, value);
				}
			}

			Path jout = tempDir.resolve(langPath.getFileName().toString());
			filesbylang.get(lang)[0] = jout;

			try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(jout),
					StandardCharsets.UTF_8)) {
				prop.store(writer, "Auto generated from conversion file on "
						+ DateFormat.getDateTimeInstance().format(new Date()));
			}

		}
		path = ROOT + "\\src\\main\\java\\org\\wcs\\smart\\connect\\i18n\\";
		for (int i = 0; i < languages.length; i++) {
			Path file = Paths.get(path + "web_messages_" + languages[i] + ".properties");
			filesbylang.get(languages[i])[1] = file;

			Path file2 = Paths.get(path + "messages_" + languages[i] + ".properties");
			filesbylang.get(languages[i])[2] = file2;

		}
		// zip files to output
		for (String lang : filesbylang.keySet()) {
			Path zip = Paths.get(ZIP_OUT + lang + "_connect.zip");

			try (OutputStream fOut = Files.newOutputStream(zip);
					BufferedOutputStream bOut = new BufferedOutputStream(fOut);
					ZipOutputStream zOut = new ZipOutputStream(bOut);) {

				for (Path p : filesbylang.get(lang)) {
					String entryName = p.getFileName().toString();
					ZipEntry zipEntry = new ZipEntry(entryName);
					zOut.putNextEntry(zipEntry);
					try (InputStream in = Files.newInputStream(p)) {
						in.transferTo(zOut);
					}
					zOut.closeEntry();
				}
			}
		}

	}

	public static void main(String args[]) throws IOException {
		Mergei18n merger = new Mergei18n();
		merger.processMessages();
		merger.processWebMessages();
		merger.processJavascript();
		
		merger.convertAndZipByLanguage();
	}
}
