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
package org.wcs.smart.paws.ui.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.AbstractPawsClass;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.model.Query;

/**
 * Composite for listing queries and data model items
 * that are associated with a PAWS Configuration 
 * 
 * @author Emily
 *
 */
public class ClassificationTableComposite extends Composite{


	private List<ClassificationData> uiElements;
	
	private Composite list;
	private FormToolkit toolkit;
	private ScrolledForm form;
	
	private Composite tableHeader;
	
	private static final int cols = 4;
	
	public ClassificationTableComposite(Composite parent) {
		super(parent, SWT.NONE);
		
		uiElements = new ArrayList<>();
		
		toolkit = new FormToolkit(Display.getDefault());
		addListener(SWT.Dispose, e->toolkit.dispose());
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		

		tableHeader = toolkit.createComposite(this);
		tableHeader.setLayout(new GridLayout(cols, false));
		tableHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)tableHeader.getLayout()).marginWidth = 0;
		((GridLayout)tableHeader.getLayout()).marginHeight = 0;
		
		Label l = toolkit.createLabel(tableHeader, Messages.ClassificationTableComposite_ClassificatoinLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		l = toolkit.createLabel(tableHeader, Messages.ClassificationTableComposite_DataSourceOp);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


		l = toolkit.createLabel(tableHeader, Messages.ClassificationTableComposite_DetailsSection);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		ToolBar tb = new ToolBar(tableHeader, SWT.FLAT);
		adapt(tb);
		
		ToolItem addItem = new ToolItem(tb, SWT.RADIO);
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.addListener(SWT.Selection, e->{
			Menu menu = new Menu(tb);
			
			menu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
				}
				
				@Override
				public void menuHidden(MenuEvent e) {
					addItem.setSelection(false);
				}
			});
			MenuItem miDataModel = new MenuItem(menu, SWT.PUSH);
			miDataModel.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON));
			miDataModel.setText(Messages.ClassificationTableComposite_DataModelMnuOp);
			miDataModel.addListener(SWT.Selection, evt->{
				addDataModelItems();
			});
			
			MenuItem miQuery = new MenuItem(menu, SWT.PUSH);
			miQuery.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.QUERY_ICON));
			miQuery.setText(Messages.ClassificationTableComposite_QueryMnuOp);
			miQuery.addListener(SWT.Selection, evt->{
				addQueryItems();
			});
			
			Point p1 = tableHeader.toDisplay(tb.getLocation());
			p1.y = p1.y + tb.getBounds().height;
			menu.setLocation( p1 );
			menu.setVisible(true);
		});
				
		Composite spacer = toolkit.createComposite(tableHeader);
		spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, cols, 1));
		((GridData)spacer.getLayoutData()).heightHint = 3;
		spacer.addListener(SWT.Paint, e->{
			Rectangle r = ((Control)e.widget).getBounds();
			e.gc.setForeground(e.widget.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			e.gc.drawLine(0, 1, r.width, 1);
		});
		
		
		form = toolkit.createScrolledForm(this);
		
		form.getBody().setLayout(new GridLayout());
		((GridLayout)form.getBody().getLayout()).marginWidth = 0;
		((GridLayout)form.getBody().getLayout()).marginHeight = 0;
		
		list = toolkit.createComposite(form.getBody(), SWT.NONE);
		list.setLayout(new GridLayout(cols, false));
		form.setContent(list);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)list.getLayout()).marginWidth = 0;
		((GridLayout)list.getLayout()).marginHeight = 0;
		
		list.addListener(SWT.Resize, e->resizeHeader());
		
		updateList();
	}
	
	protected boolean canAdd(Query query) {
		return !query.getTypeKey().equalsIgnoreCase(CompoundMapQuery.TYPE_KEY);
	}
	
	public void addQuery(Query query) {
		addQuery(query, true);
	}
	
	public void addQuery(Query query, boolean refresh) {
		if (!canAdd(query)) return;
		
		PawsQueryClass qc = new PawsQueryClass();
		qc.setCachedQuery(query);
		qc.setClassification(query.getName().toLowerCase());
		qc.setQueryType(query.getTypeKey());
		qc.setQueryUuid(query.getUuid());
		
		ClassificationData qi = new ClassificationData(qc, query.getName());

		uiElements.add(qi);
		fireListeners();
		if (refresh) updateList();
	}
	

	public void addQueries(List<Query> queries) {
		for (Query query : queries) {
			if (!canAdd(query)) continue;
			
			PawsQueryClass qc = new PawsQueryClass();
			qc.setCachedQuery(query);
			qc.setClassification(query.getName().toLowerCase());
			qc.setQueryType(query.getTypeKey());
			qc.setQueryUuid(query.getUuid());
			
			ClassificationData qi = new ClassificationData(qc, query.getName());
			uiElements.add(qi);
		}
		updateList();
		fireListeners();
	}

	private void removeQuery(ClassificationData query) {
		uiElements.remove(query);
		updateList();
		fireListeners();
	}
	
	public List<AbstractPawsClass> getClassifications(){
		return uiElements.stream().map(e->e.getPawsClass()	).collect(Collectors.toList());
	}
	
	public void initItem(Collection<ClassificationData> items) {
		for (ClassificationData i : items) {
			uiElements.add(i);
		}
		updateList();
	}
	
	private void fireListeners() {
		Event modified = new Event();
		for(Listener l : getListeners(SWT.Selection)) {
			l.handleEvent(modified);
		}
	}
	
	
	private void adapt(Control c) {
		toolkit.adapt(c,false, false);
		if (c instanceof Composite) {
			for (Control kid : ((Composite) c).getChildren()) {
				adapt(kid);
			}
		}
	}
	
	void updateList() {
		for (Control c : list.getChildren()) c.dispose();
		
		for (ClassificationData q : uiElements) {

			//classification
			EditLabel lbl = new EditLabel(list, q);
//			Label lbl = new Label(list, SWT.NONE);
			lbl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			
			//data source
			Label l = toolkit.createLabel(list, q.getDataSource());
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			((GridData)lbl.getLayoutData()).heightHint = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
					
			//details
			l = toolkit.createLabel(list, q.getDetails());
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
			ToolBar btn = new ToolBar(list, SWT.FLAT);
			btn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			adapt(btn);
			
			ToolItem miDelete = new ToolItem(btn, SWT.PUSH);
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addListener(SWT.Selection, e->removeQuery(q));
			
			Composite spacer = toolkit.createComposite(list);
			spacer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, cols, 1));
			((GridData)spacer.getLayoutData()).heightHint = 3;
			spacer.addListener(SWT.Paint, e->{
				Rectangle r = ((Control)e.widget).getBounds();
				e.gc.setForeground(e.widget.getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
				e.gc.drawLine(0, 1, r.width, 1);
			});

		}
		
		FontData fd = getFont().getFontData()[0];
		fd.setStyle(SWT.ITALIC);
		Font newFont = new Font(getDisplay(), fd);
		addListener(SWT.Dispose, e->newFont.dispose());
		
		
		Hyperlink hl = toolkit.createHyperlink(list,Messages.ClassificationTableComposite_DandDmsg, SWT.NONE);
		hl.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, cols, 1));
		((GridData)hl.getLayoutData()).verticalIndent = 5;
		hl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		hl.setFont(newFont);
		hl.addHyperlinkListener(new IHyperlinkListener() {
			
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				addQueryItems();
			}
		});

		Label l = toolkit.createLabel(list,Messages.ClassificationTableComposite_or);
		l.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, cols, 1));
		((GridData)l.getLayoutData()).verticalIndent = 5;
		l.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		l.setFont(newFont);
		
		hl = toolkit.createHyperlink(list,Messages.ClassificationTableComposite_ClickMsg, SWT.NONE);
		hl.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, cols, 1));
		((GridData)hl.getLayoutData()).verticalIndent = 5;
		hl.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		hl.setFont(newFont);
		hl.addHyperlinkListener(new IHyperlinkListener() {
			
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				addDataModelItems();
			}
		});

		form.reflow(true);
		list.layout(true);
		resizeHeader();
	}
	
	private void resizeHeader() {
		if (!uiElements.isEmpty()) {
			for (int i = 0; i < cols; i ++) {
				Rectangle r = list.getChildren()[i].getBounds(); 
				int hint = r.width;
				
				if(hint == 0) return;
				if (form.getVerticalBar().getVisible() && i == 0) {
					hint = hint - form.getVerticalBar().getSize().x;
				}
				((GridData)tableHeader.getChildren()[i].getLayoutData()).widthHint = hint;
			}
			tableHeader.layout(true);
		}
	}
	
	private void addDataModelItems() {
		DataModelDialog d = new DataModelDialog(getShell());
		if (d.open() != Window.OK) return;
		
		for (ClassificationData obj : d.getSelectedItems()) {
			uiElements.add(obj);
		}
		updateList();
		fireListeners();
	}
	
	private void addQueryItems() {
		QueryDialog d = new QueryDialog(getShell());
		if (d.open() != Window.OK) return;
		
		for (ClassificationData obj : d.getSelectedItems()) {
			uiElements.add(obj);
		}
		updateList();
		fireListeners();
	}
		
	private class EditLabel extends Composite{
		private Text txtEdit;
		private Label l;
		private ClassificationData q;
		
		public EditLabel(Composite parent, ClassificationData q) {
			super(parent, SWT.NONE);
			setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			
			this.q = q;
				
			setLayout(new StackLayout());
			((StackLayout)getLayout()).marginWidth = 0;
			((StackLayout)getLayout()).marginHeight = 0;
			
			l = new Label(this, SWT.NONE);
			l.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			l.setText(q.getClassification());
			l.setToolTipText(q.getClassification());
			
			
			txtEdit = new Text(this, SWT.NONE);
			txtEdit.setVisible(false);
			txtEdit.setBackground(getDisplay().getSystemColor(SWT.COLOR_YELLOW));

			txtEdit.setVisible(false);
			
			((StackLayout)getLayout()).topControl = l;
			layout();

			l.addMouseListener(new MouseListener() {
				
				@Override
				public void mouseUp(MouseEvent e) {
				}
				
				@Override
				public void mouseDown(MouseEvent e) {
				}
				
				@Override
				public void mouseDoubleClick(MouseEvent e) {
					editItem(true);
					
				}
			});
			txtEdit.addListener(SWT.FocusOut, e->{
				boolean diff = !l.getText().equals(txtEdit.getText());
				l.setText(txtEdit.getText());
				editItem(false);
				if (diff) fireListeners();
			});
			txtEdit.addListener(SWT.KeyDown, e->{
				if (e.character == SWT.ESC) {
					txtEdit.setText(l.getText());
					editItem(false);	
				}else if (e.character == SWT.CR || e.character == SWT.LF) {
					boolean diff = !l.getText().equals(txtEdit.getText());
					l.setText(txtEdit.getText());
					editItem(false);
					if (diff) fireListeners();
				}
			});
		}
		
		
		public void setToolTipText(String text){
			l.setToolTipText(text);
		}
			
		private void editItem(boolean edit) {
			if (edit) {
				txtEdit.setText(l.getText());
				((StackLayout)getLayout()).topControl = txtEdit;
				layout();
				
				txtEdit.selectAll();
				txtEdit.setFocus();
			}else {
				((StackLayout)getLayout()).topControl = l;
				q.getPawsClass().setClassification(l.getText());
			}
			layout();
		}
	}
}
