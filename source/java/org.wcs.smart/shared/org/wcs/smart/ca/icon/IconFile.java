package org.wcs.smart.ca.icon;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.UuidUtils;

@Entity
@Table(name="smart.iconfile")
public class IconFile extends ISmartAttachment{

	public static final String ICON_DIR = "icons";
	
	private IconSet iconSet;
	private Icon icon;
	

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="iconset_uuid", referencedColumnName="uuid")
	public IconSet getIconSet() {
		return this.iconSet;
	}
	
	public void setIconSet(IconSet iconSet){
		this.iconSet = iconSet;
	}
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="icon_uuid", referencedColumnName="uuid")
	public Icon getIcon() {
		return this.icon;
	}
	
	public void setIcon(Icon icon){
		this.icon = icon;
	}
	
	/**
	 * Getter for file name
	 * 
	 * @return
	 */
	@Column(name="filename")
	@Override
	public String getFilename() {
		return super.getFilename();
	}

	/**
	 * Setter for file name
	 * 
	 * @param filename
	 */
	@Override
	public void setFilename(String filename) {
		super.setFilename(filename);
	}

	@Transient
	@Override
	protected String getDatastoreFolderPath(Session session) throws Exception {
		Path p = Paths.get(getConservationArea().getFileDataStoreLocation()).resolve(ICON_DIR);
		return p.toString();
	}

	@Transient
	@Override
	public ConservationArea getConservationArea() {
		return icon.getConservationArea();
	}
	
	@Transient
	public boolean isEncrypted() {
		return false;
	}
	
	@Transient
	@Override
	public void computeFileLocation(Session session) {
		if (attachmentFile != null) return;
		String filename = getFilename();
		if (filename.startsWith("platform:/plugin")) return;
		attachmentFile = Paths.get(getConservationArea().getFileDataStoreLocation()).resolve(ICON_DIR).resolve(filename).toFile();
	}
	
	@Transient
	@Override
	public File getAttachmentFile(){
		String filename = getFilename();
		if (filename.startsWith("platform:/plugin")) {
			try {
				//TODO: manage this directory				
				URL url = new URL(filename);
				Path temp = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve("ICONDIR").resolve(UuidUtils.uuidToString(getUuid()) + ".svg");
				if (!Files.exists(temp.getParent())) Files.createDirectories(temp.getParent());
				if (Files.exists(temp)) return temp.toFile();
			
				try(InputStream inputStream = url.openConnection().getInputStream()){
					Files.copy(inputStream, temp);
					return temp.toFile();
				}
				
			} catch (Exception e) {
				throw new IllegalStateException("Invalid URL.", e); //$NON-NLS-1$
			}
		}
		if (attachmentFile == null){
			throw new IllegalStateException("Attachment file not set.  You must first call computeFileLocaion."); //$NON-NLS-1$
		}
		return attachmentFile;
	}
}
