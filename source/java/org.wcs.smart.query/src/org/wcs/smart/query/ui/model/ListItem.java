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
package org.wcs.smart.query.ui.model;

import java.text.Collator;
import java.util.UUID;

import org.eclipse.jface.viewers.LabelProvider;
import org.wcs.smart.query.internal.Messages;

/**
 * A list item for drop item lists.  A list item can contain
 * one or many of the following:
 * <li>uuid - a unique identifer</li>
 * <li>name - a name displayed to the user</li>
 * <li>key - a unqiue string identifier</li>
 * 
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ListItem implements Comparable<ListItem>{
	
	private UUID uuid;
	private String name;
	private String key;
	private String shortName;
	private boolean active = true;
	
	/**
	 * Creates a new list item with a name. 
	 * In this case the uuid and key are null; 
	 * 
	 * @param name
	 */
	public ListItem(String name){
		this(null, name, null);
	}
	
	/**
	 * Creates a new list item with a name and uuid. 
	 * In this case the key will be null. 
	 *  
	 * 
	 * @param uuid
	 * @param name
	 */
	public ListItem(UUID uuid, String name){
		this(uuid, name, null);
	}

	/**
	 * Creates a new list item with a name, uuid and active state. 
	 * In this case the key will be null. 
	 *  
	 * 
	 * @param uuid
	 * @param name
	 */
	public ListItem(UUID uuid, String name, boolean active){
		this(uuid, name, null);
		this.active = active;
	}
	
	/**
	 * Creates a new list item
	 * 
	 * @param name
	 * @param key
	 */
	public ListItem(UUID uuid, String name, String key){
		this.uuid = uuid;
		this.name = name;
		this.key = key;
	}
	
	/**
	 * Creates a new list item
	 * 
	 * @param name
	 * @param key
	 */
	public ListItem(UUID uuid, String name, String key, String shortName){
		this.uuid = uuid;
		this.name = name;
		this.key = key;
		this.shortName = shortName;
	}
	
	/**
	 * Updates the name
	 * @param newName
	 */
	public void updateName(String newName){
		this.name = newName;
	}
	
	/**
	 * Short name associated with list item
	 * @return
	 */
	public String getShortName(){
		return this.shortName;
	}
	
	/**
	 * @return the list item name
	 */
	public String getName(){
		return this.name;
	}
	/**
	 * @return the list item uuid
	 */
	public UUID getUuid(){
		return this.uuid;
	}
	/**
	 * @return the list item key
	 */
	public String getKey(){
		return this.key;
	}
	
	/**
	 * @return the active state for a list item
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @return label provider for list items
	 */
	public static LabelProvider createLabelProvider() {
		return new ListItemLabelProvider();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if (uuid != null){
			result = prime * result + uuid.hashCode();
		}else{
			result = prime * result + ((key == null) ? 0 : key.hashCode());
		}
		
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
		ListItem other = (ListItem) obj;
		if (key != null && other.key != null) {
			return key.equals(other.key);
		}else if (uuid != null && other.uuid != null){
			return uuid.equals(other.uuid);
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(ListItem o) {
		if (o == null){
			return 1;
		}
		String src = (name == null) ? "" : name; //$NON-NLS-1$
		String dst = (o.getName() == null) ? "" : o.getName(); //$NON-NLS-1$
		return Collator.getInstance().compare(src, dst);
	}
	
	/**
	 * Label provider for {@link ListItem}.
	 * 
	 * @author elitvin
	 * @since 4.1.0
	 */
	private static class ListItemLabelProvider extends LabelProvider {

		@Override
		public String getText (Object element) {
			if (element instanceof ListItem) {
				ListItem li = (ListItem) element;
				String name = li.getName();
				if (!li.isActive()) {
					name += Messages.ListItem_Inactive;
				}
				return name;
			}
			return super.getText(element);
		}

	}
}