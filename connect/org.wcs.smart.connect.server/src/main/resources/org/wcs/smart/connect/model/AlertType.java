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
package org.wcs.smart.connect.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


/*
 * An AlertType entity
 * 
 * Users can define new alertTypes with the attributes in this class.
 *
 * @Author Jeff
 */


@Entity
@Table(name = "connect.alert_types")
public class AlertType extends ConnectUuidItem{
	private String key;
	private String label;
	private String color; 
//	private String fillColor;
	private String opacity;
	private String markerIcon;
	private String markerColor;
	private boolean spin;
	
	
	@Column(name="key")
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	
	@Column(name="label")
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	@Column(name="color")
	public String getColor() {
		if(!color.substring(0,1).equals("#")){
			color = "#" + color;
		}
		return color;
	}
	public void setColor(String color) {
		if(!color.substring(0,1).equals("#")){
			color = "#" + color;
		}		this.color = color;
	}
//	

//	public String getFillColor() {
//		if(!fillColor.substring(0,1).equals("#")){
//			fillColor = "#" + fillColor;
//		}
//		return fillColor;
//	}
//	public void setFillColor(String fillColor) {
//		if(!fillColor.substring(0,1).equals("#")){
//			fillColor = "#" + fillColor;
//		}
//		this.fillColor = fillColor;
//	}
	
	@Column(name="opacity")
	public String getOpacity() {
		return opacity;
	}
	public void setOpacity(String opacity) {
		this.opacity = opacity;
	}
	
	@Column(name="markerColor")
	public String getMarkerColor() {
		return markerColor;
	}
	public void setMarkerColor(String markerColor) {
		this.markerColor = markerColor;
	}
	
	@Column(name="markerIcon")
	public String getMarkerIcon() {
		return markerIcon;
	}
	public void setMarkerIcon(String markerIcon) {
		this.markerIcon = markerIcon;
	}
	
	@Column(name="spin")
	public boolean getSpin() {
		return spin;
	}
	public void setSpin(boolean spin) {
		this.spin = spin;
	}
}
