import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.wcs.smart.ZipUtil;

import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

public class CompressTest {

	private static int BUFFER_SIZE = 1024*1024;
	
	public static void main(String[] args) {

		try {
//		String file = "C:\\temp\\Rich\\Etosha_Connect_download_4_20_2018\\database\\smart.track.Track.dat";
//		String outFilename = "C:\\temp\\Rich\\Etosha_Connect_download_4_20_2018\\database\\smart.track.Track.dat.lz4";
		
			String file = "C:\\temp\\Rich\\Etosha_Connect_download_4_20_2018\\filestore\\maps\\etosha.tiff";
			String outFilename = "C:\\temp\\Rich\\Etosha_Connect_download_4_20_2018\\filestore\\maps\\etosha.tiff.lz4";

		 
		 // compress data
		 
//		 LZ4Compressor compressor = factory.fastCompressor();
//		 compressor.compress(src, dest);
//		 int maxCompressedLength = compressor.maxCompressedLength(f.length());
//		 byte[] compressed = new byte[maxCompressedLength];
//		 int compressedLength = compressor.compress(data, 0, decompressedLength, compressed, 0, maxCompressedLength);
		 
		 InputStream in = Files.newInputStream(Paths.get(file));
		 OutputStream fout = Files.newOutputStream(Paths.get(outFilename));
		 BufferedOutputStream out = new BufferedOutputStream(fout);
		 FramedLZ4CompressorOutputStream lzOut = new FramedLZ4CompressorOutputStream(out);
//		 LZ4BlockOutputStream lzOut = new LZ4BlockOutputStream(out);
		 final byte[] buffer = new byte[BUFFER_SIZE];
		 int n = 0;
		 int cnt = 0;
		 while (-1 != (n = in.read(buffer))) {
			 System.out.println("write");
			 //stops at 4194304 bytes
		     lzOut.write(buffer, 0, n);
		     cnt += n;
		     System.out.println(cnt);
		     lzOut.flush();
		 }
		 lzOut.close();
		 in.close();
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		String directory = "C:\\temp\\Rich\\Etosha_Connect_download_4_20_2018";
		long time = 0;
		long time2 = 0;
		
//		System.out.println("GZIP/TAR");
//		time = System.nanoTime();
//		try {
//			gziptar(directory);
//		}catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		time2 = System.nanoTime();
//		
//		System.out.println( (time2 - time) / Math.pow(10,  9)  );
		
		
//		System.out.println("7Z");
//		time = System.nanoTime();
//		try {
//			z7(directory);
//		}catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		time2 = System.nanoTime();
//		
//		System.out.println( (time2 - time) / Math.pow(10,  9)  );
	
//		System.out.println("LZ4");
//		time = System.nanoTime();
//		try {
//			lz4(directory);
//		}catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		time2 = System.nanoTime();
//		
//		System.out.println( (time2 - time) / Math.pow(10,  9)  );
//		
//		
//		System.out.println("ZIP");
//		time = System.nanoTime();
//		try {
//			zip(directory);
//		}catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		time2 = System.nanoTime();
//		
//		System.out.println( (time2 - time) / Math.pow(10,  9)  );
	}

	private static void z7(String directory) throws Exception{
		String outputFile = "C:\\temp\\Rich\\etosha.z7";
		
		SevenZOutputFile sevenZOutput = new SevenZOutputFile(new File(outputFile));
		Path p = Paths.get(directory);

		Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//				System.out.println(file.toString());
				SevenZArchiveEntry entry = sevenZOutput.createArchiveEntry(file.toFile(), file.relativize(p).toString());
				sevenZOutput.putArchiveEntry(entry);
				InputStream in = Files.newInputStream(file);
				final byte[] buffer = new byte[BUFFER_SIZE];
				int n = 0;
				while (-1 != (n = in.read(buffer))) {
					sevenZOutput.write(buffer, 0, n);
				}
				sevenZOutput.closeArchiveEntry();
				return FileVisitResult.CONTINUE;

			}
		});
		sevenZOutput.close();
		System.out.println(outputFile);
		System.out.println(Files.size(Paths.get(outputFile)));
		
	}

	private static void gziptar(String directory) throws Exception {

		String outputFile = "C:\\temp\\Rich\\etosha.tar.gz";
		
		OutputStream fout = Files.newOutputStream(Paths.get(outputFile));
		BufferedOutputStream out2 = new BufferedOutputStream(fout);
		GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(fout);
		
		// Create a TarOutputStream
		TarArchiveOutputStream out = new TarArchiveOutputStream(new BufferedOutputStream(gzOut));
		Path p = Paths.get(directory);

		Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				TarArchiveEntry ae = new TarArchiveEntry(file.relativize(p).toString());
				ae.setSize(Files.size(file));
				out.putArchiveEntry(ae);
				
				try(InputStream in = Files.newInputStream(file)){
					IOUtils.copy(in, out);
				}
//				final byte[] buffer = new byte[BUFFER_SIZE];
//				int n = 0;
//				while (-1 != (n = in.read(buffer))) {
//					out.write(buffer, 0, n);
//					
//				}
				out.closeArchiveEntry();
				return FileVisitResult.CONTINUE;
			}
		});
		
		out.close();

