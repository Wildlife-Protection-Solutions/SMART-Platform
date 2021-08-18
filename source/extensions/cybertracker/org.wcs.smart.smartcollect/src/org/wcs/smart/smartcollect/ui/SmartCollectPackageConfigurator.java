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
package org.wcs.smart.smartcollect.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.Collator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.cybertracker.ctpackage.ConnectDataContribution;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageConfigurator;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackageProperty;
import org.wcs.smart.cybertracker.ctpackage.ui.ICtPackagePropertyProvider;
import org.wcs.smart.cybertracker.export.IPackageContribution;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.export.PackageContributionManager;
import org.wcs.smart.cybertracker.export.data.DataModelWrapper;
import org.wcs.smart.cybertracker.model.ConfigurableModelCtPropertiesProfile;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.cybertracker.properties.CtProfileLabelProvider;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesDialog;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;
import org.wcs.smart.smartcollect.connect.SmartCollectConnectDataContribution;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.SmartCollectPackage;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Package configurator for SMARTCollect package
 * 
 * @author Emily
 *
 */
public class SmartCollectPackageConfigurator implements ICtPackageConfigurator {
	
	private SmartCollectPackage ctpackage;
	
	private ComboViewer modelViewer;
	private ComboViewer profileViewer;
	private Text txtName;
	private Button btnPrivate;
	private Text txtUrl;
	private Label lblWarn;
	private ToolBar privateTb;
	
	private List<IPackageUiContribution> contributions = null;
	private ConfigurableModel selectedModel = null;
	private CyberTrackerPropertiesProfile cmDefaultProfile = null;

	private Consumer<String> onValidate;
	private Consumer<Boolean> onModified;
	
	private boolean isInit = false;
	
	
	@Inject
	private IEclipseContext context;
	
