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
package org.wcs.smart.i2.ui.handler;

import java.net.URI;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
public class NewQueryDropDownHandler {

	private static final String SOURCE_ID = "org.wcs.smart.i2.query.source"; //$NON-NLS-1$
	
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
		showMenu(p.x, p.y + cc.getSize().y, cc, mi, context);
	}
	
	public void execute(IEclipseContext context, ToolItem item){
		Composite cc = item.getParent();
		Point p = cc.getParent().toDisplay(cc.getLocation());
		showMenu(p.x, p.y + cc.getSize().y, cc, item, context);
	}
	
	private void processChild(MMenuElement e, Menu menu, IEclipseContext context) {
		if (e instanceof MHandledMenuItem) {
			MenuItem mi = new MenuItem(menu, SWT.PUSH);
			try {
				if (e.getIconURI() != null) {
					ImageDescriptor id = ImageDescriptor.createFromURL(URI.create(e.getIconURI()).toURL());
					Image img = id.createImage();
					mi.addListener(SWT.Dispose, evt->img.dispose());
					mi.setImage(  img  );
				}
				mi.addListener(SWT.Selection, evt->{
					
					EHandlerService ehandler = context.get(EHandlerService.class);
					ECommandService  ecmd = context.get(ECommandService .class);
					MCommand cmd = ((MHandledMenuItem)e).getCommand();

					HashMap<String,Object> params = new HashMap<>();
					for (MParameter mp : ((MHandledMenuItem)e).getParameters()) {
						params.put(mp.getName(), mp.getValue());
					}
					
					ParameterizedCommand pcmd = ecmd.createCommand(cmd.getElementId(), params);
					ehandler.executeHandler(pcmd);
					
					
				});
			}catch (Exception ex) {
				ex.printStackTrace();
			}
			mi.setText(e.getLabel());
			mi.setToolTipText(e.getTooltip());
		}else if (e instanceof MMenu) {
			MenuItem mi = new MenuItem(menu, SWT.SEPARATOR);
			if (e.getLabel() != null) mi.setText(e.getLabel());
			for (MMenuElement kid : ((MMenu) e).getChildren()) {
				processChild(kid, menu, context);
			}
		}else if (e instanceof MMenuSeparator) {
//			MenuItem mi = new MenuItem(menu, SWT.SEPARATOR);
//			if (e.getLabel() != null) mi.setText(e.getLabel());
		}
	}

	
	private void showMenu(int x, int y, Composite parent, ToolItem item, IEclipseContext context) {
		Menu mnu = new Menu(parent);
		EModelService mService = context.get(EModelService.class);
		MMenu createMenu = (MMenu) mService.find("org.wcs.smart.i2.query.menu.new", context.get(MWindow.class).getMainMenu()); //$NON-NLS-1$
		for (MMenuElement e : createMenu.getChildren()) {
			processChild(e, mnu, context);
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
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}
	}
	
}
