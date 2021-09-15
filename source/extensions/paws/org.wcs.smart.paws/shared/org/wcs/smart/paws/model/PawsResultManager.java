/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.paws.PawsFileManager;


/**
 * Tools for managing PAWS results
 * 
 * @author Emily
 *
 */
public class PawsResultManager {

	public static final String PROJ_FILE = "runtime.wkt"; //$NON-NLS-1$
	private PawsRun run;
	private List<PawsResultFile> resultFiles;
	
	public PawsResultManager(PawsRun run) throws IOException {
		this.run = run;
		resultFiles = new ArrayList<>();
		
		Path resultsDirectory = PawsFileManager.INSTANCE.getResultsDirectory(run);
		
		if (!Files.exists(resultsDirectory)) return;
		
		try(Stream<Path> stream = Files.walk(resultsDirectory, 1)){
			stream.forEach(f->{
				if (!Files.isDirectory(f) && f.getFileName().toString().endsWith("csv"))  //$NON-NLS-1$
					resultFiles.add(new PawsResultFile(run, f));
			});
		}
	}

	public PawsRun getRun() {
		return this.run;
	}
	
	public List<PawsResultFile> getResults(){
		return this.resultFiles;
	}
	
	public void createImages() throws Exception{
		//determine projection
		Path projFile = PawsFileManager.INSTANCE.getResultsDirectory(run).resolve(PROJ_FILE);
		String wktprj = Files.readString(projFile);
		CoordinateReferenceSystem crs = CRS.parseWKT(wktprj);
		
		//create image files
		for (PawsResultFile f : resultFiles) {
			f.createOutputImages(crs);
		}
	}

	
}
