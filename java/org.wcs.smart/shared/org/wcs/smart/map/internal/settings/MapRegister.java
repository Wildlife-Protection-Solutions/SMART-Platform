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

import java.net.URI;
import java.util.List;

import org.geotools.brewer.color.BrewerPalette;
import org.locationtech.udig.ui.palette.ColourScheme;


/**
 * Maintains the custom setting done in the map object
 * 
 * @author Mauricio Pazos
 *
 */
public final class MapRegister {

	private final BrewerPalette colorPalette;
	private final List<LayerRegister> layerList;
	private final URI id;
	private final String name;
	private final ColourScheme colourScheme;
	private final String crsWkt;

	public MapRegister(
			final URI id, final String sName, 
			final BrewerPalette colorPalette,
			final ColourScheme colourScheme,
			final List<LayerRegister> layerRegisterList,
			final String crsWkt) {

		this.id = id;
		this.name = sName;
		this.colorPalette = colorPalette;
		this.colourScheme = colourScheme;

		this.layerList = layerRegisterList;
		this.crsWkt = crsWkt;
	}

	public String getCrsWkt(){
		return this.crsWkt;
	}
	
	public URI getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public BrewerPalette getColorPalette() {
		return colorPalette;
	}

	public List<LayerRegister> getLayerList() {
		return layerList;
	}

	public ColourScheme getColourScheme() {
		return colourScheme;
	}

	@Override
	public String toString() {
		return "MapRegister [colorPalette=" + colorPalette + ", layerList=" //$NON-NLS-1$ //$NON-NLS-2$
				+ layerList + ", id=" + id + ", name=" + name + ", envelop=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ ", colourScheme=" + colourScheme + "]"; //$NON-NLS-1$ //$NON-NLS-2$
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
		MapRegister other = (MapRegister) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
