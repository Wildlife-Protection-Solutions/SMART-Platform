/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ca;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.FixedIconSet;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.SmartUtils;
/**
 * For managing icons in the desktop and creating/caching thumbnails.
 * 
 * Also see IconUtils for code shared with Connect
 * 
 * @author Emily
 * @since 8.0.0
 *
 */
public enum IconManager {
	
	INSTANCE;
	
	private static final String SYSTEM_THUMBNAILS_DIR = "system_thumbnails"; //$NON-NLS-1$
	public static final String CA_ICON_THUMBNAIL_DIR = "icon_thumbs"; //$NON-NLS-1$
	
	public enum Size{
		ICON(16),
		SMALL(32),
		MEDIUM(50);
		
		public int size;
		
		Size(int size){
			this.size = size;
		}
		
	}

	/**
	 * Gets the conservation area specific icons - these are used in the CA
	 * or have been manually configured.
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<Icon> getIcons(Session session, ConservationArea ca){
		List<Icon> icons = QueryFactory.buildQuery(session, Icon.class, 
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		icons.forEach(e->e.getFiles().forEach(f->{
			f.getIconSet().getUuid();
			try {
				f.computeFileLocation(session);
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
		}));
		return icons;
	}
	
	/**
	 * Returns all system icons that aren't already used in the conservation area.
	 * 
	 * @param session
	 * @param ca
	 * @return
	 */
	public List<Icon> getSystemIcons(Session session, ConservationArea ca) {

		String query = "SELECT keyId FROM Icon WHERE conservationArea = :ca"; //$NON-NLS-1$
		List<String> icons = session.createQuery(query, String.class)
				.setParameter("ca", ca).list();  //$NON-NLS-1$

		Set<String> existingIcons = new HashSet<>(icons);

		List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, 
				new Object[] { "conservationArea", ca }) //$NON-NLS-1$
				.list();

		List<Icon> libraryIcons = new ArrayList<>();
		
