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
package org.wcs.smart.cybertracker.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.util.UuidUtils;

/**
 * Represents a cybertracker package configuration.  These configurations
 * are managed via a ICtPackageManager.
 * @author Emily
 *
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public interface ICtPackage {

	public static final String PACKAGE_DATE_FORMAT = "yyyyMMddHHmmss"; //$NON-NLS-1$

	/**
	 * Unique identifier
	 * @return
	 */
	public UUID getUuid();
	
	/**
	 * Owing conservation area
	 * @return
	 */
	public ConservationArea getConservationArea();
	
	/**
	 * 
	 * @return the name of the package
	 */
	public String getName();
	
	/**
	 * Used to link packages to package managers
	 * 
	 * @return the key that identifies the package type 
	 */
	public String getTypeIdentifier();

	/**
	 * Create a copy of this package
	 * 
	 * @return
	 */
	public ICtPackage copy();
	
	@Transient
	public default Path getLocalFile() throws IOException {
		if (getUuid() == null) return null;
		
		Path root = ICyberTrackerConstants.getCyberTrackerPackageFolder(getConservationArea());
		if (!Files.exists(root)) return null;
		
		String idpart = UuidUtils.uuidToString(getUuid());
		
		try(Stream<Path> files = Files.walk(root)){
			Optional<Path> p = files.filter(file->file.getFileName().toString().startsWith(idpart)).findFirst();
			if (p.isEmpty()) return null;
			return p.get();
		}
	}
	
}
