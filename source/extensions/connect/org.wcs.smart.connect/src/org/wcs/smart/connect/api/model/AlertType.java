/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.api.model;

import java.util.UUID;

/**
 * Alert Type model object
 * 
 * @author Emily
 *
 */
public class AlertType {
	
	private String label;
	private String color; 
	private String opacity;
	private String markerIcon;
	private String markerColor;
	private Boolean spin;
	private UUID uuid;
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getColor() {
		if(color != null && !color.substring(0,1).equals("#")){ //$NON-NLS-1$
			color = "#" + color; //$NON-NLS-1$
		}
		return color;
	}
	public void setColor(String color) {
		if(color != null && !color.substring(0,1).equals("#")){ //$NON-NLS-1$
			color = "#" + color; //$NON-NLS-1$
		}	this.color = color;
	}

	
	public String getOpacity() {
		return opacity;
	}
	public void setOpacity(String opacity) {
		this.opacity = opacity;
	}
	
	public String getMarkerColor() {
		return markerColor;
	}
	public void setMarkerColor(String markerColor) {
		this.markerColor = markerColor;
	}
	
	public String getMarkerIcon() {
		return markerIcon;
	}
	public void setMarkerIcon(String markerIcon) {
		this.markerIcon = markerIcon;
	}
	
	public Boolean getSpin() {
		return spin;
	}
	public void setSpin(Boolean spin) {
		this.spin = spin;
	}
	
	/**
	 * 
	 * @return the uuid for the list element
	 */
	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	
	
	public boolean equals(Object other){
		if (other != null && other instanceof AlertType){
			AlertType s = (AlertType)other;
			if (s.getUuid() == null && this.getUuid() == null){
				return this == s;
			}else if (s.getUuid() != null && this.getUuid() != null){
				return s.getUuid().compareTo(this.getUuid()) == 0;
			}
		}
		return false;
	}
	
	
	public int hashCode(){
		if (getUuid() != null){
			return getUuid().hashCode();
		}
		return super.hashCode();
	}
}