		for (String[] icondef : IconUtils.INSTANCE.SMART_ICON_MAPPING) {
			if (existingIcons.contains(icondef[0].toLowerCase()))
				continue;

			Icon icon = new Icon();

			icon.setKeyId(icondef[0]);

			icon.setName(icondef[1]);
			icon.updateName(SmartDB.getCurrentLanguage(), icondef[1]);
			icon.updateName(ca.getDefaultLanguage(), icondef[1]);
			icon.setFiles(new ArrayList<>());
			icon.setConservationArea(ca);
			
			libraryIcons.add(icon);
			
			// black
			for (IconSet set : sets) {
				String filename = null;
				if (set.getKeyId().equalsIgnoreCase(FixedIconSet.BLACK.key)) {
					filename = icondef[2];
				} else if (set.getKeyId().equalsIgnoreCase(FixedIconSet.LINE.key)) {
					filename = icondef[3];
				} else if (set.getKeyId().equalsIgnoreCase(FixedIconSet.COLOR.key)) {
					filename = icondef[4];
				}
				if (filename == null)
					continue;

				IconFile file = new IconFile();
				file.setIcon(icon);
				file.setFilename(filename);
				file.setIconSet(set);
				icon.getFiles().add(file);
			}
		}
		return libraryIcons;

	}
	
	/**
	 * Returns all system icons 
	 * 
	 * @param session
	 * @return
	 */
	public List<Icon> getSystemIcons(Session session) {

		List<Icon> libraryIcons = new ArrayList<>();
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		
		List<IconSet> sets = QueryFactory.buildQuery(session, IconSet.class, 
				new Object[] { "conservationArea", ca }) //$NON-NLS-1$
				.list();
		
		for (String[] icondef : IconUtils.INSTANCE.SMART_ICON_MAPPING) {

			Icon icon = new Icon();

			icon.setKeyId(icondef[0]);
			icon.setName(icondef[1]);
			icon.updateName(SmartDB.getCurrentLanguage(), icondef[1]);
			icon.updateName(ca.getDefaultLanguage(), icondef[1]);
			icon.setFiles(new ArrayList<>());
			icon.setConservationArea(ca);
			
			libraryIcons.add(icon);
			
			// black
			for (IconSet set : sets) {
				String filename = null;
				if (set.getKeyId().equalsIgnoreCase(FixedIconSet.BLACK.key)) {
					filename = icondef[2];
				} else if (set.getKeyId().equalsIgnoreCase(FixedIconSet.LINE.key)) {
					filename = icondef[3];
				} else if (set.getKeyId().equalsIgnoreCase(FixedIconSet.COLOR.key)) {
					filename = icondef[4];
				}
				if (filename == null)
					continue;

				IconFile file = new IconFile();
				file.setIcon(icon);
				file.setFilename(filename);
				file.setIconSet(set);
				icon.getFiles().add(file);
			}
		}
		return libraryIcons;

	}
	
	
	/**
	 * Creates the default icon sets with no icons. Icons will be added as they are
	 * used in the Conservation Area.
	 * 
	 */
	/*
	 *  * 0 - icon key
	 * 1 - icon name
	 * 2 - black icon reference
	 * 3 - line icon reference
	 * 4 - color icon reference
	 * 5 - data model mappings (comma separated)
	 */
	public void createDefaultIconSet(Session session, ConservationArea ca) {
		IconSet blackIs = new IconSet();
		blackIs.setConservationArea(ca);
		blackIs.setIsDefault(false);
		blackIs.setKeyId(FixedIconSet.BLACK.key);
		blackIs.setName(FixedIconSet.BLACK.name);
		blackIs.updateName(ca.getDefaultLanguage(), FixedIconSet.BLACK.name);
		session.persist(blackIs);
		
		IconSet colorIs = new IconSet();
		colorIs.setConservationArea(ca);
		colorIs.setIsDefault(true);
		colorIs.setKeyId(FixedIconSet.COLOR.key);
		colorIs.setName(FixedIconSet.COLOR.name);
		colorIs.updateName(ca.getDefaultLanguage(), FixedIconSet.COLOR.name);
		session.persist(colorIs);
		
		IconSet lineIs = new IconSet();
		lineIs.setConservationArea(ca);
		lineIs.setIsDefault(false);
		lineIs.setKeyId(FixedIconSet.LINE.key);
		lineIs.setName(FixedIconSet.LINE.name);
		lineIs.updateName(ca.getDefaultLanguage(), FixedIconSet.LINE.name);
		session.persist(lineIs);

	}
	
	public String getLibraryFile(String iconKey, IconSet set) {
		return IconUtils.INSTANCE.getLibraryFile(iconKey, set);
	}
	
	public IconSet getDefaultIconSet(Session session, ConservationArea ca) {
		return session.createQuery("FROM IconSet WHERE conservationArea = :ca and isDefault", IconSet.class) //$NON-NLS-1$
		.setParameter("ca", ca) //$NON-NLS-1$
		.uniqueResult();
	}
	
	/**
	 * Clears all files from the conservation area thumbnail cache
	 */
	public void clearThumbnails() {
		Path caDir = Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
				.resolve(CA_ICON_THUMBNAIL_DIR);
		try {
			SmartUtils.deleteDirectory(caDir);
		} catch (IOException e) {
			SmartPlugIn.log(e.getMessage(), e);
		}
	}
	
	public void clearThumbnailFiles(Icon icon) {
		icon.getFiles().forEach(f->clearThumbnailFile(f));
	}
	
	public void clearThumbnailFile(IconFile file) {
		String name2 = null;
		Path iconfile = null;
		if (!file.isSystemIcon()) {
			//only non-system icons can be modified
			
			//find all thumbnails of each size
			for(Size s : Size.values()) {
				name2 = file.getUuid().toString() + "_" + s.size + ".png"; //$NON-NLS-1$ //$NON-NLS-2$
				iconfile = Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
						.resolve(CA_ICON_THUMBNAIL_DIR)
						.resolve(name2);
				if (Files.exists(iconfile)) {
					try {
						Files.delete(iconfile);
					} catch (IOException e) {
						SmartPlugIn.log(e.getMessage(), e);
					}
				}
			}
		}	
	}
	
	public Path getThumbnailFile(IconFile file, Size size) {
		
		String name2 = null;
		Path iconfile = null;
		if (file.isSystemIcon()) {
			//system icons can be shared across conservation areas
			name2 = file.getIcon().getKeyId() + "_" + file.getIconSet().getKeyId() + "_" + size.size + ".png"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			iconfile = SmartContext.INSTANCE.getTempFilestoreLocation()
					.resolve(SYSTEM_THUMBNAILS_DIR)
					.resolve(name2);
			
	        
		}else {
			//only available to this conservation area
			name2 = file.getUuid().toString() + "_" + size.size + ".png"; //$NON-NLS-1$ //$NON-NLS-2$
			iconfile = Paths.get(SmartDB.getCurrentConservationArea().getFileDataStoreLocation())
					.resolve(CA_ICON_THUMBNAIL_DIR)
					.resolve(name2);
			
		}
		
		if (!Files.exists(iconfile)) {
		
			Path temp = null;
			if (file.getCopyFromLocation() != null) {
				temp = file.getCopyFromLocation();
			}else {
				temp = file.getAttachmentFile();
			}
			
			Image newImage = SmartUtils.getImage(temp, size.size);
			if (newImage != null) {
				try {
					Files.createDirectories(iconfile.getParent());
				
					//write to file
					ImageLoader saver = new ImageLoader();
					saver.data = new ImageData[] { newImage.getImageData() };
					try(OutputStream out = Files.newOutputStream(iconfile)){
						saver.save(out, SWT.IMAGE_PNG);
					}
				}catch(IOException ex) {
					SmartPlugIn.log("Cannot cache icon thumbnail: " + ex.getMessage(), ex); //$NON-NLS-1$
					return null;
				}finally {
					newImage.dispose();
				}
			}
		}
        return iconfile;
	}
	/**
	 * 
	 * @param icon
	 * @param size
	 * @return the image associated with the default icon set
	 */
	public Image getThumbnail(IconFile icon, Size size) {
		if (icon == null) return null;
		
		Path p = getThumbnailFile(icon, size);
		if (p == null) return null;
				
		try(InputStream is  = Files.newInputStream(p)){
			return new Image(Display.getDefault(), is); 
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getLocalizedMessage(), ex);
		}
		return null;
	}
	
	/**
	 * 
	 * @param icon
	 * @param size
	 * @return the image associated with the default icon set
	 */
	public Image getThumbnail(Icon icon, Size size) {
		if (icon == null) return null;
		for (IconFile file : icon.getFiles()) {
			if (file.getIconSet().getIsDefault()) {
				
				Path p = getThumbnailFile(file, size);
				if (p == null) return null;
				try(InputStream is  = Files.newInputStream(p)){
					return new Image(Display.getDefault(), is); 
				}catch (Exception ex) {
					SmartPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		return null;
	}
	
	/**
	 * Merges all the icon files into a single image. 
	 * 
	 * @param icon the icon to generate image from
	 * @param size the size for each file image
	 * @return
	 */
	public Image generateImage(Icon icon, Size size) {
		if (icon == null) return null;
		List<IconFile> files = icon.getFiles();
		if (files.isEmpty()) return null;
		
		//combine all files into a single image 
		Image img = new Image(Display.getDefault(), (size.size + 5) * files.size(), size.size);
		GC gc = new GC(img);
		try {
			for (int i = 0; i < files.size(); i++) {
				IconFile ff = files.get(i);
				
				Image mm = getThumbnail(ff, size);
				if (mm == null) continue;
				try {
					gc.drawImage(mm, 0,0, size.size, size.size, i * (size.size + 5), 0, size.size, size.size);
				}finally {
					mm.dispose();
				}				
			}
		}finally {
			gc.dispose();
		}
		return img;
	}
}
