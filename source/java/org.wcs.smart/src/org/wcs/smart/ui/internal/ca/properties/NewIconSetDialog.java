/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.ui.internal.ca.properties;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.ca.properties.NameKeyComposite;

/**
 * New iconset dialog 
 * 
 * @author Emily
 *
 */
public class NewIconSetDialog extends SmartStyledTitleDialog {

	protected NameKeyComposite keyComp;

	private IconSet newSet;
	private List<IconSet> sets;
	private List<Icon> icons;
	
	private IconSet templateSet = null;
	private IconUtils.FixedIconSet defaultSet = null;
	
	private Button btnCustom, btnDefault;
	private Composite compDefault, compCustom;
	
	private Session session;
	private List<Path> cleanUp;
	
	protected NewIconSetDialog(Shell parent, IconSet newSet,
			List<IconSet> sets, List<Icon> icons, Session session) {
		super(parent);
		this.newSet = newSet;
		this.sets = sets;
		this.icons = icons;
		this.session = session;
		
		cleanUp = new ArrayList<>();
	}

	public void okPressed() {
		
		if (btnDefault.getSelection()) {
			if (!createDefaultIconSet()) return;			
			
		}else if (btnCustom.getSelection()) {
			createCustomIconSet();
		}
		super.okPressed();
	}
	
	public List<Path> getDeleteFiles(){
		return this.cleanUp;
	}

	private void createCustomIconSet() {
		
		keyComp.updateFields(newSet);
		session.saveOrUpdate(newSet);
		
		if (templateSet != null && templateSet instanceof IconSet) {
			for (Icon icon : icons) {
				IconFile copyIcon = icon.getIconFile((IconSet) templateSet);
				if (copyIcon != null) {
					IconFile newfile = new IconFile();
					newfile.setIcon(icon);
					newfile.setIconSet(newSet);
					try {
						if (copyIcon.isSystemIcon()) {
							newfile.setFilename(copyIcon.getFilename());
						}else {
							Path temp = Files.createTempFile("smart", "icon"); //$NON-NLS-1$ //$NON-NLS-2$
							Path inputFile = null;
							if (copyIcon.getCopyFromLocation() != null) {
								inputFile = copyIcon.getCopyFromLocation();
							}else {
								inputFile = copyIcon.getAttachmentFile();
							}
							try(OutputStream out = Files.newOutputStream(temp)){
								Files.copy(inputFile, out);
							}
							newfile.setCopyFromLocation(temp);
							newfile.setFilename(copyIcon.getFilename());
							
							cleanUp.add(temp);
						}
						icon.getFiles().add(newfile);
					}catch (Exception ex) {
						SmartPlugIn.displayLog(MessageFormat.format(Messages.IconPreferencePage_CopyError,  icon.getName()), ex);
					}
					session.saveOrUpdate(icon);
				}
			}
		}
	}
	private boolean createDefaultIconSet() {
		if (defaultSet == null) {
			MessageDialog.openError(getShell(), Messages.NewIconSetDialog_ErrorTitle, Messages.NewIconSetDialog_IconsetREquired);
			return false;
		}
			
		for (IconSet is : sets) {
			if (is.getKeyId().equalsIgnoreCase(defaultSet.key)) {
				MessageDialog.openError(getShell(), Messages.NewIconSetDialog_ErrorTitle,  MessageFormat.format(Messages.NewIconSetDialog_IconSetExists,is.getKeyId()));
				return false;
			}
		}

		newSet.setKeyId(defaultSet.key);
		newSet.setName(defaultSet.name);
		newSet.updateName(SmartDB.getCurrentLanguage(), defaultSet.name);
		newSet.updateName(newSet.getConservationArea().getDefaultLanguage(), defaultSet.name);
		session.saveOrUpdate(newSet);
		
		for (String[] icon : IconUtils.SMART_ICON_MAPPING) {
					//0 - icon key
					//1 - icon name
					//2 - black icon reference
					//3 - line icon reference
					//4 - color icon reference
					//5 - data model mappings (comma seperated)
		
			String file = null;
			switch(defaultSet) {
			case BLACK:
				file = icon[2];
				break;
			case COLOR:
				file = icon[4];
				break;
			case LINE:
				file = icon[3];
				break;
			default:
				break;
			
			}
			String key = icon[0];
			Icon toUpdate = null;
			for (Icon i : icons) {
				if (i.getKeyId().equalsIgnoreCase(key)) {
					toUpdate = i;
					break;
				}
			}
			if (toUpdate == null) {
				toUpdate = new Icon();
				icons.add(toUpdate);
				toUpdate.setConservationArea(newSet.getConservationArea());
				toUpdate.setFiles(new ArrayList<>());
				toUpdate.setKeyId(key.toLowerCase());
				toUpdate.setName(icon[1]);
				toUpdate.updateName(newSet.getConservationArea().getDefaultLanguage(), icon[1]);
				toUpdate.updateName(SmartDB.getCurrentLanguage(), icon[1]);
			}
			
			IconFile iconfile = new IconFile();		
			iconfile.setIcon(toUpdate);
			iconfile.setIconSet(newSet);
			iconfile.setFilename(file);
			toUpdate.getFiles().add(iconfile);
			
			session.saveOrUpdate(toUpdate);
					
		}
		return true;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout());
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnDefault = new Button(temp, SWT.RADIO);
		btnDefault.setText(Messages.NewIconSetDialog_DefaultOp);
		btnDefault.setToolTipText(Messages.NewIconSetDialog_DefaultOpTooltip);

