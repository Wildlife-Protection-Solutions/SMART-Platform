/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.record;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.ui.ObservationTreeViewer;
import org.wcs.smart.i2.ui.SmartShellDialog;

/**
 * Shell dialog for displaying details of an intel location.
 * 
 * @author Emily
 *
 */
public class LocationDetailsShell extends SmartShellDialog{
	
	private IntelLocation location;
	private List<IntelEntityLocation> entityLocationLinks;
	
	private Color bgColor = null;
	private Font boldFont = null;
	
	private boolean blnMouseDown;
	private int xPos, yPos;
	private Label lblId;
	
//	private TreeViewer observationTree;
	private ScrolledComposite sc;
	private Composite main;
	
	@Override
	public void open(Point position){
		Point sizea = shell.getSize();
		createContents(shell);
		shell.setSize(shell.getSize().x, 400);
		Point sizeb = shell.getSize();
		int y = sizea.y - sizeb.y + position.y;
		int x = sizea.x - sizeb.x + position.x;
		shell.setLocation(x, y);
		shell.open();
		
		sc.layout(true, true);
		sc.setMinSize(main.computeSize(main.getBounds().width, SWT.DEFAULT));
	}
		
	public LocationDetailsShell(Shell ownerShell, IntelLocation location, List<IntelEntityLocation> entityLocationLinks){
		super(ownerShell, SWT.RESIZE);
		this.location = location;
		this.entityLocationLinks = new ArrayList<IntelEntityLocation>();
		for (IntelEntityLocation l : entityLocationLinks){
			if (l.getLocation().equals(location)){
				this.entityLocationLinks.add(l);
			}
		}
	}
	
	public void createContents(Composite parent){
		Composite owner = new Composite(parent, SWT.NONE);
		owner.setLayout(new GridLayout());
		owner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lblId = new Label(owner, SWT.NONE);
		lblId.setText(location.getId());
		FontData fd = lblId.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.height = fd.height + 1;
		boldFont = new Font(owner.getDisplay(), fd);
		lblId.setFont(boldFont);
		lblId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblId.addListener(SWT.MouseDown, this);
		lblId.addListener(SWT.MouseUp, this);
		lblId.addListener(SWT.MouseMove, this);
		lblId.addListener(SWT.MouseEnter, this);
		lblId.addListener(SWT.MouseExit, this);
		
		Label lblDate = new Label(owner, SWT.NONE);
		lblDate.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(location.getDateTime()));
		
		
		sc = new ScrolledComposite(owner, SWT.V_SCROLL | SWT.H_SCROLL);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		main = new Composite(sc, SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		sc.setContent(main);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		
		
		if (location.getComment() != null && !location.getComment().isEmpty()){
			Label lblComment = new Label(main, SWT.WRAP);
			lblComment.setText(location.getComment());
			lblComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)lblComment.getLayoutData()).widthHint = 100;
		}
		
		Composite entities = new Composite(main, SWT.NONE);
		entities.setLayout(new GridLayout(2, false));
		((GridLayout)entities.getLayout()).marginWidth = 0;
		((GridLayout)entities.getLayout()).marginHeight = 0;
		entities.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Label lblEntities = new Label(entities, SWT.NONE);
		lblEntities.setText(Messages.LocationDetailsShell_EntitiesLabel);
		lblEntities.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		if (!entityLocationLinks.isEmpty()){	
			ListViewer lstViewer = new ListViewer(entities, SWT.NONE);
			lstViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			lstViewer.setContentProvider(ArrayContentProvider.getInstance());
			lstViewer.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof IntelEntityLocation){
						return ((IntelEntityLocation) element).getEntity().getIdAttributeAsText();
					}
					return super.getText(element);
				}
			});
			
			lstViewer.setInput(entityLocationLinks);
		}else{
			Label l = new Label(entities, SWT.NONE);
			l.setText(Messages.LocationDetailsShell_EntityNoneLabel);
		}
		
		
		Composite obs = new Composite(main, SWT.NONE);
		obs.setLayout(new GridLayout(2, false));
		((GridLayout)obs.getLayout()).marginWidth = 0;
		((GridLayout)obs.getLayout()).marginHeight = 0;
		obs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Label lblObservations = new Label(obs, SWT.NONE);
		lblObservations.setText(Messages.LocationDetailsShell_ObservationsLabel);
		lblObservations.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		if (location.getObservations() != null && !location.getObservations().isEmpty()){
			ObservationTreeViewer observationTree = new ObservationTreeViewer(obs, SWT.FULL_SELECTION);
			observationTree.getViewer().getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			observationTree.setInput(location);
			observationTree.expandToLevel(2);
		}else{
			Label l = new Label(obs, SWT.NONE);
			l.setText(Messages.LocationDetailsShell_ObsNoneLabel);
		}
		sc.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		//configure background color
		bgColor = new Color(parent.getDisplay(), 255, 255, 225);
		
		List<Control> items = new ArrayList<Control>();
		items.add(owner);
		while(!items.isEmpty()){
			Control c = items.remove(0);
			c.setBackground(bgColor);
			if (c instanceof Composite){
				for (Control kid : ((Composite) c).getChildren()){
					items.add(kid);
				}
			}
		}	
	}

	
	public IntelLocation getLocationRecord(){
		return location;
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
		
		if (event.type == SWT.Dispose){
			if (bgColor != null){
				bgColor.dispose();
				bgColor = null;
			}
			if (boldFont != null){
				boldFont.dispose();
				boldFont = null;
			}
			return;
		}else if (event.type == SWT.Deactivate){
			close();
			return;
		}else if (event.type == SWT.MouseDown){
			if (event.widget != lblId) return;
			blnMouseDown = true;
			xPos = event.x;
			yPos = event.y;
		} else if (event.type == SWT.MouseUp) {
			if (event.widget != lblId) return;
			blnMouseDown = false;
		} else if (event.type == SWT.MouseMove) {
			if (event.widget != lblId) return;
			if (blnMouseDown) {
				shell.setLocation(shell.getLocation().x + (event.x - xPos),
						shell.getLocation().y + (event.y - yPos));
			}
		} else if (event.type == SWT.MouseEnter) {
			if (event.widget != lblId) return;
			shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_SIZEALL));
		} else if (event.type == SWT.MouseExit) {
			if (event.widget != lblId) return;
			shell.setCursor(null);
		}
	}
	
}
