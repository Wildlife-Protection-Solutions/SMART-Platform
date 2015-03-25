package org.wcs.smart.i18n;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
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
public class Mergei18n {

    public static final String IN_DIR[] = {"C:\\data\\SMART\\Source\\trunk\\source\\java",
    	"C:\\data\\SMART\\Source\\trunk\\source\\extensions",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er"};
    
    public static final String TRANS_DIR[] = {"C:\\data\\SMART\\Source\\trunk\\source\\translations\\",
    	"C:\\data\\SMART\\Source\\trunk\\source\\extensions",
		"C:\\data\\SMART\\Source\\trunk\\source\\extensions\\er\\translations"};
	
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
            processFile(f, transDir);
        }

        return files.toArray(new File[files.size()]);
    }

    /*
     * Processes a base file, looking for matching i18n files
     * and merging matched files.
     */
    private void processFile(File f, String stransDir) throws Exception {
        File transDir = new File(stransDir);

        int index = f.getCanonicalPath().indexOf("org.wcs.smart");
        String pluginName = f.getCanonicalPath().substring(index);
        index = pluginName.indexOf(File.separator);
        pluginName = pluginName.substring(0, index);
        String pathName = f.getCanonicalPath().substring(f.getCanonicalPath().indexOf(pluginName) + pluginName.length());
        File transFile = new File(pathName);


        List<File> filesList = new ArrayList<File>();

        final String matchDir = pluginName + ".nl_es";  /*ADD _XX if you want to search for a specific language */
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

//            System.out.println(f.toString());
//            System.out.println(toMerge.toString());

            if (!f.exists() || !toMerge.exists()){
                System.err.println("Error either source or target file does not exists. " + f.toString() + "  |  " + toMerge.toString());
            }else{
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
//                target.put(e.getKey(), e.getValue());
//                target.put(e.getKey(), "**NEW**" + e.getValue());
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

        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f.toString() + ".temp"), "UTF-8");
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
//        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
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
        Mergei18n util = new Mergei18n();
        try{
        	for (int i = 0; i < IN_DIR.length; i ++){
        		util.findFiles(IN_DIR[i], TRANS_DIR[i]);
        	}
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}