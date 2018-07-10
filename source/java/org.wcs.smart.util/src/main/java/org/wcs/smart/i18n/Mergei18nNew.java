package org.wcs.smart.i18n;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
public class Mergei18nNew {

    public static final String IN_DIR[] = {"C:\\data\\SMART\\Source\\trunk\\source\\java",
    	
    	"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\asset",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\connect",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\cybertracker",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\entity",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\i2",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\event",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\qa",
//		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\r",
		
		
    };
    
    
    public static final String TRANS_DIR[] = {"C:\\data\\SMART\\Source\\trunk\\source\\translations\\",
    	
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\asset\\translations",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\connect\\translations",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\cybertracker\\translations",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\entity\\translations",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er\\translations",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\i2\\translations",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\event\\translations",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\qa\\translations",
//		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\r",
    };
	
    public static final String[] LANGUAGES =  new String[] {"es","fr", "hi","in","ka","kar","km","lo","mn","ms","ru","sw","th","vi","zh"};
    
    public static final String LINE_SEP = "\n";

    public static final String NATIVE2ASCII = "C:\\Java\\jdk1.6.0_38\\bin\\native2ascii.exe";

    /**
     * find all plugin.properties, messages.properties or bundle.properties
     * files and process each file.
     *
     * @param srcDir
     * @return
     * @throws Exception
     */
    public File[] findFiles(String srcDir, String transDir) throws Exception {
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
                if (f.getName().equals("bin") || f.getName().equals("target")){
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
            processFile(f, transDir);
        }

        return files.toArray(new File[files.size()]);
    }

    /*
     * Processes a base file, looking for matching i18n files
     * and merging matched files.
     */
    private void processFile(File enFile, String stransDir) throws Exception {
       
        int index = enFile.getCanonicalPath().indexOf("org.wcs.smart");
        String pluginName = enFile.getCanonicalPath().substring(index);
        index = pluginName.indexOf(File.separator);
        pluginName = pluginName.substring(0, index);
        String pathName = enFile.getCanonicalPath().substring(enFile.getCanonicalPath().indexOf(pluginName) + pluginName.length());
        
        Path transFile = Paths.get(pathName);

        final String matchDir = pluginName + ".nl";
        
        
        Path translationsPath = Paths.get(stransDir + File.separator + matchDir + File.separator + pathName).getParent();
        
        int index2 = transFile.getFileName().toString().lastIndexOf('.');
        final String prefix = transFile.getFileName().toString().substring(0, index2);
        final String postfix = transFile.getFileName().toString().substring(index2 + 1);
            
        for (String langCode : LANGUAGES) {
        	Path toMerge = translationsPath.resolve(prefix + "_" + langCode + "." + postfix);

        	if (!enFile.exists() || !Files.exists(toMerge)){
           		System.err.println("Error either source or target file does not exists. " + enFile.toString() + "  |  " + toMerge.toString());
           	}else{
           		mergeFile(enFile, toMerge.toFile());
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
                //System.out.println("add: " + e.getKey());
                target.put(e.getKey(), e.getValue());
                //target.put(e.getKey(), "**NEW**" + e.getValue());
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

//        if (changes){
//            writeFile(targetFile, target);
//        }
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
        System.out.println("Writing " + f.getPath() );
        SortedProperties properties = new SortedProperties();
        for(String key : values.keySet()){
            properties.put(key, values.get(key));
        }

        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f.toString() + ".temp"), StandardCharsets.UTF_8);
        properties.store(writer, "Auto generated from conversion file on " + DateFormat.getDateTimeInstance().format(new Date()));
        writer.close();

        String cmd = "\"" + NATIVE2ASCII + "\" -encoding utf8 \"" + f.toString() + ".temp\" " + f.toString();
        System.out.println(cmd);
        Process pr = Runtime.getRuntime().exec(cmd);
        InputStream is = pr.getInputStream();
        while(is.read() != -1){}
        is = pr.getErrorStream();
        while(is.read() != -1){}

        ((new File(f.toString() + ".temp"))).delete();
//        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8);
//        writer.write("#Auto generated from conversion files on " + DateFormat.getDateTimeInstance().format(new Date()));
//        writer.write(Packagei18n.LINE_SEP);
//        TreeSet<String> keys = new TreeSet<String>(values.keySet());
//        for (String key : keys){
//            String value = values.get(key);
//            value = value.replaceAll("\\r\\n|\\r|\\n", "\\\\n");
//            writer.write(key + "=" + ConversionUtils.nativeToAscii(value));
//            writer.write(Packagei18n.LINE_SEP);
//        }
//        writer.close();
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