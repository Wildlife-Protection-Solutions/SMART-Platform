package org.wcs.smart;

import java.io.File;

import org.apache.commons.io.FileUtils;

public class PackageProduct {

	public static final String VERSION = "4.0.0";
	public static final String RC = "a13";
	
	public static final String GPS_BABEL = "C:\\data\\SMART\\Exports\\dependencies\\GPSBabel";
	
	public static final String DATA = "C:\\data\\SMART\\Exports\\dependencies\\data-" + VERSION;

	public static final String WIN32_JRE = "C:\\data\\SMART\\Exports\\dependencies\\jres\\win32\\jre";
	public static final String WIN64_JRE = "C:\\data\\SMART\\Exports\\dependencies\\jres\\win64\\jre";
	
	public static final String LINUX32_JRE = "C:\\data\\SMART\\Exports\\dependencies\\jres\\linux32\\jre";
	public static final String MAC64_JRE = "C:\\data\\SMART\\Exports\\dependencies\\jres\\macosx64\\jre";

	
	public static final String BUILD_WIN32 = "C:\\data\\SMART\\Exports\\win32.win32.x86";
	public static final String BUILD_WIN64 = "C:\\data\\SMART\\Exports\\win32.win32.x86_64";
	public static final String BUILD_LINUX32 = "C:\\data\\SMART\\Exports\\linux.gtk.x86";
	public static final String BUILD_MAC32 = "C:\\data\\SMART\\Exports\\macosx.cocoa.x86";
	public static final String BUILD_MAC64 = "C:\\data\\SMART\\Exports\\macosx.cocoa.x86_64";
	
	public static final String APP_NAME = "smart";
	public static final String UPDATESITE_DIRNAME = "updatesite";
	
//	public static final String UPDATE_SITE_DIR = "C:\\data\\SMART\\Source\\Version1\\trunk\\source\\java\\org.wcs.smart.updatesite";
	public static final String UPDATE_SITE_DIR = "C:\\data\\SMART\\Source\\trunk\\source\\java\\org.wcs.smart.updatesite";
	
	public static final String[] UPDATE_SITE_FILES = new String[]{"features", "plugins", "artifacts.jar", "content.jar"};
	
	public static File UPDATE_ZIP_FILE = new File(new File(UPDATE_SITE_DIR), "site_" + VERSION + ".v1.zip");
	
	public static final String NETWORK = "L:\\Refractions\\SMART\\internal_demo\\";
	
	public static final String LOW_MEMORY_WIN = "L:\\Refractions\\SMART\\Delivery\\LowMemory_Shortcuts\\SMART.LowMemory.exe.lnk";
	public static final String LOW_MEMORY_MAC = "L:\\Refractions\\SMART\\Delivery\\LowMemory_Shortcuts\\SMART.LowMemory.app";
	public static final String LOW_MEMORY_LINUX = "L:\\Refractions\\SMART\\Delivery\\LowMemory_Shortcuts\\linux_SMART.LowMemory";
	
	public static void packageUpdateSize() throws Exception{
		File updateDir = new File(UPDATE_SITE_DIR);
		File[] toZip = new File[UPDATE_SITE_FILES.length];
		for (int i = 0; i < UPDATE_SITE_FILES.length; i ++){
			toZip[i] = new File(updateDir, UPDATE_SITE_FILES[i]);
		}
		
		if (UPDATE_ZIP_FILE.exists()){
			UPDATE_ZIP_FILE.delete();
		}
		ZipUtil.createZip(toZip, UPDATE_ZIP_FILE);
	}
	
	public static void processWindows() throws Exception{
		System.out.println("Processing Windows");
		File winDir = new File(BUILD_WIN32, APP_NAME);
		
		//jre
		File out = new File(winDir, "jre");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(WIN32_JRE), out);
		
