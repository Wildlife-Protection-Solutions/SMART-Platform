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
package org.wcs.smart.query.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.util.UuidUtils;

/**
 * Dialog box for modifying query information.  This includes the query
 * name and the columns in the query.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryPropertiesDialog extends TitleAreaDialog {

	private List<AbstractQueryPropertyProvider> props;
	private Query query;
	private IQueryType qType;
	private Text txtName;
	private HashMap<Language, String> names;
	
	Listener changeListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			setChangesMade(true);
		}
	};

	/**
	 * @param parent the parent shell
	 * @param query the query to update
	 * @param allCollumns all columns available to the query
	 */
	public QueryPropertiesDialog(Shell parent, 
			Query query) {
		super(parent);
		this.query = query;
		this.qType = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
		
		if (query.getUuid() != null){
			//load translations from db
			Session s = HibernateManager.openSession();
			s.clear();
			try{
				s.beginTransaction();
				
				/* load query names */
				s.saveOrUpdate(query);
				query.getNames().size();
				
				Query tmp = (Query) s.load(Query.class, this.query.getUuid());
				this.names = new HashMap<Language, String>();
				for (org.wcs.smart.ca.Label l : tmp.getNames()){
					names.put(l.getLanguage(), l.getValue());
				}
				this.names.put(SmartDB.getCurrentLanguage(), tmp.getName());
				
				s.getTransaction().rollback();				
			}finally{
				s.close();
			}
		}else{
			//load translations from object
			this.names = new HashMap<Language, String>();
			for (org.wcs.smart.ca.Label l : query.getNames()){
				names.put(l.getLanguage(), l.getValue());
			}
			this.names.put(SmartDB.getCurrentLanguage(), query.getName());
		}
	}
	
	@Override
	public boolean close(){
		boolean canClose = super.close();
		for (AbstractQueryPropertyProvider p : props){
			p.clearChangeListener();
		}
		return canClose;
	}
	
	
	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if (performSave()){
				okPressed();
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			cancelPressed();
		}
	}
	
	@Override 
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		
		if (p.y > 650){
			p.y = 650;
		}else if (p.y < 400){
			p.y = 400;
		}
		if (p.x > 500){
			p.x = 500;
		}
		
		return p;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
		btnOk.setEnabled(false);
	}

	/**
	 * enables or disables the save button based changesMade
	 * @param changesMade 
	 */
	private void setChangesMade(boolean changesMade){
		getButton(IDialogConstants.OK_ID).setEnabled(changesMade);
	}
	
	/**
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#createContent(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Composite createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		getShell().setText(Messages.QueryPropertiesDialog_DialogTitle);
		setTitle(Messages.QueryPropertiesDialog_DialogTitle);
		setMessage(Messages.QueryPropertiesDialog_DialogMessage);
		
		ScrolledComposite scroll = new ScrolledComposite(parent,  SWT.V_SCROLL);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		Composite main = new Composite(scroll, SWT.NONE);
		
		
		GridLayout gl = new GridLayout(2, false);
		gl.marginTop = 10;
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblName = new Label(main, SWT.NONE);
		lblName.setText(Messages.QueryPropertiesDialog_QueryNameLabel);
		lblName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Composite tmp = new Composite(main, SWT.NONE);
		gl = new GridLayout(2, false);
		gl.marginBottom = gl.marginHeight = gl.marginLeft = gl.marginRight = gl.marginWidth = gl.marginTop = 0;
		tmp.setLayout(gl);
		tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName = new Text(tmp, SWT.BORDER);
		txtName.setText(query.getName());
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
			}
		});
		
		Link lnkTranslate = new Link(tmp, SWT.NONE);
		lnkTranslate.setText("<a>" + Messages.QueryPropertiesDialog_TranslateLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		lnkTranslate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				NamedItem toUpdate = new NamedItem();
				for(Entry<Language, String> l : QueryPropertiesDialog.this.names.entrySet()){
					toUpdate.updateName(l.getKey(), l.getValue());
					
				}
				TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(
						getShell(), toUpdate);
				if (dialog.open() == TranslateSimpleListItemDialog.OK){
					txtName.setText(toUpdate.getName());
					 QueryPropertiesDialog.this.names.clear();
					 for (org.wcs.smart.ca.Label l : toUpdate.getNames()){
						 QueryPropertiesDialog.this.names.put(l.getLanguage(), l.getValue());	 
					 }
					setChangesMade(true);
				}
				
				
			}
		});
		Label lblSystemId = new Label(main, SWT.NONE);
		lblSystemId.setText("System ID:");
		Label lblUuid = new Label(main, SWT.NONE);
		lblUuid.setText(UuidUtils.uuidToString(query.getUuid()));
		Label lblOwner = new Label(main, SWT.NONE);
		lblOwner.setText(Messages.QueryPropertiesDialog_CreatorLabel);
		Label lblOwnerName = new Label(main, SWT.NONE);
		lblOwnerName.setText(SmartLabelProvider.getFullLabel(query.getOwner()));
		

		
		List<AbstractQueryPropertyProvider> allProps = new ArrayList<AbstractQueryPropertyProvider>();
		allProps.addAll(QueryPlugIn.getPropertyProviders());
		
		//	sort so simple names are first, then more complex composites
		Collections.sort(allProps, new Comparator<AbstractQueryPropertyProvider>() {
			@Override
			public int compare(AbstractQueryPropertyProvider o1,
					AbstractQueryPropertyProvider o2) {
				if (o1.getName() == null && o2.getName() == null){
					return 0;
				}else if (o1.getName() == null && o2.getName() != null){
					return 1;
				}else if (o1.getName() != null && o2.getName() == null){
					return -1;
				}else{
					return o1.getName().compareTo(o2.getName());
				}
			}
		});
		props = new ArrayList<AbstractQueryPropertyProvider>();
		for(AbstractQueryPropertyProvider prop: allProps){
			if (prop.isValid(qType)){
				//prop.clearChangeListener();
				props.add(prop);
				
				prop.addChangeListener(changeListener);
				if (prop.getName() != null){
					Label lblProp = new Label(main, SWT.NONE);
					lblProp.setText(prop.getName()+": "); //$NON-NLS-1$
					lblProp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
				
					Label lblText = new Label(main, SWT.WRAP);
					lblText.setText(prop.getValue(query));
					lblText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
				}else{
					Composite c = prop.createComposite(main, query);
					c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				}
			}
		}
		
		scroll.setMinSize(150,main.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		scroll.setContent(main);

		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		return parent;
	}


	
	/**
	 * Updates the query.
	 * 
	 * @see org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog#performSave()
	 */
	protected boolean performSave() {
		
		//update names
		if (!query.getName().equals(txtName.getText())){
			query.setName(txtName.getText());
		}
		for (Entry<Language, String> l : names.entrySet()){
			query.updateName(l.getKey(), l.getValue());
		}
		//remove any languages no longer valid
		Set<org.wcs.smart.ca.Label> toRemove = new HashSet<org.wcs.smart.ca.Label>();
		for (org.wcs.smart.ca.Label l : query.getNames()){
			if (names.get(l.getLanguage()) == null){
				toRemove.add(l);
			}
		}
		for (org.wcs.smart.ca.Label l : toRemove){
			query.getNames().remove(l);
		}
		
		for (AbstractQueryPropertyProvider provider:props){
			provider.save(query, null);
		}

		setChangesMade(false);
		return true;
	}

	/*
	 * Creates checkbox table viewer for selecting columns
	 */
	
	
	/**
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 * @return <code>true</code>
	 */
	@Override
	public boolean isResizable(){
		return true;
	}
}
