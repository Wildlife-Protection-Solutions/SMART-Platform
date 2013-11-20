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
package org.wcs.smart.query.ui.querylist;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryFolder;

/**
 * Command handler for adding a new folder to 
 * the query list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AddFolderHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ISelection thisSelection = HandlerUtil.getCurrentSelection(event);
		if (thisSelection == null || thisSelection.isEmpty() || !(thisSelection instanceof IStructuredSelection) ){
			return null;
		}
		
		Object o = ((IStructuredSelection)thisSelection).getFirstElement();
		if (o instanceof QueryFolder){
			QueryFolder newFolder = addQueryFolder((QueryFolder)o);
			if (newFolder != null){
				QueryEventManager.getInstance().fireFolderAdded(newFolder);
			}
		}
		
		return null;
		
	}
	
	public static QueryFolder addQueryFolder(QueryFolder parent){
		
		QueryFolder newFolder = new QueryFolder();
		
		newFolder.setConservationArea(parent.getConservationArea());
		newFolder.setEmployee(parent.getEmployee());
		if (!parent.isRootFolder()){
			newFolder.setParentFolder(parent);
		}
		if (parent.getChildren() == null){
			parent.setChildren(new ArrayList<QueryFolder>());
		}
		parent.getChildren().add(newFolder);

		Label lbl = new Label();
		lbl.setElement(newFolder);
		lbl.setLanguage(SmartDB.getCurrentLanguage());
		lbl.setValue(Messages.AddFolderHandler_DefaultNewFolderName);
		newFolder.setNames(new HashSet<Label>());
		newFolder.getNames().add(lbl);
		newFolder.setName(lbl.getValue());
		
		if (!SmartDB.getCurrentLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage())){
			newFolder.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), Messages.AddFolderHandler_DefaultNewFolderName);
		}
		
		//need to save and refresh query list view
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			s.save(newFolder);
			s.save(lbl);
			s.getTransaction().commit();
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.AddFolderHandler_CouldNotAddFolderError + ex.getLocalizedMessage(), ex);
			return null;
		}finally{
			s.close();
		}
		
		return newFolder;

	}

}
