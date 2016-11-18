package org.wcs.smart.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class CheckBoxDropDown extends Composite implements Listener {

	private Text txtInfo;
	private Button btnDown;
	
	private Color borderColor;
	private Color mouseOverColor;
	private Color defaultBgColor;
	
	private boolean mouseOver = false;
	
	private Shell popup;
	private CheckboxTableViewer table;
	
	private ILabelProvider labelProvider;
	private IContentProvider contentProvider;
	private Object input;
	
	private List<ISelectionChangedListener> listeners;
	
	public CheckBoxDropDown(Composite parent) {
		super(parent, SWT.NONE);
		borderColor = new Color(getShell().getDisplay(),0, 120,215);
		mouseOverColor = new Color(getShell().getDisplay(),229,241,251);
		
		createControl();
		listeners = new ArrayList<>();
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener){
		listeners.add(listener);
	}
	
	private void fireChangeListener(){
		SelectionChangedEvent event = new SelectionChangedEvent(table, table.getSelection());
		listeners.forEach(l -> l.selectionChanged(event));
	}
	@Override
	public void dispose(){
		super.dispose();
		if (borderColor != null) borderColor.dispose();
		if (popup != null) popup.dispose();
		if (labelProvider != null) labelProvider.dispose();
	}
	
	private void createControl(){
		setLayout(new GridLayout(2, false));
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)getLayout()).marginWidth = 3;
		((GridLayout)getLayout()).marginHeight = 3;
	
	
		txtInfo = new Text(this, SWT.NONE);
		txtInfo.setEditable(false);
		txtInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtInfo.setCursor(getShell().getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		txtInfo.addListener(SWT.MouseDown, event->dropDown(true));
		
		defaultBgColor = txtInfo.getBackground();
		
		btnDown = new Button(this, SWT.DOWN | SWT.ARROW | SWT.FLAT);
		btnDown.addListener(SWT.MouseDown, event->dropDown(true));		
	
		addListener(SWT.Paint, this);
		addListener(SWT.MouseEnter, this);
		addListener(SWT.MouseExit, this);
	
		txtInfo.addListener(SWT.Paint, this);
		txtInfo.addListener(SWT.MouseEnter, this);
		txtInfo.addListener(SWT.MouseExit, this);
		
		btnDown.addListener(SWT.Paint, this);
		btnDown.addListener(SWT.MouseEnter, this);
		btnDown.addListener(SWT.MouseExit, this);
	
	}
	
	@Override
	public void setEnabled(boolean enabled){
		super.setEnabled(enabled);
		txtInfo.setEnabled(enabled);
		btnDown.setEnabled(enabled);
	}
	private void mouseIn(){
		colorComponent(mouseOverColor);
		mouseOver = true;
		
	}
	private void mouseOut(){
		colorComponent(defaultBgColor);
		mouseOver = false;
	}
	private void colorComponent(Color c){
		txtInfo.setBackground(c);
		btnDown.setBackground(c);
		setBackground(c);
		
	}

	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.Paint && event.widget == this){
			if (mouseOver){
				event.gc.setForeground(borderColor);
			}else{
				event.gc.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
			}
			event.gc.drawRectangle(0, 0, getBounds().width - 1, getBounds().height - 1);
		}else if (event.type == SWT.MouseEnter){
			mouseIn();
		}else if (event.type == SWT.MouseExit){
			mouseOut();
		}	
	}
	
	
	public void setLabelProvider(ILabelProvider provider){
		labelProvider = provider;
		if (table != null && !table.getTable().isDisposed()) table.setLabelProvider(provider);
	}
	public void setContentProvider(IContentProvider provider){
		contentProvider = provider;
		if (table != null && !table.getTable().isDisposed()) table.setContentProvider(provider);
	}
	public void setInput(Object input){
		this.input = input;
		if (table != null && !table.getTable().isDisposed()) table.setInput(input);
	}
	
	public Collection<?> getCheckObjects(){
		if (txtInfo.getData() == null) return Collections.emptyList();
		return (Collection<?>) txtInfo.getData();
	}
	
	public void setValue(Collection<?> object){
		txtInfo.setData(object);
		
		StringBuilder sb = new StringBuilder();
		List<Object> objects = new ArrayList<>();
		object.forEach(a -> objects.add(a));
		objects.sort((a,b) ->
		Collator.getInstance().compare(labelProvider.getText(a), labelProvider.getText(b)));
		for (Object o : objects){
			sb.append(labelProvider.getText(o));
			sb.append(", "); //$NON-NLS-1$
		}
		if (sb.length() > 0){
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
		}
		txtInfo.setText(sb.toString());
	}
	
	private void createPopup(){
		// create shell and table
		popup = new Shell(getShell(),  SWT.NO_TRIM | SWT.ON_TOP);
		popup.setLayout(new GridLayout());
		((GridLayout)popup.getLayout()).marginWidth = 1;
		((GridLayout)popup.getLayout()).marginHeight = 1;
		
		popup.addListener(SWT.Deactivate, (event)->dropDown(false));
		popup.addListener(SWT.Paint, event->{
			Rectangle bounds = popup.getBounds();
            event.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
            event.gc.setLineWidth(1);
            event.gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
		});
		// create table
		table = CheckboxTableViewer.newCheckList(popup, SWT.V_SCROLL);
		table.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (labelProvider != null) table.setLabelProvider(labelProvider);
		if (contentProvider != null) table.setContentProvider(contentProvider);
		if (input != null) table.setInput(input);
		popup.pack();
		
	}
	
	/**
	 * handle DropDown request
	 * @param drop
	 */
	private void dropDown (boolean drop) {
		
		// if already dropped then return
		if (popup == null && !drop) return;
		if (popup != null && !popup.isVisible() && !drop) return;
		if (popup != null && popup.isVisible() && drop) return;
		
	    // closing the dropDown
	    if (!drop) {
	    	//update value
	    	Object[] selected = table.getCheckedElements();
	    	List<Object> elements = new ArrayList<Object>();
	    	for (Object xx : selected) elements.add(xx);
	    	setValue(elements);
	    	fireChangeListener();
	    	
	        popup.setVisible (false);
	        if (!isDisposed() && isFocusControl()) {
	            txtInfo.setFocus();
	        }
	        return;
	    }
	    
	    // if not visible then return
	    if (!isVisible()) return;
	
	    // create a new popup if needed.
	    if (popup != null && getShell() != popup.getParent ()) {
	        popup.dispose();
	        popup = null;
	        
	    }
	    if (popup == null) createPopup();
		
	    Control c = (Composite)txtInfo.getParent();
	    Rectangle l = c.getBounds();
	    Point pnt = c.getParent().toDisplay(new Point(l.x, l.y));    
	    popup.setBounds(pnt.x, pnt.y + l.height , l.width, 0);
	    

		// set the popup visible
		popup.setVisible (true);
		popup.getChildren()[0].setFocus();
		for (int i = 0; i < 150; i +=2){
			popup.setBounds(pnt.x, pnt.y + l.height , l.width, i);	
	    }
	    
		Collection<Object> items = (Collection<Object>) txtInfo.getData();
		if (items != null && !items.isEmpty()){
			table.setCheckedElements(items.toArray(new Object[items.size()]));
		}
	}	
}