		//gps babel
		out = new File(winDir, "GPSBabel");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(GPS_BABEL), out);
		
		//data
		out = new File(winDir, "data");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(DATA), out);
		
		//updatesite
		File f = new File(winDir, UPDATESITE_DIRNAME);
		if (f.exists()){
			FileUtils.deleteDirectory(f);
		}
		f.mkdir();
		FileUtils.copyFileToDirectory(UPDATE_ZIP_FILE, f);
		
		//copy low memory shortcut
		f = new File(winDir, "SMART.LowMemory.exe");
		if (f.exists()){
			f.delete();
		}
		FileUtils.copyFile(new File(LOW_MEMORY_WIN), f);
		
		
		//install plugins
	}
	
	public static void processWindows64() throws Exception{
		System.out.println("Processing Windows");
		File winDir = new File(BUILD_WIN64, APP_NAME);
		
		//jre
		File out = new File(winDir, "jre");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(WIN64_JRE), out);
		
		//gps babel
		out = new File(winDir, "GPSBabel");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(GPS_BABEL), out);
		
		//data
		out = new File(winDir, "data");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(DATA), out);
		
		//updatesite
		File f = new File(winDir, UPDATESITE_DIRNAME);
		if (f.exists()){
			FileUtils.deleteDirectory(f);
		}
		f.mkdir();
		FileUtils.copyFileToDirectory(UPDATE_ZIP_FILE, f);
		
		//copy low memory shortcut
		f = new File(winDir, "SMART.LowMemory.exe");
		if (f.exists()){
			f.delete();
		}
		FileUtils.copyFile(new File(LOW_MEMORY_WIN), f);
		
		//install plugins
	}
	
	public static void processLinux() throws Exception{
		System.out.println("Processing Linux");
		File linuxDir = new File(BUILD_LINUX32, APP_NAME);
		
		//jre
		File out = new File(linuxDir, "jre");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(LINUX32_JRE), out);
		
		
		//data
		out = new File(linuxDir, "data");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(DATA), out);
		
		//updatesite
		File f = new File(linuxDir, UPDATESITE_DIRNAME);
		if (f.exists()){
			FileUtils.deleteDirectory(f);
		}
		f.mkdir();
		FileUtils.copyFileToDirectory(UPDATE_ZIP_FILE, f);
		
		//copy low memory shortcut
		f = new File(linuxDir, "SMART.LowMemory");
		if (f.exists()){
			f.delete();
		}
		FileUtils.copyFile(new File(LOW_MEMORY_LINUX), f);
		
		//install plugins
	}
	
	public static void processMac() throws Exception{
		System.out.println("Processing MACOSX");
		File macDir = new File(BUILD_MAC32 + File.separator + APP_NAME + File.separator + "SMART.app" + File.separator +"Contents" + File.separator + "MacOS");

		//data
		File out = new File(macDir, "data");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(DATA), out);
		
		//updatesite
		File f = new File(macDir, UPDATESITE_DIRNAME);
		if (f.exists()) {
			FileUtils.deleteDirectory(f);
		}
		f.mkdir();
		FileUtils.copyFileToDirectory(UPDATE_ZIP_FILE, f);
		
		//copy low memory shortcut
		f = new File(macDir, "SMART.LowMemory.app");
		if (f.exists()){
			FileUtils.deleteDirectory(f);
		}
		FileUtils.copyDirectory(new File(LOW_MEMORY_MAC), f);
			
		//sort out pInfo.plist file
		File pInfo = new File(BUILD_MAC32 + File.separator + APP_NAME + File.separator + "SMART.app" + File.separator +"Contents" + File.separator + "Info.plist");
		File pInfo2 = new File(BUILD_MAC32 + File.separator + APP_NAME + File.separator + "SMART.app" + File.separator +"Contents" + File.separator + "Info2.plist");
		if (pInfo2.exists()){
			pInfo.delete();
			FileUtils.moveFile(pInfo2, pInfo);
		}
		
		//install plugins
	}
	public static void processMac64() throws Exception{
		System.out.println("Processing MACOSX64");
		File macDir = new File(BUILD_MAC64 + File.separator + APP_NAME);

		//jre
		File out = new File(macDir, "jre");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(MAC64_JRE), out);
				
		//data
		macDir = new File(BUILD_MAC64 + File.separator + APP_NAME + File.separator + "SMART.app" + File.separator +"Contents" + File.separator + "MacOS");
		out = new File(macDir, "data");
		if (out.exists()){
			FileUtils.deleteDirectory(out);
		}
		FileUtils.copyDirectory(new File(DATA), out);
		
		//updatesite
		File f = new File(macDir, UPDATESITE_DIRNAME);
		if (f.exists()) {
			FileUtils.deleteDirectory(f);
		}
		f.mkdir();
		FileUtils.copyFileToDirectory(UPDATE_ZIP_FILE, f);
		
		//copy low memory shortcut
		f = new File(macDir, "SMART.LowMemory.app");
		if (f.exists()){
			FileUtils.deleteDirectory(f);
		}
		FileUtils.copyDirectory(new File(LOW_MEMORY_MAC), f);
		
		//sort out pInfo.plist file
		File pInfo = new File(BUILD_MAC64 + File.separator + APP_NAME
				+ File.separator + "SMART.app" + File.separator + "Contents"
				+ File.separator + "Info.plist");
		File pInfo2 = new File(BUILD_MAC64 + File.separator + APP_NAME
				+ File.separator + "SMART.app" + File.separator + "Contents"
				+ File.separator + "Info2.plist");
		if (pInfo2.exists()) {
			pInfo.delete();
			FileUtils.moveFile(pInfo2, pInfo);
		}
		//install plugins
	}
	
	public static void zipMac() throws Exception{
		System.out.println("Zipping MACOSX");
		File macDir = new File(BUILD_MAC32, APP_NAME);
		
		File outFile = new File(BUILD_MAC32, "smart." + VERSION + ".macosx32" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		if (outFile.exists()){
			outFile.delete();
		}
		ZipUtil.createZip(new File[]{macDir}, outFile);
	}
	public static void zipMac64() throws Exception{
		System.out.println("Zipping MACOSX64");
		File macDir = new File(BUILD_MAC64, APP_NAME);
		
		File outFile = new File(BUILD_MAC64, "smart." + VERSION + ".macosx32_64" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		if (outFile.exists()){
			outFile.delete();
		}
		ZipUtil.createZip(new File[]{macDir}, outFile);
	}
	
	public static void zipLinux() throws Exception{
		System.out.println("Zipping Linux");
		File macDir = new File(BUILD_LINUX32, APP_NAME);
		
		File outFile = new File(BUILD_LINUX32, "smart." + VERSION + ".linux32" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		if (outFile.exists()){
			outFile.delete();
		}
		ZipUtil.createZip(new File[]{macDir}, outFile);
	}
	
	public static void zipWindows() throws Exception{
		System.out.println("Zipping Windows");
		File winDir = new File(BUILD_WIN32, APP_NAME);
		
		File outFile = new File(BUILD_WIN32, "smart." + VERSION + ".win32" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		if (outFile.exists()){
			outFile.delete();
		}
		ZipUtil.createZip(new File[]{winDir}, outFile);
	}
	public static void zipWindows64() throws Exception{
		System.out.println("Zipping Windows");
		File winDir = new File(BUILD_WIN64, APP_NAME);
		
		File outFile = new File(BUILD_WIN64, "smart." + VERSION + ".win64" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		if (outFile.exists()){
			outFile.delete();
		}
		ZipUtil.createZip(new File[]{winDir}, outFile);
	}
	
	public static void copyToNetworkLinux() throws Exception{
		System.out.println("copy to network - linux");
		File srcFile = new File(BUILD_LINUX32, "smart." + VERSION + ".linux32" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		FileUtils.copyFileToDirectory(srcFile, new File(NETWORK));
	}
	
	public static void copyToNetworkMacosx() throws Exception{
		System.out.println("copy to network - mac");
		File srcFile = new File(BUILD_MAC32, "smart." + VERSION + ".macosx32" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		FileUtils.copyFileToDirectory(srcFile, new File(NETWORK));
	}
	public static void copyToNetworkMacosx64() throws Exception{
		System.out.println("copy to network - mac");
		File srcFile = new File(BUILD_MAC64, "smart." + VERSION + ".macosx32_64" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		FileUtils.copyFileToDirectory(srcFile, new File(NETWORK));
	}
	public static void copyToNetworkWindows() throws Exception{
		System.out.println("copy to network - windows");
		File srcFile = new File(BUILD_WIN32, "smart." + VERSION + ".win32" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		FileUtils.copyFileToDirectory(srcFile, new File(NETWORK));
	}
	public static void copyToNetworkWindows64() throws Exception{
		System.out.println("copy to network - windows");
		File srcFile = new File(BUILD_WIN64, "smart." + VERSION + ".win64" + (RC.length() == 0 ? "" : ".") + RC + ".zip" );
		FileUtils.copyFileToDirectory(srcFile, new File(NETWORK));
	}
	public static void main(String[] args) throws Exception{

//		packageUpdateSize();
//		
//		processWindows();
//		processWindows64();
//		zipWindows();
//		copyToNetworkWindows();
		zipWindows64();
		copyToNetworkWindows64();
		
//		processMac();
//		zipMac();
//		copyToNetworkMacosx();
		
//		processMac64();
//		zipMac64();
//		copyToNetworkMacosx64();
//		
//		processLinux();
//		zipLinux();
//		copyToNetworkLinux();
	}
}
