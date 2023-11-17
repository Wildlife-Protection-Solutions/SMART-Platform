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
package org.wcs.smart.patrol.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.hibernate.annotations.SQLOrder;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.IFolder;
import org.wcs.smart.ca.NamedItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Folder for {@link Patrol} objects.
 *
 * @author elitvin
 * @since 6.0.0
 */
@Entity
@Table(name="patrol_folder", schema="smart")
public class PatrolFolder extends NamedItem implements IFolder {
	
	private static final long serialVersionUID = 1L;
	
	private ConservationArea conservationArea;
	private PatrolFolder parentFolder;
	private List<PatrolFolder> childFolders;
	private int folderOrder;

	/**
	 * The conservation area this folder is associated with
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	/**
	 * The conservation area this folder is associated with
	 * @param conservationArea
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}

	/**
	 * @return the parent folder or <code>null</code> if root folder
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="parent_uuid", referencedColumnName="uuid")
	@Override
	public PatrolFolder getParentFolder() {
		return parentFolder;
	}
	public void setParentFolder(PatrolFolder parentFolder) {
		this.parentFolder = parentFolder;
	}
	@Override
	public void setParentFolder(IFolder parentFolder) {
		Assert.isTrue(parentFolder == null || parentFolder instanceof PatrolFolder);
		setParentFolder((PatrolFolder)parentFolder);
	}

	/**
	 * @return the children folders
	 */
//	@LazyCollection(LazyCollectionOption.FALSE)
	@OneToMany(mappedBy="parentFolder", cascade={CascadeType.ALL}, orphanRemoval = true)
	@SQLOrder("folder_order")
	public List<PatrolFolder> getChildFolders() {
		if (this.childFolders == null) {
			this.childFolders = new ArrayList<>();
		}
		return this.childFolders;
	}
	/**
	 * Sets the children folder
	 * @param children
	 */
	public void setChildFolders(List<PatrolFolder> children) {
		this.childFolders = children;
	}

	@Column(name = "folder_order")
	public int getFolderOrder() {
		return folderOrder;
	}
	public void setFolderOrder(int folderOrder) {
		this.folderOrder = folderOrder;
	}
	
}
