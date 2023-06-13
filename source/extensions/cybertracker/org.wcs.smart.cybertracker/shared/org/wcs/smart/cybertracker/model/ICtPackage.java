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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.json.simple.JSONObject;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Transient;

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
	 * Metadata key for flagging package and private or public
	 */
	public static final String PRIVATE_PROP_KEY = "PKG_ISPRIVATE";  //$NON-NLS-1$

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
	 * Gets a set of metadata values about the package
	 * 
	 * @return
	 */
	public List<MetadataFieldValue> getMetadataValues();

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
	
	/**
	 * 
	 * @param url the url to access the package on connect
	 * @param requiresPassword if a password on connect is required to access password
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Transient
	public static String generateSmartMobileAppLink(URL url, boolean requiresPassword) {
		
		StringBuilder serverUrl = new StringBuilder();
		serverUrl.append(url.getProtocol());
		serverUrl.append("://"); //$NON-NLS-1$
		serverUrl.append(url.getHost());
		if (url.getPort() != -1) {
			serverUrl.append(":"); //$NON-NLS-1$
			serverUrl.append(url.getPort());
		}
		serverUrl.append(url.getPath());
		
		JSONObject json = new JSONObject();
		json.put("connector", "SMART"); //$NON-NLS-1$ //$NON-NLS-2$
		json.put("server", serverUrl.toString()); //$NON-NLS-1$
		json.put("launch", Boolean.TRUE); //$NON-NLS-1$
		json.put("auth", requiresPassword); //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append("https://cybertrackerwiki.org/applink-smart?"); //$NON-NLS-1$
		sb.append(new String(Base64.getEncoder().encode(json.toString().getBytes())));
		return sb.toString();
	}
	
	/**
	 * True of false if the package requires a configuration 
	 * page for the Connect URL
	 * 
	 * @return true if supports connect URL, false otherwise
	 */
	@Transient
	public default boolean showConnectUrlConfiguration() {
		return true;
	}
}
