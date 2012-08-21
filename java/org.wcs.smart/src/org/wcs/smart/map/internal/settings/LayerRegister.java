/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.map.internal.settings;

import java.awt.Color;
import java.net.URI;
import java.util.List;

import net.refractions.udig.ui.palette.ColourScheme;

/**
 * Maintains the layer's data set by the user which will be serialized
 * 
 * @author Mauricio Pazos
 *
 */
final class LayerRegister {

	
	private final String name;
	private final ColourScheme colourScheme;
	private final URI URI;
	private final String CRS;
	private final String CQL;
	private final Color defaultColor;
	private final List<StyleRegister> styleRegisterList;
	private final double maxScaleDenominator;
	private final double minScaleDenominator;
	private final String envelope;
	
	
	public LayerRegister(
			final String name, 
			final ColourScheme colourScheme,
			final URI uri, 
			final String crs,
			final String cql,
			final Color defaultColor, 
			final Double maxScaleDenominator,
			final Double minScaleDenominator,
			final String envelope,
			final List<StyleRegister> styleList) {
		
		this.URI = uri;
		this.name = name;
		this.colourScheme = colourScheme;
		this.CRS = crs;
		this.CQL = cql;
		this.defaultColor = defaultColor;
		this.maxScaleDenominator = maxScaleDenominator;
		this.minScaleDenominator = minScaleDenominator;
		this.envelope = envelope;
		
		this.styleRegisterList = styleList;
	}

	@Override
	public String toString() {
		return "LayerRegister [name=" + name + ", colourScheme=" + colourScheme
				+ ", URI=" + URI + ", CRS=" + CRS + ", CQL=" + CQL
				+ ", defaultColor=" + defaultColor + ", styleRegisterList="
				+ styleRegisterList + ", maxScaleDenominator="
				+ maxScaleDenominator + ", minScaleDenominator="
				+ minScaleDenominator + ", envelope=" + (envelope ==null ? "" : envelope) + "]";
	}

	public String getName() {
		return name;
	}

	public ColourScheme getColourScheme() {
		return colourScheme;
	}

	public URI getURI() {
		return URI;
	}
	
	public String getCQL() {
		return CQL;
	}

	/**
	 * @return CRS as WKT
	 */
	public String getCRS() {
		return CRS;
	}

	public Color getDefaultColor() {
		return defaultColor;
	}

	public List<StyleRegister> getStyleRegisterList() {
		return styleRegisterList;
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}


	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerRegister other = (LayerRegister) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public Double getMaxScaleDenominator() {
		
		return this.maxScaleDenominator;
	}

	public Double getMinScaleDenominator() {
		return this.minScaleDenominator;
	}

	public String getEnvelope() {
		return this.envelope;
	}
	
	
}
