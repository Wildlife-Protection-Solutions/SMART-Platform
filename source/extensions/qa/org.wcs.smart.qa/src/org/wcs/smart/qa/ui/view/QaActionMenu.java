/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.view;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.wcs.smart.qa.ActionEngine;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.QaActionInfo;
import org.wcs.smart.qa.model.QaError;

/**
 * Generates menu items based on selection provided
 * by selection provider.
 * 
 * @author Emily
 *
 */
public abstract class QaActionMenu implements MenuListener {

	protected List<MenuItem> newItems = new ArrayList<>();
	protected ISelectionProvider selectionProvider;
	protected IEclipseContext context;
	protected Menu parent;
	
	public QaActionMenu(Menu parent, IEclipseContext context, ISelectionProvider selectionProvider ){
		this.selectionProvider = selectionProvider;
		this.context = context;
		this.parent = parent;
		parent.addMenuListener(this);
	}
	
	@Override
	public void menuShown(MenuEvent e) {
		for (MenuItem mi : newItems){
			mi.dispose();
		}
		newItems.clear();
		
		if (selectionProvider.getSelection().isEmpty()) return;
		boolean isSingle = ((IStructuredSelection)selectionProvider.getSelection()).size() == 1;
		
		Map<String, QaActionInfo> actions = new HashMap<>();
		boolean isFirst = true;
		for (Iterator<?> iterator = ((IStructuredSelection)selectionProvider.getSelection()).iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (!(item instanceof QaError)) continue;
			
			QaError errorItem = (QaError)item;
			
			if (isFirst){
				for (QaActionInfo action : InternalExtensionManager.INSTANCE.getQaActions(errorItem.getDataProvider(), context)){
					if(isSingle || action.getAction().supportsMultiple()){
						actions.put(action.getAction().getId(), action);
					}
				}
				isFirst = false;
			}else{
				List<String> toRemove = new ArrayList<>();
				for (Iterator<String> iterator2 = actions.keySet().iterator(); iterator2.hasNext();) {
					String ea = (String) iterator2.next();
					boolean found = false;
					for (QaActionInfo action : InternalExtensionManager.INSTANCE.getQaActions(errorItem.getDataProvider(), context)){
						if(action.getAction().supportsMultiple()){
							if (action.getAction().getId().equals(ea)) found = true;
						}
					}
					if (!found){
						toRemove.add(ea);
					}
				}
				for (String s : toRemove){
					actions.remove(s);
				}
			}
		}
		actions.put(QaActionInfo.IGNORE_INSTANCE.getAction().getId(), QaActionInfo.IGNORE_INSTANCE);
		
		List<QaActionInfo> sortedActions = new ArrayList<>();
		sortedActions.addAll(actions.values());
		sortedActions.sort((a,b)->{
			return Collator.getInstance().compare(a.getAction().getName(Locale.getDefault()), b.getAction().getName(Locale.getDefault()));
		});
		
		for (QaActionInfo action : sortedActions){
			if (newItems.size() == 0){
				newItems.add(new MenuItem(parent, SWT.SEPARATOR));
			}
			MenuItem mi = new MenuItem(parent, SWT.PUSH);
			mi.setText(action.getAction().getName(Locale.getDefault()));
			if (action.getImage() != null) {
				mi.setImage(action.getImage().createImage());
				mi.addListener(SWT.Dispose, ev->{
					if (mi.getImage() != null && !mi.getImage().isDisposed()) mi.getImage().dispose();
				});
			}
			newItems.add(mi);
		
			final IStructuredSelection thisSelection =  (IStructuredSelection)selectionProvider.getSelection();
			mi.addListener(SWT.Selection, event->{
				List<QaError> items = new ArrayList<QaError>();
				for (Iterator<?> iterator3 = thisSelection.iterator(); iterator3.hasNext();) {
					Object x = iterator3.next();
					if (x instanceof QaError){
						items.add((QaError) x);
					}
				}
				if (ActionEngine.INSTANCE.performActions(items, action.getAction().getId(), context)){
					refresh(items);
				}else{
					refresh(null);
				}
			});		
		}				
	}

	@Override
	public void menuHidden(MenuEvent e) {

	}
	
	/**
	 * called when actions are complete;
	 * @param modified a list of items modified, can be null if nothing modified
	 */
	public abstract void refresh(List<QaError> modified);

}