//		InputStream in = Files.newInputStream(Paths.get("C:\\temp\\Rich\\etosha.tar"));
//	
//		final byte[] buffer = new byte[BUFFER_SIZE];
//		int n = 0;
//		while (-1 != (n = in.read(buffer))) {
//			gzOut.write(buffer, 0, n);
//		}
		gzOut.close();
//		in.close();
		
		System.out.println(outputFile);
		System.out.println(Files.size(Paths.get(outputFile)));
	}

	private static void lz4(String directory) throws Exception {
		
		String outputFile = "C:\\temp\\Rich\\etosha.tar.lz4";
//		FileOutputStream dest = new FileOutputStream("C:\\temp\\Rich\\etosha.2.tar");

		
//		InputStream in = Files.newInputStream(Paths.get("C:\\temp\\Rich\\etosha.2.tar"));
		
		OutputStream fout = Files.newOutputStream(Paths.get(outputFile));
//		BufferedOutputStream bfout = new BufferedOutputStream(fout);
//		FramedLZ4CompressorOutputStream lzOut = new FramedLZ4CompressorOutputStream(fout);
//		BlockLZ4CompressorOutputStream lzOut = new BlockLZ4CompressorOutputStream(fout);
		LZ4BlockOutputStream lzOut = new LZ4BlockOutputStream(fout);
		
		// Create a TarOutputStream
		TarArchiveOutputStream out = new TarArchiveOutputStream(lzOut);
		Path p = Paths.get(directory);

		Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				System.out.println(file.toString());
				TarArchiveEntry ae = new TarArchiveEntry(file.relativize(p).toString());
				ae.setSize(Files.size(file));
				out.putArchiveEntry(ae);
				
				InputStream in = Files.newInputStream(file);
				final byte[] buffer = new byte[BUFFER_SIZE];
				int n = 0;
				int cnt = 0;
				while (-1 != (n = in.read(buffer))) {
					out.write(buffer, 0, n);
					cnt += n;
//					System.out.println(cnt);
				}
				out.closeArchiveEntry();
				return FileVisitResult.CONTINUE;
			}
		});
		
		out.close();

//		InputStream in = Files.newInputStream(Paths.get("C:\\temp\\Rich\\etosha.2.tar"));
//		OutputStream fout = Files.newOutputStream(Paths.get(outputFile));
//		BufferedOutputStream bfout = new BufferedOutputStream(fout);
//		FramedLZ4CompressorOutputStream lzOut = new FramedLZ4CompressorOutputStream(bfout);
//		
//		final byte[] buffer = new byte[BUFFER_SIZE];
//		int n = 0;
//		while (-1 != (n = in.read(buffer))) {
//			lzOut.write(buffer, 0, n);
//		}
		lzOut.close();
//		in.close();
		System.out.println(outputFile);
		System.out.println(Files.size(Paths.get(outputFile)));
	}
	
	
	private static void zip(String directory) throws Exception {
		String outputFile = "C:\\temp\\Rich\\etosha.zip";
		ZipUtil.createZip(new File[] {new File(directory)}, new File(outputFile));
		System.out.println(outputFile);
		System.out.println(Files.size(Paths.get(outputFile)));
	}
}
