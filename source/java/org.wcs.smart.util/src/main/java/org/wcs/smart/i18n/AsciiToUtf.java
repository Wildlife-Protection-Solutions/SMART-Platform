package org.wcs.smart.i18n;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
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
public class AsciiToUtf {

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
	
    public static final String[] LANGUAGES =  new String[] {"ar", "es","fr", "hi","in","ka","kar","km","lo","mn","ms","ru","sw","th","vi","zh","pt"};
    
    //public static final String[] LANGUAGES =  new String[] {"pt"};
    
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

        	
        	StringBuilder cmd = new StringBuilder();
        	cmd.append(NATIVE2ASCII);
        	cmd.append(" -reverse -encoding UTF-8 ");
        	cmd.append(toMerge.toString());
        	cmd.append(" " );
        	cmd.append(toMerge.toString());
        	
        	System.out.println(cmd);
        	
        	Process pr = Runtime.getRuntime().exec(cmd.toString());
            InputStream is = pr.getInputStream();
            while(is.read() != -1){}
            is = pr.getErrorStream();
            while(is.read() != -1){}

        }
    }

  

    public static void main(String args[]) {
        AsciiToUtf util = new AsciiToUtf();
        try{
        	for (int i = 0; i < TRANS_DIR.length; i ++){
        		util.findFiles(IN_DIR[i], TRANS_DIR[i]);
        	}
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}