	@Override
	public void createGui(Composite parent, ICtPackage ctitem, Consumer<String> validate,
			Consumer<Boolean> modified) {
		
		contributions = new ArrayList<>();
		contributions.add(new SmartCollectConnectDataContribution().getUiController());
		for ( IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
			if (c.getUiController() == null) continue;
			if (c.getClass().equals(ConnectDataContribution.class)) continue;
			contributions.add(c.getUiController());
		}
		
		contributions.forEach(e->ContextInjectionFactory.inject(e, context));
		
		this.onModified = modified;
		this.onValidate = validate;
		if (!(ctitem instanceof SmartCollectPackage)) throw new IllegalStateException(Messages.SmartCollectPackageConfigurator_InvalidType);
		this.ctpackage = (SmartCollectPackage) ctitem;
	
		
		CTabFolder tabs = new CTabFolder(parent, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tabs.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		CTabItem mainTab = new CTabItem(tabs, SWT.NONE);
		mainTab.setText(Messages.SmartCollectPackageConfigurator_SettingsTab);
		
		ScrolledComposite scroll = new ScrolledComposite(tabs,  SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		
		Composite main = new Composite(scroll, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		scroll.setContent(main);
		mainTab.setControl(scroll);
		
		Composite g = new Composite(main, SWT.NONE);
		g.setLayout(new GridLayout());
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SmartUiUtils.createHeaderLabel(g, Messages.SmartCollectPackageConfigurator_ConfigurationSection);
		
		g = new Composite(g, SWT.NONE);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite parts = new Composite(g, SWT.NONE);
		parts.setLayout(new GridLayout(2, false));
		parts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		((GridLayout)parts.getLayout()).marginWidth = 0;
		((GridLayout)parts.getLayout()).marginHeight = 0;
		
		Label lblWarnheader = new Label(parts, SWT.NONE);
		lblWarnheader.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
		
		Label warnLabel = new Label(parts, SWT.WRAP);
		warnLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)warnLabel.getLayoutData()).widthHint = 200;
		warnLabel.setText(Messages.SmartCollectPackageConfigurator_WarningLabel);
		
		Label nameLabel = new Label(g, SWT.NONE);
		nameLabel.setText(Messages.SmartCollectPackageConfigurator_PackageNameLabel);
		
		txtName = new Text(g, SWT.BORDER);
		txtName.setText(ctitem.getName() == null ? (ctitem.getTypeIdentifier() + Messages.SmartCollectPackageConfigurator_DeafultName) : ctitem.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->{ if (!isInit) validate();});
		
		Label modelLabel = new Label(g, SWT.NONE);
		modelLabel.setText(Messages.SmartCollectPackageConfigurator_CMLabel);
		
		modelViewer = new ComboViewer(g, SWT.READ_ONLY);
		modelViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		((GridData)modelViewer.getControl().getLayoutData()).widthHint = 100;
		modelViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelViewer.setLabelProvider(new ConfigurableModelLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof DataModelWrapper) {
					return Messages.SmartCollectPackageConfigurator_DataModelLabel;
				}
				return super.getText(element);
			}
		});
		modelViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		modelViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		modelViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				selectedModel = null;
				Object profile = ((IStructuredSelection)modelViewer.getSelection()).getFirstElement();
				if (profile instanceof ConfigurableModel) {
					selectedModel = (ConfigurableModel)profile;
					cmDefaultProfile = getAssciatedProfile(selectedModel);
					profileViewer.setSelection(new StructuredSelection(cmDefaultProfile));
				}else if (profile instanceof DataModelWrapper) {
					selectedModel = null;					
				}
				if (selectedModel == null) {
					context.set(ConfigurableModel.class, new ConfigurableModel());
				}else {
					context.set(ConfigurableModel.class, selectedModel);
				}
				{ if (!isInit) validate();}
			}
		});

		
		Label lblProfile = new Label(g, SWT.NONE);
		lblProfile.setText(Messages.SmartCollectPackageConfigurator_DeviceSettingsLabel);

		Composite c = new Composite(g, SWT.NONE);
		c.setLayout(new GridLayout(2, false));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		profileViewer = new ComboViewer(c, SWT.READ_ONLY);
		profileViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		profileViewer.setContentProvider(ArrayContentProvider.getInstance());
		profileViewer.setLabelProvider(new CtProfileLabelProvider());
		profileViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
		profileViewer.setSelection(new StructuredSelection(DialogConstants.LOADING_TEXT));
		profileViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				{ if (!isInit) validate();}
			}
		});
		
		ToolBar tb = new ToolBar(c, SWT.FLAT);
		ToolItem tiEdit = new ToolItem(tb,SWT.PUSH);
		tiEdit.setToolTipText(Messages.SmartCollectPackageConfigurator_EditSettingsTooltip);
		tiEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		tiEdit.addListener(SWT.Selection, e->{
			Object x = profileViewer.getStructuredSelection().getFirstElement();
			if (!(x instanceof CyberTrackerPropertiesProfile)) return;
			
			Dialog dialog = new CyberTrackerPropertiesDialog(c.getShell(), (CyberTrackerPropertiesProfile)x);
			dialog.open();
		});
	
		Label lblPrivate = new Label(g, SWT.NONE);
		lblPrivate.setText(Messages.SmartCollectPackageConfigurator_PrivatePkgLabel);
		lblPrivate.setToolTipText(Messages.SmartCollectPackageConfigurator_PrivatePkgTooltip);
		
		btnPrivate = new Button(g, SWT.CHECK);
		btnPrivate.addListener(SWT.Selection, e->{
			if (!isInit) validate();
			updateUrl();
		});
	
		new Label(g, SWT.NONE);
		
		Composite urlpart = new Composite(g, SWT.NONE);
		urlpart.setLayout(new GridLayout(2, false));
		urlpart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)urlpart.getLayout()).marginWidth = 0;
		((GridLayout)urlpart.getLayout()).marginHeight = 0;
		
		txtUrl = new Text(urlpart, SWT.WRAP | SWT.READ_ONLY | SWT.BORDER);
		txtUrl.setEditable(false);
		txtUrl.setEnabled(false);
		txtUrl.setText(""); //$NON-NLS-1$
		txtUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtUrl.getLayoutData()).heightHint = 50;
		
		privateTb = new ToolBar(urlpart, SWT.FLAT);
		privateTb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		ToolItem btnCopy = new ToolItem(privateTb, SWT.PUSH);
		btnCopy.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.COPY_ICON));
		btnCopy.setToolTipText(Messages.SmartCollectPackageConfigurator_copytooltip);
		btnCopy.addListener(SWT.Selection,e->{
			txtUrl.setRedraw(false);
			txtUrl.selectAll();
			txtUrl.copy();
			txtUrl.clearSelection();
			txtUrl.setRedraw(true);
			
		});
		
		new Label(g, SWT.NONE);
		
		lblWarn = new Label(g, SWT.WRAP);
		lblWarn.setText(Messages.SmartCollectPackageConfigurator_urltooltip);
		lblWarn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblWarn.getLayoutData()).widthHint = 50;

		Composite ct = new Composite(g, SWT.NONE);
		ct.setLayout(new GridLayout(3, false));
		((GridLayout)ct.getLayout()).marginWidth = 0;
		((GridLayout)ct.getLayout()).marginHeight = 0;
		ct.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		//main page contributions
		if (contributions != null) {
			for (IPackageUiContribution cc : contributions) {
				if (!cc.isTab()) {
					Composite part = cc.createUi(main, ctpackage, e->validate(), ()->validate(false));
					if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));	
				}
			}
		}
		
		scroll.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		//tab contributaions
		if (contributions != null) {
			for (IPackageUiContribution cc : contributions) {
				if (cc.isTab()) {
					CTabItem item = new CTabItem(tabs, SWT.NONE);
					item.setText(cc.getTabName());
					Composite all = new Composite(tabs, SWT.NONE);
					all.setLayout(new GridLayout());
					item.setControl(all);
					Composite part = cc.createUi(all, ctpackage, e->validate(), ()->validate(false));
					if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				}
			}
		}
		
		loadData();
	}
	
	private void updateUrl() {
		lblWarn.setEnabled(btnPrivate.getSelection());
		txtUrl.setEnabled(btnPrivate.getSelection());
		privateTb.setEnabled(btnPrivate.getSelection());
		if (!txtUrl.getEnabled()) {
			txtUrl.setText(""); //$NON-NLS-1$
		}else {
			if (ctpackage.getUuid() == null) {
				txtUrl.setText(Messages.SmartCollectPackageConfigurator_saverequired);	
			}else {
				ConnectServer cs;
				try(Session s = HibernateManager.openSession()){
					cs = ConnectHibernateManager.getConnectServer(s);
				}
				if (cs == null) {
					txtUrl.setText(Messages.SmartCollectPackageConfigurator_connectnotconfigured);
				}else {
					String surl = cs.getServerUrl();
				
					try {
						URL url = new URL(surl);
						String link = SmartCollectPackage.generateSmartMobileAppLink(url, ctpackage.getUuid());
						txtUrl.setText(link);
					}catch (Exception ex) {
						SmartCollectPlugIn.log(ex.getMessage(), ex);
						txtUrl.setText(Messages.SmartCollectPackageConfigurator_urlerror);
					}
				}
			}
		}
	}
	
	private CyberTrackerPropertiesProfile getAssciatedProfile(Object src) {
		try (Session session = HibernateManager.openSession()){
			if (src instanceof ConfigurableModel) {
				ConfigurableModel cm = (ConfigurableModel) src;
				ConfigurableModelCtPropertiesProfile cmctp = CyberTrackerHibernateManager.getAssociatedCmProfile(session, cm);
				CyberTrackerPropertiesProfile  profile = cmctp.getProfile();
				profile.equals(profile);
				return profile;
			} else {
				CyberTrackerPropertiesProfile profile = CyberTrackerHibernateManager.getDefaultProfile(session);
				profile.equals(profile);
				return profile;
			}
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.SmartCollectPackageConfigurator_DeviceSettingsError, ex);
			return null;
		}
	}
	
	private void validate() {
		validate(true);
	}
	
	private void validate(boolean modified) {
		if (modified) onModified.accept(true);
		
		try {
			if (txtName.getText().isBlank()) {
				throw new Exception(Messages.SmartCollectPackageConfigurator_NameRequired);
			}
		
			if (modelViewer.getSelection().isEmpty()) {
				throw new Exception(Messages.SmartCollectPackageConfigurator_CmRequired);
			}
		
			if (profileViewer.getSelection().isEmpty()) {
				throw new Exception(Messages.SmartCollectPackageConfigurator_SettingsRequired);
			}
			for (IPackageUiContribution cc : contributions) {
				String x = cc.isValid();
				if (x != null) throw new Exception(x);
			}
		}catch (Exception ex) {
			onValidate.accept(ex.getMessage());
			return;
		}
		onValidate.accept(null);
	}

	@Override
	public Composite createDetails(Composite parent, ICtPackage ctpackage,
			List<ICtPackagePropertyProvider> properties) {
		Composite all = new Composite(parent, SWT.NONE);
		all.setLayout(new GridLayout());
		((GridLayout)all.getLayout()).marginWidth = 0;
		((GridLayout)all.getLayout()).marginHeight = 0;
		all.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		
		
		
		try(Session session = HibernateManager.openSession()){
			SmartCollectPackage local = session.get(SmartCollectPackage.class, ((SmartCollectPackage)ctpackage).getUuid());
			if (local == null) return all;
			
			Label header = new Label(all, SWT.NONE);
			header.setText(local.getName());
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			header.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			FontData fd = header.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			fd.setHeight(fd.getHeight() + 1);
			Font f = new Font(parent.getDisplay(), fd);
			header.addListener(SWT.Dispose, e->f.dispose());
			header.setFont(f);
			
			ScrolledComposite scroll = new ScrolledComposite(all, SWT.V_SCROLL | SWT.H_SCROLL );
			scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			scroll.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			Composite inner = new Composite(scroll, SWT.NONE);
			scroll.setContent(inner);
			inner.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			inner.setLayout(new GridLayout());
			((GridLayout)inner.getLayout()).marginWidth = 0;
			((GridLayout)inner.getLayout()).marginHeight = 0;
			
			Label l = new Label(inner, SWT.NONE);
			l.setText(Messages.SmartCollectPackageConfigurator_SecuritySection);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l = new Label(inner, SWT.NONE);
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			boolean isPrivate = false;
			for (MetadataFieldValue v : local.getMetadataValues()) {
				if (v.getMetadataKey().equals(ICtPackage.PRIVATE_PROP_KEY)) {
					isPrivate = v.getBooleanValue();
					break;
				}
			}
			if (isPrivate) {
				l.setText(Messages.SmartCollectPackageConfigurator_PrivatePackage);
			}else {
				l.setText(Messages.SmartCollectPackageConfigurator_PublicPackage);
			}
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l = new Label(inner, SWT.NONE);
			l.setText(Messages.SmartCollectPackageConfigurator_CMLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l = new Label(inner, SWT.NONE);
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			if (local.getConfigurableModel() != null) {
				l.setText(local.getConfigurableModel().getName());
			}else {
				l.setText( Messages.SmartCollectPackageConfigurator_DataModelLabel );
			}
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			l= new Label(inner, SWT.NONE);
			l.setText(Messages.SmartCollectPackageConfigurator_DeviceSettingsLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			l= new Label(inner, SWT.NONE);
			l.setText(local.getCtProfile().getName());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			for ( IPackageContribution c : PackageContributionManager.INSTANCE.getContributionItems()) {
				c.createDetails(inner, local, session);
			}
			
			l= new Label(inner, SWT.NONE);
			l.setText(Messages.SmartCollectPackageConfigurator_PackageDetails);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).verticalIndent = 5;
			
			Path p = null;
			try {
				p = local.getLocalFile();
				if (p != null) {
					String fname = p.getFileName().toString();
					String revision = fname.substring(fname.indexOf('.')+1,fname.lastIndexOf('.'));
					int index = revision.indexOf('.');
					String date = revision.substring(index+1);
					revision = revision.substring(0,index);
					DateTimeFormatter sdf = DateTimeFormatter.ofPattern(ICtPackage.PACKAGE_DATE_FORMAT);
					
					Composite temp = new Composite(inner, SWT.NONE);
					temp.setLayout(new GridLayout(2, false));
					temp.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					((GridLayout)(temp.getLayout())).marginWidth = 0;
					((GridLayout)(temp.getLayout())).marginHeight = 0;
					temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					
					l = new Label(temp, SWT.NONE);
					l.setText(Messages.SmartCollectPackageConfigurator_Date);
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText( DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).format( LocalDateTime.parse(date,sdf)));
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
					
					l = new Label(temp, SWT.NONE);
					l.setText(Messages.SmartCollectPackageConfigurator_Version);
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText(revision);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}else{
					l= new Label(inner, SWT.NONE);
					l.setText(Messages.SmartCollectPackageConfigurator_NoPackage);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}
			}catch (IOException | DateTimeParseException ex) {
				l= new Label(inner, SWT.NONE);
				l.setText(Messages.SmartCollectPackageConfigurator_UnknownErrorLabel);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			}
			
			for (ICtPackagePropertyProvider pp : properties) {
				pp.getProperties();
			
				l= new Label(inner, SWT.NONE);
				l.setText(pp.getName());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).verticalIndent = 5;
				
				Composite temp = new Composite(inner, SWT.NONE);
				temp.setLayout(new GridLayout(2, false));
				temp.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				((GridLayout)(temp.getLayout())).marginWidth = 0;
				((GridLayout)(temp.getLayout())).marginHeight = 0;
				temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				
				for (ICtPackageProperty pprop : pp.getProperties()) {
					String value = pprop.getValue(ctpackage);
					if (value == null || value.isBlank()) continue;
							
					l = new Label(temp, SWT.NONE);
					l.setText(pprop.getShortName()  + ":"); //$NON-NLS-1$
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));

					l= new Label(temp, SWT.NONE);
					l.setText( value);
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					l.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
				}
			}
			
			
			scroll.setExpandHorizontal(true);
			scroll.setExpandVertical(true);
			scroll.setMinSize(inner.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}
		return all;
	}

	@Override
	public void save() throws Exception {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				Object first = modelViewer.getStructuredSelection().getFirstElement();
				if (first instanceof ConfigurableModel) {
					ctpackage.setConfigurableModel((ConfigurableModel) first);
				}else {
					ctpackage.setConfigurableModel(null);
				}
				ctpackage.setCtProfile((CyberTrackerPropertiesProfile) profileViewer.getStructuredSelection().getFirstElement());
				ctpackage.setName(txtName.getText());
				
				MetadataFieldValue privatemd = null;
				for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
					if (v.getMetadataKey().equals(ICtPackage.PRIVATE_PROP_KEY)){
						privatemd = v;
						break;
					}
				}
				if (privatemd == null) {
					privatemd = new MetadataFieldValue();
					privatemd.setMetadataKey(ICtPackage.PRIVATE_PROP_KEY);
					privatemd.setCtPackage(ctpackage);
					privatemd.setConservationArea(ctpackage.getConservationArea());
					if (ctpackage.getMetadataValues() == null) ctpackage.setMetadataValues(new ArrayList<>());
					ctpackage.getMetadataValues().add(privatemd);
				}
				privatemd.setBooleanValue(btnPrivate.getSelection());
				
				session.saveOrUpdate(ctpackage);
				session.flush();
				for (IPackageUiContribution cc : contributions) {
					cc.updatePackage(ctpackage);
				}
				session.getTransaction().commit();
				
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw ex;
			}
		}
		updateUrl();
	}
	
	
	private void loadData() {		
    	
		Job j = new Job("loading data") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<Object> modelList = new ArrayList<>();
				List<CyberTrackerPropertiesProfile>  profiles = new ArrayList<>();
				DataModelWrapper dm = new DataModelWrapper();
				
				SmartCollectPackage init = null;
				try(Session session = HibernateManager.openSession()){
					List<ConfigurableModel> models = DataentryHibernateManager.getConfigurableModels(session);
					models.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(),  b.getName().toLowerCase()));
					modelList.addAll(models);
					modelList.add(dm);
					
					profiles.addAll(CyberTrackerHibernateManager.getPropertiesProfiles(session));
					
					if (ctpackage != null && ctpackage.getUuid() != null) {
						init = session.get(SmartCollectPackage.class, ctpackage.getUuid());
						if (init.getConfigurableModel() != null) init.getConfigurableModel().getUuid();
						if (init.getCtProfile() != null) init.getCtProfile().getUuid();
					}else if (ctpackage != null) {
						init = ctpackage;
						if (init.getConfigurableModel() != null) {
							init.setConfigurableModel(session.get(ConfigurableModel.class, init.getConfigurableModel().getUuid()));
							init.getConfigurableModel().getUuid();
						}
						if (init.getCtProfile() != null) {
							init.setCtProfile(session.get(CyberTrackerPropertiesProfile.class, init.getCtProfile().getUuid()));
							init.getCtProfile().getUuid();
						}
					}
					if (init.getMetadataValues() != null) init.getMetadataValues().size();
				}
				
				profiles.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));

				if (init.isDataModel()) {
					context.set(ConfigurableModel.class, new ConfigurableModel());
				}else {
					context.set(ConfigurableModel.class, init.getConfigurableModel());
				}
				SmartCollectPackage finit = init;
				
				Display.getDefault().syncExec(()->{
					try {
						isInit = true;
				
						profileViewer.setInput(profiles);
						modelViewer.setInput(modelList);
						
						if (finit.getConfigurableModel() != null) {
							modelViewer.setSelection(new StructuredSelection(finit.getConfigurableModel()));
						}else {
							modelViewer.setSelection(new StructuredSelection(dm));
						}
						
						if (finit.getCtProfile() != null) {
							profileViewer.setSelection(new StructuredSelection(finit.getCtProfile()));
						}else {
							if (!profiles.isEmpty()) profileViewer.setSelection(new StructuredSelection(profiles.get(0)));
						}
						
						for (MetadataFieldValue v : finit.getMetadataValues()) {
							if (v.getMetadataKey().equalsIgnoreCase(ICtPackage.PRIVATE_PROP_KEY)) {
								btnPrivate.setSelection(v.getBooleanValue());
							}
						}
						updateUrl();
						
						validate(false);
					}finally {
						isInit = false;
					}
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}

}
