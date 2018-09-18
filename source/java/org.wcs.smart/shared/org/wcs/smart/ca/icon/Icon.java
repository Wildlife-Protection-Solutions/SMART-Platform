package org.wcs.smart.ca.icon;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedKeyItem;

@Entity
@Table(name="smart.icon")
public class Icon extends NamedKeyItem{

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
	
}
