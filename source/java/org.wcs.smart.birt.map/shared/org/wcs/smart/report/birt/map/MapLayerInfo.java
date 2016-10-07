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
package org.wcs.smart.report.birt.map;

import java.io.File;

import org.locationtech.udig.project.internal.StyleBlackboard;

/**
 * A simple class for tracking information about a map layer.
 * 
 * @author Emily
 *
 */
public class MapLayerInfo {
	
	public enum LayerType{
		POINT ("Point"), //$NON-NLS-1$
		POLYGON ("Polygon"), //$NON-NLS-1$
		LINE("LineString"), //$NON-NLS-1$
		MULTIPOINT("MultiPoint"), //$NON-NLS-1$
		MULTIPOLYGON("MultiPolygon"), //$NON-NLS-1$
		MULTILINE("MultiLineString"), //$NON-NLS-1$
		RASTER("Raster"); //$NON-NLS-1$
		
		private String geotoolsType;
		
		LayerType(String geotoolsType){
			this.geotoolsType = geotoolsType;
		}
		
		public String getGeotoolsType(){
			return this.geotoolsType;
		}
	};
	
	private String layerName;
	
	private String mapStyle;
	
	private LayerType layerType;
	
	private String gometryColumn;
	
	private StyleBlackboard blackboard;
		
	private File rasterFile;
		
	public MapLayerInfo(String layerName, String mapStyle, LayerType type, String gometryColumn){
		this.layerName = layerName;
		this.mapStyle = mapStyle;
		this.layerType = type;
		this.gometryColumn = gometryColumn;
	}

	public String getLayerName() {
		return layerName;
	}

	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

	public String getMapStyle() {
		return mapStyle;
	}

	public void setMapStyle(String mapStyle) {
		this.mapStyle = mapStyle;
	}

	public StyleBlackboard getMapStyleBlackboard() {
		return blackboard;
	}
	
	public void setMapStyleBlackboard(StyleBlackboard blackboard) {
		this.blackboard = blackboard;
	}
	
	public LayerType getLayerType() {
		return layerType;
	}

	public void setLayerType(LayerType layerType) {
		this.layerType = layerType;
	}

	public String getGeometryColumn() {
		return gometryColumn;
	}

	public void setGometryColumn(String gometryColumn) {
		this.gometryColumn = gometryColumn;
	}


	public void setRasterFile(File file) {
		this.rasterFile = file;
	}
	
	public File getRasterFile(){
		return this.rasterFile;
	}
}
