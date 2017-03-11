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
package org.wcs.smart.i2.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.search.IIntelEntitySearch;
import org.wcs.smart.i2.search.LoadSavedSearches;
import org.wcs.smart.i2.search.SearchProxy;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for saving searches
 * 
 * @author Emily
 *
 */
public class SaveSearchDialog extends TitleAreaDialog{

	private String NEW_QUERY_OP = Messages.SaveSearchDialog_CreateQueryOption;
	
	private Label newQuery;
	private Text newQueryName;
	
	private ComboViewer cmbQueries;
	private IIntelEntitySearch isearch;
	
	private IntelEntitySearch savedSearch;
	private IntelEntitySearch defaultSelection;
	
	@Inject
	private IEventBroker broker;
	
	private LoadSavedSearches loadSearches = new LoadSavedSearches() {
		@Override
		public void searchesLoaded(List<SearchProxy> queries) {
			List<Object> input = new ArrayList<>();
			input.add(NEW_QUERY_OP);
			input.addAll(queries);
			
			Display.getDefault().syncExec(()->{
				cmbQueries.setInput(input);
				if(defaultSelection == null){
					cmbQueries.setSelection(new StructuredSelection(NEW_QUERY_OP));
				}else{
					cmbQueries.setSelection(new StructuredSelection(new SearchProxy(defaultSelection.getUuid(), null)));
				}
			});
		}
	};
	
	public SaveSearchDialog(Shell parentShell, IIntelEntitySearch search, IntelEntitySearch defaultSelection) {
		super(parentShell);
		this.isearch = search;
		this.defaultSelection = defaultSelection;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.SaveSearchDialog_SaveAsLabel);
		
		cmbQueries = new ComboViewer(main, SWT.READ_ONLY);
		cmbQueries.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbQueries.setContentProvider(ArrayContentProvider.getInstance());
		cmbQueries.setInput(new String[]{DialogConstants.LOADING_TEXT});
		cmbQueries.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof SearchProxy){
					return ((SearchProxy) element).getName();
				}
				return super.getText(element);
			}
		});
		
		cmbQueries.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object x = ((IStructuredSelection)event.getSelection()).getFirstElement();
				newQuery.setEnabled(x == NEW_QUERY_OP);
				newQueryName.setEnabled(x == NEW_QUERY_OP);
				
				getButton(IDialogConstants.OK_ID).setEnabled( (x == NEW_QUERY_OP && !newQueryName.getText().trim().isEmpty()) || x instanceof SearchProxy);
			}
		});
		
		newQuery = new Label(main, SWT.NONE);
		newQuery.setText(Messages.SaveSearchDialog_NewQueryLabel);
		newQuery.setEnabled(false);
		
		newQueryName = new Text(main, SWT.BORDER);
		newQueryName.setText(Messages.SaveSearchDialog_DefaultNewQueryName);
		newQueryName.setEnabled(false);
		newQueryName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		loadSearches.schedule();
		
		setTitle(Messages.SaveSearchDialog_Title);
		getShell().setText(Messages.SaveSearchDialog_Title);
		setMessage(Messages.SaveSearchDialog_Message);
		return parent;
	}

	public void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public void okPressed(){
		Object x = ((IStructuredSelection)cmbQueries.getSelection()).getFirstElement();
		if (x == NEW_QUERY_OP){
			IntelEntitySearch search = new IntelEntitySearch();
			search.setConservationArea(SmartDB.getCurrentConservationArea());
			search.setName(newQueryName.getText().trim());
			search.updateName(SmartDB.getCurrentLanguage(), search.getName());
			search.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), search.getName());
			search.setSearchString(isearch.serialize());
			
			Session session = HibernateManager.openSession();
			try{
				session.beginTransaction();
				session.save(search);
				session.getTransaction().commit();
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(Messages.SaveSearchDialog_SaveError + ex.getMessage(), ex);
				return;
			}finally{
				session.close();
			}
			savedSearch = search;
			broker.send(IntelEvents.ENTITY_SEARCH_NEW, search);
			super.okPressed();
			return;
		}else if (x instanceof SearchProxy){
			IntelEntitySearch eSearch = null;
			Session session = HibernateManager.openSession();
			try{
				session.beginTransaction();
				eSearch = (IntelEntitySearch) session.get(IntelEntitySearch.class, ((SearchProxy) x).getUuid());
				eSearch.setSearchString(isearch.serialize());
				session.getTransaction().commit();
			}catch (Exception ex){
				Intelligence2PlugIn.displayLog(Messages.SaveSearchDialog_OverwriteError + ex.getMessage(), ex);
				return;
			}finally{
				session.close();
			}
			savedSearch = eSearch;
			broker.send(IntelEvents.ENTITY_SEARCH_MODIFIED, eSearch);
			
			super.okPressed();
			return;
		}
	}
	
	public IntelEntitySearch getSavedSearch(){
		return this.savedSearch;
	}
		
}
