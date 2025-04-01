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
package org.wcs.smart.paws;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.util.UuidUtils;

/**
 * Managing paws filestore details
 * 
 * @author Emily
 *
 */
public enum PawsFileManager {
	
	INSTANCE;
	
	public static final String PAWS_DIR = "paws";  //$NON-NLS-1$

	public Path getDirectory(PawsConfiguration config) {
		Path ds = Paths.get(config.getConservationArea().getFileDataStoreLocation())
				.resolve(PAWS_DIR)
				.resolve("config") //$NON-NLS-1$
				.resolve(UuidUtils.uuidToString(config.getUuid()));
		return ds;
	}
	
	public Path getDirectory(PawsRun run) {
		 return getRunDirectory(run.getConservationArea())
			.resolve(UuidUtils.uuidToString(run.getUuid()));
	}
	
	/**
	 * Gets the directory where PAWS run data and
	 * results are stored.
	 * 
	 * @param ca
	 * @return
	 */
	public Path getRunDirectory(ConservationArea ca) {
		 return Paths.get(ca.getFileDataStoreLocation())
					.resolve(PAWS_DIR)
					.resolve("run"); //$NON-NLS-1$
	}
	
	public Path getResultsDirectory(PawsRun run) {
		return getDirectory(run).resolve("results"); //$NON-NLS-1$
	}
	
}
