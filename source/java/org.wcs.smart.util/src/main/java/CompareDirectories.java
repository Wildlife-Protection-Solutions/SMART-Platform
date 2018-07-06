import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class CompareDirectories {

	public static void main(String[] args) throws IOException{
		
		String dir1 = "C:\\Users\\Emily\\Desktop\\SMART_CA.1\\database";
		String dir2 = "C:\\Users\\Emily\\Desktop\\SMART_CA.2\\database";
		
		Path p1 = FileSystems.getDefault().getPath(dir1);
		Path p2 = FileSystems.getDefault().getPath(dir2);
		
		List<Path> p1files = fileList(p1);
		List<Path> p2files = fileList(p2);
		
		for (Path p : p1files){
			Path tester = p2.resolve(p.getFileName());
			if (!p2files.contains(tester)){
				System.out.println("p2 missing: " + tester.toString());
			}
		}
		for (Path p : p2files){
			Path tester = p1.resolve(p.getFileName());
			if (!p1files.contains(tester)){
				System.out.println("p1 missing: " + tester.toString());
			}
		}
		
		for (Path ap1 : p1files){
			Path ap2 = p2.resolve(ap1.getFileName());
			if (Files.size(ap1) != Files.size(ap2)){
				System.out.println("different file sizes: " + ap1.getFileName());
				continue;
			}
			
			byte[] b1 = Files.readAllBytes(ap1);
			byte[] b2 = Files.readAllBytes(ap2);
			if (b1.length != b2.length){
				System.out.println("different file sizes: " + ap1.getFileName());
				continue;
			}
			for (int i = 0; i < b1.length; i ++){
				if (b1[i] != b2[i]){
					System.out.println("different files: " + ap1.getFileName());
					break;
				}
			}
		}
	}
	
    public static List<Path> fileList(Path rpath) {
        List<Path> fileNames = new ArrayList<Path>();
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rpath)) {
            for (Path path : directoryStream) {
                fileNames.add(path);
            }
        } catch (IOException ex) {}
        return fileNames;
    }
}
