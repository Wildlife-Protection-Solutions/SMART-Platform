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
import java.util.Map.Entry;
import java.util.Properties;
/**
 * Matches i18n resource property files in fragments with the
 * default property file and removes any no longer used key/value pairs.  It
 * will also add missing key/value pairs (with the value using the
 * English language).
 *
 * Used for 6.2 and above, when all translations were merged into a single
 * project file.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("nls")
public class Mergei18nNew {
	
	private static final String ROOT = "C:\\data\\SMART\\Source\\Trunk\\";

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
	
//    public static final String[] LANGUAGES =  new String[] {"ar", "es","fr", "hi","in","ka","kar","km","lo","mn","ms","ru","sw","th","vi","zh","pt"};
    public static final String[] LANGUAGES =  new String[] {"es", "pt"};
    
    public static final String LINE_SEP = "\n";

    public static final String NATIVE2ASCII = "C:\\Java\\jdk1.8.0_201\\bin\\native2ascii.exe";

    /**
     * find all plugin.properties, messages.properties or bundle.properties
     * files and process each file.
     *
     * @param srcDir
     * @return
     * @throws Exception
     */
    public List<Path> findFiles(String srcDir, String transDir) throws Exception {
    	
    	Path start = Paths.get(srcDir);
    	
    	List<Path> files = new ArrayList<>();
    	
    	Files.walkFileTree(start, new FileVisitor<Path>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.getFileName().toString().equals("bin")) return FileVisitResult.SKIP_SUBTREE;
				if (dir.getFileName().toString().equals("target")) return FileVisitResult.SKIP_SUBTREE;
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String name = file.getFileName().toString();
				if (name.equals("plugin.properties") || name.equals("messages.properties") || 
						name.equals("bundle.properties")) {
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
            System.out.println("Processing: " +f.toString() + "  " + (i++) + "/" + files.size() );
            processFile(f, transDir);
        }

        return files;
    }

    /*
     * Processes a base file, looking for matching i18n files
     * and merging matched files.
     */
    private void processFile(Path enFile, String stransDir) throws Exception {
       
        int index = enFile.toString().indexOf("org.wcs.smart");
        String pluginName = enFile.toString().substring(index);
        index = pluginName.indexOf(File.separator);
        pluginName = pluginName.substring(0, index);
        String pathName = enFile.toString().substring(enFile.toString().indexOf(pluginName) + pluginName.length());
        
        Path transFile = Paths.get(pathName);

        final String matchDir = pluginName + ".nl";
        
        
        Path translationsPath = Paths.get(stransDir).resolve(matchDir).resolve(pathName.substring(1)).getParent();
        
        int index2 = transFile.getFileName().toString().lastIndexOf('.');
        final String prefix = transFile.getFileName().toString().substring(0, index2);
        final String postfix = transFile.getFileName().toString().substring(index2 + 1);
            
        for (String langCode : LANGUAGES) {
        	Path toMerge = translationsPath.resolve(prefix + "_" + langCode + "." + postfix);

        	if (!Files.exists(enFile) || !Files.exists(toMerge)){
           		System.err.println("Error either source or target file does not exists. " + enFile.toString() + "  |  " + toMerge.toString());
           	}else{
           		mergeFile(enFile, toMerge);
           	}
        }
    }

    /*
     * Processes a file and its matched i18n files
     */
    private void mergeFile(Path sourceFile, Path targetFile) throws Exception {
        boolean changes = false;

        HashMap<String, String> source = readFile(sourceFile);
        HashMap<String, String> target = readFile(targetFile);

        for (Entry<String, String> e : source.entrySet()){
            if (!target.containsKey(e.getKey())){
//                System.out.println("add: " + e.getKey());
//                target.put(e.getKey(), e.getValue());
                target.put(e.getKey(), "**NEW**" + e.getValue());
                changes = true;
            }
        }

        List<String> toRemove = new ArrayList<String>();
        for (Entry<String, String> e : target.entrySet()){
            if (!source.containsKey(e.getKey())){
                //this key no longer exists so we can remove it
                System.out.println("Remove: " + e.getKey());
                toRemove.add(e.getKey());
            }

        }
        for (String key : toRemove){
            target.remove(key);
            changes = true;
        }

        if (changes){
            writeFile(targetFile, target);
        }
    }


    /*
     * reads i18n properties file
     */
    private HashMap<String, String> readFile(Path f) throws Exception {
        Properties prop = new Properties();
        try(InputStream fr = Files.newInputStream(f)){
        	prop.load(fr);
        }

        HashMap<String, String> results = new HashMap<String, String>();
        for (Object s : prop.keySet()){
            results.put(s.toString(), prop.getProperty(s.toString()));
        }

        return results;
    }

    /*
     * reads i18n properties file
     */
    private void writeFile(Path f, HashMap<String, String> values) throws Exception {
        System.out.println("Writing " + f.toString() );
        
        SortedProperties properties = new SortedProperties();
        for(String key : values.keySet()){
            properties.put(key, values.get(key));
        }


        Path tempFile = f.getParent().resolve(f.getFileName().toString() + ".temp");
        
        try(OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(tempFile), StandardCharsets.UTF_8)){
        	properties.store(writer, "Auto generated from conversion file on " + DateFormat.getDateTimeInstance().format(new Date()));
        }

        String cmd = "\"" + NATIVE2ASCII + "\" -encoding utf8 \"" + tempFile.toString() + "\" " + f.toString() + "";
        System.out.println(cmd);
        Process pr = Runtime.getRuntime().exec(cmd);
        InputStream is = pr.getInputStream();
        while(is.read() != -1){}
        is = pr.getErrorStream();
        while(is.read() != -1){}

        Files.delete(tempFile);
    }

    public static void main(String args[]) {
        Mergei18nNew util = new Mergei18nNew();
        try{
        	for (int i = 0; i < IN_DIR.length; i ++){
        		util.findFiles(IN_DIR[i], TRANS_DIR[i]);
        	}
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}