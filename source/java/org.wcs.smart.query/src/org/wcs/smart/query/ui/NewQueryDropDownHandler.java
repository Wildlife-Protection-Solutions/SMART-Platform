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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.commands.MCommand;
import org.eclipse.e4.ui.model.application.commands.MParameter;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledMenuItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuSeparator;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

/**
 * Handler specific to the create new query toolbar button. This drop
 * down shows a menu that is a flattened version of the new query menu from
 * the main menu bar.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class NewQueryDropDownHandler {

	private static final String SOURCE_ID = "org.wcs.smart.query.source"; //$NON-NLS-1$
	private static final String MENU_TYPE = "org.wcs.smart.query.menutype"; //$NON-NLS-1$

	//valid menu types for the MENU_TYPE parameter
	public static final String DROP_DOWN = "dropdown"; //$NON-NLS-1$
	public static final String SUBMENU = "submenu"; //$NON-NLS-1$
	
	@Execute
	public void execute(IEclipseContext context){
		EModelService m = context.get(EModelService.class);
		ToolItem mi = null;
		MUIElement miItem = m.find((String)context.get(SOURCE_ID), context.get(MWindow.class)); 
		if (miItem == null) {
			//search toolbars
			MToolBar menus = context.get(MPart.class).getToolbar();
			for (MToolBarElement i : menus.getChildren()) {
				if (i.getElementId().equals((String)context.get(SOURCE_ID))) {
					miItem = i;
					break;
				}
			}
		}
		if (miItem != null && miItem.getWidget() instanceof ToolItem) {
			mi = (ToolItem) miItem.getWidget();
		}
		
		Composite cc = mi.getParent();
		Point p = cc.getParent().toDisplay(cc.getLocation());
		String type = (String) context.get(MENU_TYPE);
		if (type.equals(DROP_DOWN)) {
			showDropDownMenu(p.x, p.y, cc, mi, context);
		}else if (type.equals(SUBMENU)) {
			showMenu(p.x, p.y , cc, mi, context);
		}
	}

	
	private void processChild(MMenuElement e, Menu menu, IEclipseContext context, Runnable onSelection, boolean dispose) {
		if (e instanceof MHandledMenuItem) {
			MenuItem mi = new MenuItem(menu, SWT.PUSH);
			try {
				ImageDescriptor id = ImageDescriptor.createFromURL(new URL(e.getIconURI()));
				Image img = id.createImage();
				mi.addListener(SWT.Dispose, evt->img.dispose());
				mi.setImage(  img  );
				
				mi.addListener(SWT.Selection, evt->{
					EHandlerService ehandler = context.get(EHandlerService.class);
					ECommandService  ecmd = context.get(ECommandService.class);
					MCommand cmd = ((MHandledMenuItem)e).getCommand();

					HashMap<String,Object> params = new HashMap<>();
					for (MParameter mp : ((MHandledMenuItem)e).getParameters()) {
						params.put(mp.getName(), mp.getValue());
					}
					
					//specifically displose this shell
					//otherwise it gets used in the ccaa merge dialog progress dialog
					//but ends up getting disposed and throws an error
					if (dispose) menu.getShell().dispose();
					
					ParameterizedCommand pcmd = ecmd.createCommand(cmd.getElementId(), params);
					ehandler.executeHandler(pcmd, context);
					onSelection.run();
				});
			}catch (Exception ex) {
				ex.printStackTrace();
			}
			mi.setText(e.getLabel());
			mi.setToolTipText(e.getTooltip());
		}else if (e instanceof MMenu ) {
			if (menu.getItemCount() > 0) {
				new MenuItem(menu, SWT.SEPARATOR);
			}
			for (MMenuElement kid : ((MMenu) e).getChildren()) {
				processChild(kid, menu, context, onSelection, dispose);
			}
		}else if (e instanceof MMenuSeparator) {
//			MenuItem mi = new MenuItem(menu, SWT.SEPARATOR);
//			if (e.getLabel() != null) mi.setText(e.getLabel());
		}
	}

	/**
	 * Shows a new shell with a query type group tool bar, each group 
	 * button has a menu associated with it for the query types.
	 * 
	 * @param x
	 * @param y
	 * @param parent
	 * @param item
	 * @param context
	 */
	private void showMenu(int x, int y, Composite parent, ToolItem item, IEclipseContext context) {

		Shell outer = new Shell(parent.getDisplay(), SWT.NO_TRIM | SWT.ON_TOP);
		outer.addListener(SWT.Deactivate, event -> outer.close());
		IEclipseContext fcontext = context.createChild();
		fcontext.set(Shell.class, outer);
		outer.setData("org.eclipse.e4.ui.shellContext", fcontext); //$NON-NLS-1$
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		outer.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		Composite temp = new Composite(outer, SWT.BORDER);
		temp.setLayout(new GridLayout());
		temp.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		((GridLayout)temp.getLayout()).marginWidth = 0;
		((GridLayout)temp.getLayout()).marginHeight = 0;
		
		ToolBar tb = new ToolBar(temp,  SWT.FLAT | SWT.HORIZONTAL );
		tb.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		tb.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		tb.setFocus();

		EModelService mService = context.get(EModelService.class);
		MMenu createMenu = (MMenu) mService.find("org.wcs.smart.menu.query.newquery", context.get(MWindow.class).getMainMenu()); //$NON-NLS-1$
		for (MMenuElement e : createMenu.getChildren()) {
			if (e instanceof MMenu) {
				ToolItem ti = new ToolItem(tb, SWT.RADIO);
				ti.setText(e.getLabel());

				try {
					if (e.getIconURI() != null) {
						ImageDescriptor id = ImageDescriptor.createFromURL(new URL(e.getIconURI()));
						Image img = id.createImage();
						ti.addListener(SWT.Dispose, evt->img.dispose());
						ti.setImage(  img  );
					}
				} catch (MalformedURLException e1) {
				}
				
				
				ti.addListener(SWT.Selection, ev->{
					if(!ti.getSelection()) return;
					Menu mnu = new Menu(tb);
					processChild(e,mnu,fcontext, ()->outer.dispose(), true);
					Point p = tb.getParent().toDisplay(ti.getBounds().x, ti.getBounds().y + tb.getBounds().height);
					mnu.setLocation(p.x,p.y);
					mnu.setVisible(true);
				});
			}
		}

		outer.pack();
		
		x = x - outer.getSize().x/2;
		
		outer.setLocation(x,y);
		outer.setVisible(true);
		outer.setFocus();
		item.setSelection(false);
	}
	
	
	private void showDropDownMenu(int x, int y, Composite parent, ToolItem item, IEclipseContext context) {		
		Menu mnu = new Menu(parent);
		EModelService mService = context.get(EModelService.class);
		MMenu createMenu = (MMenu) mService.find("org.wcs.smart.menu.query.newquery", context.get(MWindow.class).getMainMenu()); //$NON-NLS-1$
		for (MMenuElement e : createMenu.getChildren()) {
			processChild(e, mnu, context, ()->{}, false);
		}
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
				item.setSelection(false);
				
			}
		});

		mnu.setLocation(x,y);
		mnu.setVisible(true);
	}
	
	public static class NewQueryDropDownHandlerWrapper extends AbstractHandler {

		private NewQueryDropDownHandler component;

		public NewQueryDropDownHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(NewQueryDropDownHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(SOURCE_ID, event.getParameter(SOURCE_ID));
			ctx.set(MENU_TYPE, event.getParameter(MENU_TYPE));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}
	}
	
}