		compDefault = new Composite(temp, SWT.NONE);
		compDefault.setLayout(new GridLayout(2, false));
		compDefault.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)compDefault.getLayout()).marginLeft = 20;
		((GridLayout)compDefault.getLayout()).marginHeight = 0;
		Label l = new Label(compDefault, SWT.NONE);
		l.setText(Messages.NewIconSetDialog_SystemSet);
		
		ComboViewer cmbDefaultSet = new ComboViewer(compDefault, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbDefaultSet.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbDefaultSet.setContentProvider(ArrayContentProvider.getInstance());
		cmbDefaultSet.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((IconUtils.FixedIconSet)element).name;
			}
		});
		cmbDefaultSet.setInput(IconUtils.FixedIconSet.values());
		cmbDefaultSet.addSelectionChangedListener(e->defaultSet=(IconUtils.FixedIconSet) cmbDefaultSet.getStructuredSelection().getFirstElement());

		
		btnCustom = new Button(temp, SWT.RADIO);
		btnCustom.setText(Messages.NewIconSetDialog_CustomOp);
		btnCustom.setToolTipText(Messages.NewIconSetDialog_CustomTooltip);
		compCustom = new Composite(temp, SWT.NONE);
		compCustom.setLayout(new GridLayout(3, false));
		compCustom.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		keyComp = new NameKeyComposite();
		keyComp.createControls(compCustom, true, true, ()->{keyComp.validate();});
		
		l = new Label(compCustom, SWT.NONE);
		l.setText(Messages.IconPreferencePage_TemplateIconLabel);
		
		ComboViewer cmbViewer = new ComboViewer(compCustom, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof IconSet) return ((IconSet) element).getName();
				return super.getText(element);
			}
		});
		List<Object> ins = new ArrayList<>();
		ins.add(""); //$NON-NLS-1$
		ins.addAll(sets);
		cmbViewer.setInput(ins);
		cmbViewer.addSelectionChangedListener(e->templateSet=(IconSet) cmbViewer.getStructuredSelection().getFirstElement());
		
		btnCustom.addListener(SWT.Selection, e->updateEnabled());
		btnDefault.addListener(SWT.Selection, e->updateEnabled());
		
		btnCustom.setSelection(true);
		updateEnabled();
		
		setTitle(Messages.NewIconSetDialog_Title);
		setMessage(Messages.NewIconSetDialog_Message);
		getShell().setText(Messages.NewIconSetDialog_Title);
		
		return parent;
	}
	
	private void updateEnabled() {
		updateEnabled(compCustom, btnCustom.getSelection());
		updateEnabled(compDefault, btnDefault.getSelection());
		
	}
	
	private void updateEnabled(Composite root, boolean state) {
		List<Control> toUpdate = new ArrayList<>();
		toUpdate.add(root);
		while(!toUpdate.isEmpty()) {
			Control c = toUpdate.remove(0);
			c.setEnabled(state);
			if (c instanceof Composite) {
				for (Control kid:  (((Composite)c).getChildren())){
					toUpdate.add(kid);
				}
			}
		}
	}
}
