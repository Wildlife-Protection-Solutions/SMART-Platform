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
package org.wcs.smart.ca.icon;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Represents an icon - icons can have multiple representations, one
 * for each icon set in the system
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="icon", schema="smart")
public class Icon extends NamedKeyItem{

	private static final long serialVersionUID = 1L;
	
	private ConservationArea ca;
	
	private List<IconFile> files;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.ca;
	}
	
	public void setConservationArea(ConservationArea ca){
		this.ca = ca;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="icon", cascade={CascadeType.ALL}, orphanRemoval = true)
	public List<IconFile> getFiles(){
		return this.files;
	}
	
	public void setFiles(List<IconFile> files) {
		this.files = files;
	}
	
	@Transient
	public IconFile getIconFile(IconSet set) {
		if (getFiles() == null) return null;
		for (IconFile f : getFiles()) {
			if (f.getIconSet() == set) return f;
			if (f.getIconSet().equals(set)) return f;
		}
		return null;
	}
	
	@Transient
	public void computeFileLocations(Session session) {
		for (IconFile f : getFiles()) f.computeFileLocation(session);
	}
	
	
	
}
