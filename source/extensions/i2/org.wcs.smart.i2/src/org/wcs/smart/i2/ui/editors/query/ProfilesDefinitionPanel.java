/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.IDefinitionPanel;

/**
 * Profiles filter panel
 * 
 * @author Emily
 *
 */
public class ProfilesDefinitionPanel implements IDefinitionPanel {
	
	private Composite mainComposite;
	private List<Runnable> queryListeners = new ArrayList<>();	
	private CheckboxTableViewer chProfiles;
	
	public ProfilesDefinitionPanel(){
	}
	
	/**
	 * Sets the filter type
	 * @param filterType
	 */
	@SuppressWarnings("unchecked")
	public void setProfileFilter(String filter){
		chProfiles.setAllChecked(false);
		Collection<IntelProfile> profiles = (Collection<IntelProfile>) chProfiles.getInput();
		Collection<String> selected = AbstractIntelQuery.convertFromProfileFilter(filter); 
		for (IntelProfile ip : profiles) {
			if (selected.contains(ip.getKeyId())) chProfiles.setChecked(ip, true);
		}
	}

	@Override
	public void dispose(){
		mainComposite.dispose();
	}

	@Override
	public void clear(){		
		chProfiles.setAllChecked(false);
		for (IntelProfile ip :ProfilesManager.INSTANCE.getActiveProfiles()
				.stream().filter(e->IntelSecurityManager.INSTANCE.canViewQuery(e)).collect(Collectors.toSet())) {
			chProfiles.setChecked(ip,  true);
		}
	}

	/**
	 * Validates the items in the filter panel 
	 */
	public String validate(){
		if (chProfiles.getCheckedElements().length == 0) return Messages.ProfilesDefinitionPanel_ProfileRequired;
		return null;
	}
	
	
	/**
	 * Creates the drop target composite
	 * @param parent
	 * @return
	 */
	@Override
	public Composite createComposite(Composite parent) {
		
		mainComposite = new Composite(parent, SWT.BORDER);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		mainComposite.setLayout(layout);
		

		chProfiles = CheckboxTableViewer.newCheckList(mainComposite, SWT.NONE);
		chProfiles.setContentProvider(ArrayContentProvider.getInstance());
		chProfiles.setLabelProvider(new ProfileLabelProvider());
		chProfiles.setInput(getProfiles());
		chProfiles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		chProfiles.addSelectionChangedListener(e->fireQueryChangedListeners());
		
		return mainComposite;
	}
	
	private List<IntelProfile> getProfiles(){
		HashMap<String, IntelProfile> items = new HashMap<>();
		for (IntelProfile ip : ProfilesManager.INSTANCE.getActiveProfiles()) {
			if (IntelSecurityManager.INSTANCE.canViewQuery(ip)) {
				if (!items.containsKey(ip.getKeyId())) {
					IntelProfile temp = new IntelProfile();
					temp.setKeyId(ip.getKeyId());
					temp.setName(ip.getName());
					temp.setColor(ip.getColor());
					items.put(ip.getKeyId(), temp);
				}
				
			}
		}
		return new ArrayList<>(items.values());
	}
	
	@Override
	public Composite getDropTargetComposite() {
		return null;
	}

	
	
	public void addQueryChangedListener(Runnable r){
		queryListeners.add(r);
	}
	
	@Override
	public void fireQueryChangedListeners() {
		for (Runnable r : queryListeners){
			r.run();
		}
	}

	@Override
	public void addItem(DropItem item) {
	}

	@Override
	public void removeItem(DropItem item) {
	}

	@Override
	public String getQueryPart() {
		Set<IntelProfile> items = new HashSet<>();
		for (Object x : chProfiles.getCheckedElements()) items.add((IntelProfile)x);
		return AbstractIntelQuery.convertToProfileFilter(items);
	}

	@Override
	public void redraw() {
	}

	@Override
	public void finishDrag(DropItem item) {
	}

}

