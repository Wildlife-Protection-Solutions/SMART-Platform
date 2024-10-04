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
package org.wcs.smart.observation.query.ui.definition;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.filter.Operator;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.observation.query.model.IncidentTypeProviderManager;
import org.wcs.smart.observation.query.model.QueryIncidentType;
import org.wcs.smart.observation.query.ui.itempanel.GeneralContentProvider;
import org.wcs.smart.query.model.filter.WaypointCmFilter;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;
/**
 * Drop item for filtering on waypoint source field.
 * @author Emily
 *
 */
public class WaypointListOpFilterDropItem extends DropItem implements IFilterDropItem{

	private ComboViewer listViewer;
	private ComboViewer opViewer;
	
	private Font smallerFont;
	
	private Object currentSelection = null;
	private Operator currentOperator = null;
	
	/**
	 * If this drop item represents the waypoint source or configurable model field
	 * @author Emily
	 *
	 */
	public enum Type{
		SOURCE,
		INCIDENTTYPE,
		CM
	}
	
	private Type type;
	
	/**
	 * Creates waypoint list drop item
	 */
	public WaypointListOpFilterDropItem(Type type) {
		super();
		this.type = type;
	}

	
	/**
	 * @param data - an array of the {Operator, Object} source or {Object} cm
	 */
	public void initializeData(Object data){
		if (this.type == Type.SOURCE) {
			currentOperator = (Operator) ((Object[])data)[0];
			currentSelection = ((Object[])data)[1];
		}else if (this.type == Type.INCIDENTTYPE) {
			currentOperator = (Operator) ((Object[])data)[0];
			currentSelection = ((Object[])data)[1];
		}else if(this.type == Type.CM) {
			currentSelection = ((Object[])data)[0];
		}
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		super.dispose();
		if (smallerFont != null){
			smallerFont.dispose();
		}
		listViewer = null;
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#getText()
	 */
	@Override
	public String getText() {
		if (this.type == Type.SOURCE) {
			return "wpn:src" + " " + opViewer.getCombo().getText() + " " + listViewer.getCombo().getText(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else if (this.type == Type.INCIDENTTYPE) {
			return "wpn:incidenttype = " + listViewer.getCombo().getText(); //$NON-NLS-1$ 
		}else if (this.type == Type.CM) {
			Object li = listViewer.getStructuredSelection().getFirstElement();
			if (li == null) {
				return WaypointCmFilter.KEY + " = "; //$NON-NLS-1$
			}else if (li instanceof ConfigurableModel){
				return WaypointCmFilter.KEY + " = " + ((ConfigurableModel) li).getName(); //$NON-NLS-1$
			}else if (li instanceof ListItem){
				return WaypointCmFilter.KEY + " = " + ((ListItem) li).getName(); //$NON-NLS-1$
			}
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#asQueryPart()
	 */
	@Override
	public String asQueryPart() {
		if (this.type == Type.SOURCE ) {
			
			StringBuilder query = new StringBuilder("wpn:src"); //$NON-NLS-1$
			query.append(" "); //$NON-NLS-1$
			query.append(currentOperator.asSmartValue());
			query.append(" "); //$NON-NLS-1$
			
			IWaypointSource it = null;
			if (currentSelection != null){
				it = (IWaypointSource)currentSelection;
			}else{
				IStructuredSelection sel = (IStructuredSelection) listViewer.getSelection();
				if (sel != null && !sel.isEmpty()){
					it = (IWaypointSource) sel.getFirstElement();
				}
			}
			if (it != null){
				query.append("\"" + it.getKey() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return query.toString();
		}else if (type == Type.INCIDENTTYPE) {
			
			StringBuilder query = new StringBuilder("wpn:incidenttype "); //$NON-NLS-1$
			query.append(Operator.EQUALS.asSmartValue());
			
			QueryIncidentType it = null;
			if (currentSelection != null){
				it = (QueryIncidentType)currentSelection;
			}else{
				it = (QueryIncidentType) listViewer.getStructuredSelection().getFirstElement();				
			}
			if (it != null){
				query.append("\"" + it.getKey() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return query.toString();
			
		}else if (this.type == Type.CM) {
			StringBuilder sb = new StringBuilder(WaypointCmFilter.KEY);
			sb.append(" "); //$NON-NLS-1$
			sb.append(Operator.EQUALS.asSmartValue()); 
			
			Object li = listViewer.getStructuredSelection().getFirstElement();
			if (li != null) {
				sb.append(" \""); //$NON-NLS-1$
				if (li instanceof ConfigurableModel ) {
					sb.append(UuidUtils.uuidToString(((ConfigurableModel)li).getUuid()));
				}else if (li instanceof ListItem){
					sb.append(((ListItem)li).getKey());
				}
				sb.append("\""); //$NON-NLS-1$
			}
			return sb.toString(); 
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.DropItem#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginTop = 0;
		gl.marginBottom = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		
		main.setLayout(gl);
		main.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, true));
		
		Label l = new Label(main, SWT.NONE);
		if (this.type == Type.SOURCE) {
			l.setText(Messages.WaypointSourceFilterDropItem_IncidentSourceDropItem);
		}else if (type == Type.INCIDENTTYPE) {
			l.setText("Incident Type");
		}else {
			l.setText(GeneralContentProvider.GeneralItem.WAYPOINT_CM.guiName + " " + Operator.EQUALS.getGuiValue()); //$NON-NLS-1$
		}
		
		/* -- operator viewer **/
		if (this.type == Type.SOURCE) {
			opViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
			opViewer.setContentProvider(ArrayContentProvider.getInstance());
			opViewer.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					return ((Operator)element).getGuiValue();
				}
			});
			opViewer.setInput(new Operator[]{Operator.STR_EQUALS});
			if (currentOperator == null){
				currentOperator = Operator.STR_EQUALS;
			}
			opViewer.setSelection(new StructuredSelection(currentOperator));
		}
		
		/* -- list viewer **/
		listViewer = new ComboViewer(main, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
		
		FontData fd = (listViewer.getCombo().getFont().getFontData()[0]);
		fd.setHeight(fd.getHeight() - 1);
		smallerFont = new Font(listViewer.getControl().getDisplay(), fd);
		listViewer.getCombo().setFont(smallerFont);
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		listViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof IWaypointSource){
					return ((IWaypointSource)element).getName(null);
				}else if (element instanceof ConfigurableModel) {
					return ((ConfigurableModel)element).getName();
				}else if (element instanceof ListItem) {
					return ((ListItem)element).getName();
				}else if (element instanceof QueryIncidentType qi) {
					return qi.getName();
				}
				return super.getText(element);
			}
		});
		
		if (this.type == Type.SOURCE) {
			List<IWaypointSource> srcs = new ArrayList<IWaypointSource>();
			srcs.addAll(WaypointSourceEngine.INSTANCE.getSupportedSources());
			Collections.sort(srcs, new Comparator<IWaypointSource>() {
				@Override
				public int compare(IWaypointSource o1, IWaypointSource o2) {
					return o1.getName(null).compareTo(o2.getName(null));
				}
			});
			listViewer.setInput(srcs);
		}else if (type == Type.INCIDENTTYPE) {

			listViewer.setInput(new String[] {DialogConstants.LOADING_TEXT});
			Job loading = new Job("loading types") { //$NON-NLS-1$

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					List<QueryIncidentType> allTypes;
					try(Session session = HibernateManager.openSession()){
						allTypes = IncidentTypeProviderManager.INSTANCE.getTypes(session, SmartDB.getConservationAreaConfiguration().getConservationAreas());
					}
					List<QueryIncidentType> fallTypes=allTypes;
					Display.getDefault().asyncExec(()->{
						if (listViewer.getControl().isDisposed()) return;
						listViewer.setInput(fallTypes);
						
						if (currentSelection != null) {
							listViewer.setSelection(new StructuredSelection(currentSelection));
						}
						getTargetPanel().redraw();
					});
					return Status.OK_STATUS;
				}				
			};
			loading.schedule();
			
		}else {
			try(Session session = HibernateManager.openSession()){
				List<ConfigurableModel> models = QueryFactory.buildQuery(session, ConfigurableModel.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				models.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
				List<Object> items = new ArrayList<>();
				items.add(new ListItem(null, 
						SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(IFilter.NULL_OP, Locale.getDefault()),
						IFilter.NULL_OP));
				for (ConfigurableModel cm : models) {
					items.add(cm);
				}
				listViewer.setInput(items);
			}
		}
		
		
		
		if (currentSelection != null){
			listViewer.setSelection(new StructuredSelection(currentSelection));
		}
		
		/* -- events --*/
		listViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				Object newSelection = ((IStructuredSelection)listViewer.getSelection()).getFirstElement();
				if (! (currentSelection != null && currentSelection.equals(newSelection))){
					queryChanged();	
				}			
				currentSelection = newSelection;
			}
		});
		if (opViewer != null) {
			opViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					Operator newSelection = (Operator) ((IStructuredSelection)opViewer.getSelection()).getFirstElement();
					if (! (currentOperator != null && currentOperator.equals(newSelection))){
						queryChanged();	
					}			
					currentOperator = newSelection;
				}
			});
		}
		
		initDrag(main);
		initDrag(l);
	}
}
