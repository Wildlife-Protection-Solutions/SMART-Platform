package org.wcs.smart.i2.ui.editors.query;

import java.util.ArrayList;
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
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.IDefinitionPanel;

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
	public void setProfileFilter(String filter){
		chProfiles.setAllChecked(false);
		for (IntelProfile ip : AbstractIntelQuery.convertFromProfileFilter(filter)) {
			chProfiles.setChecked(ip, true);
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
		if (chProfiles.getCheckedElements().length == 0) return "At least one profile must be selected";
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
		chProfiles.setInput(ProfilesManager.INSTANCE.getActiveProfiles()
				.stream().filter(e->IntelSecurityManager.INSTANCE.canViewQuery(e)).collect(Collectors.toList()));
		chProfiles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		chProfiles.addSelectionChangedListener(e->fireQueryChangedListeners());
		
		return mainComposite;
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

