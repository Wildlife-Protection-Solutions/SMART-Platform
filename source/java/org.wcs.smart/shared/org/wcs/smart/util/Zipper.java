/*
 * Copyright (C) 2023 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;


/**
 * A collection of zip utilities.
 * 
 * @since 1.0.0
 */
public class Zipper {

	
	
	private FileOutputStream fOut = null;
	private BufferedOutputStream bOut = null;
	private ZipArchiveOutputStream zOut = null;
	
	private Set<Path> excludes;
	
	public static Zipper create(Path fileName) throws FileNotFoundException {
		return new Zipper(fileName);
	}
	
	private Zipper (Path fileName) throws FileNotFoundException {
		fOut = new FileOutputStream(fileName.toFile());
		bOut = new BufferedOutputStream(fOut);
		zOut = new ZipArchiveOutputStream(bOut);
		
		excludes = new HashSet<>();
	}
	
	public void close() throws IOException {
		zOut.close();
		fOut.close();
		bOut.close();
	}
	
	public Zipper excludeFiles(Set<Path> excludes) {
		this.excludes.addAll(excludes);
		return this;
	}
	
	/**
	 * Add set of paths to zip file at root
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public Zipper addFiles(List<Path> path) throws IOException {
		for(Path p : path) addFile(p);
		return this;
	}	
	
	/**
	 * Adds path to zip file at root
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public Zipper addFile(Path path) throws IOException {
		addFileToZip(path, null, null);
		return this;
	}
	
	/**
	 * Adds all the children files/dirs from the source folder to the target
	 * folder in the zip file
	 * 
	 * For example:
	 * 
	 * If you have the following directory structure: 
	 * data/file1.txt
	 * data/dir1/file2.txt
	 * data/dir2/file3.txt
	 * 
	 * And call addMappedChildren(Paths.get("data"), Paths.get("target")) the resulting
	 * zip file will contain contain:
	 * 
	 * target/file1.txt
	 * target/dir1/file2.txt
	 * target/dir2/file3.txt
	 * 
	 * If source is a file it is added to the target directory in the zip file 
	 * 
	 * @param source source file or directory
	 * @param target always the target directory or null if root
	 * @return
	 * @throws IOException
	 */
	public Zipper addMappedChildren(Path source, Path target) throws IOException {
		List<Path> all = new ArrayList<>();
		if (Files.isDirectory(source)) {
			try(Stream<Path> files = Files.list(source)){
				files.forEach(f->all.add(f));
			}
			for(Path p : all) addFileToZip(p, null, target);
		}else {
			addFileToZip(source, null, target);
		}
		return this;
	}
	
	/**
	 * Adds all children to zip file with children being at root
	 * of file
	 * 
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	public Zipper addChildrenFiles(Path dir) throws IOException {
		List<Path> all = new ArrayList<>();
		try(Stream<Path> files = Files.list(dir)){
			files.forEach(f->all.add(f));
		}
		for(Path p : all) addFileToZip(p, null, null);
		
		return this;
	}
	
    
    private void addFileToZip( Path source, Path sourceBase, Path targetBase) throws IOException {
    	
    	if (this.excludes.contains(source)) return;
    	
    	String entryName = source.getFileName().toString();
    	
    	if (targetBase != null) {
	    	Path base = targetBase;
	    	if (sourceBase != null) {
	    		base = targetBase.resolve(sourceBase);
	    	}
	    	entryName = base.resolve(source.getFileName()).toString();
    	}else {
    		if (sourceBase != null) {
    			entryName = sourceBase.resolve(source.getFileName()).toString();
    		}
    	}
    	
    	
    	if (!Files.exists(source)) {
    		//assume empty directory and ignore
    	}else if (!Files.isDirectory(source)) {
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(source.toAbsolutePath().normalize().toFile(), entryName); 
            zOut.putArchiveEntry(zipEntry);
            try(InputStream in = Files.newInputStream(source)){
            	IOUtils.copy(in, zOut);
            }
            zOut.closeArchiveEntry();
        } else {
        	
        	List<Path> kids = null;
        	
        	try(Stream<Path> items = Files.list(source)){
        		kids = items.collect(Collectors.toList());
        	}
        	
        	if (Files.isDirectory(source) && kids.size() == 0){
        		//empty directory
        		ZipArchiveEntry zipEntry = new ZipArchiveEntry(entryName + File.separator); 
        		zOut.putArchiveEntry(zipEntry);
        		zOut.closeArchiveEntry();
        	} else {
        		Path newBase = null;
        		if (sourceBase != null) {
        			newBase = sourceBase.resolve(source.getFileName());
        		}else {
        			newBase = source.getFileName();
        		}
        		
        		for (Path child : kids) {
        			addFileToZip(child, newBase, targetBase);
        		}
            }

        }
    }
}
