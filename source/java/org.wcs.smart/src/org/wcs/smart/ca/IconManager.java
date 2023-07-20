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
package org.wcs.smart.ca;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.FixedIconSet;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

public enum IconManager {
	
	INSTANCE;
	
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
}
