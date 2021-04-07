package org.wcs.smart.ui.internal.preference;

import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.IdGeneratorContribution;
import org.wcs.smart.IdGeneratorManager;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;

public class IDTokenPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.preference.IdTokens"; //$NON-NLS-1$

	private List<IdGeneratorContribution> contributions;
	
	public IDTokenPreferencePage() {
	}

	public IDTokenPreferencePage(String title) {
		super(title);
	}

	public IDTokenPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
		contributions = IdGeneratorManager.INSTANCE.getContributions();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		
		for (IdGeneratorContribution item : contributions) {
			Composite part = item.createComposite(c);
			part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		}
		
		try(Session session = HibernateManager.openSession()){
			for (IdGeneratorContribution item : contributions) item.initComponent(session);	
		}
		
		return c;
	}

	@Override
	protected void performDefaults() {
		
	}
	
	@Override
	public boolean performOk() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (IdGeneratorContribution item : contributions) item.save(session);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				SmartPlugIn.displayLog(ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}
}
