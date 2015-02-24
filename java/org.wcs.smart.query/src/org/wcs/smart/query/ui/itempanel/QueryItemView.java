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
package org.wcs.smart.query.ui.itempanel;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISourceProviderListener;
import org.wcs.smart.query.event.QueryDataModelModifiedListener;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.ui.QuerySourceProvider;
import org.wcs.smart.query.ui.definition.DefinitionPanelManager;

/**
 * A view that display the query filter options.
 * <p>
 * The Source Provider is updated when a query filter
 * option is chosen.
 * </p>
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryItemView implements ISourceProviderListener {

	public static final String ID ="org.wcs.smart.query.parts.filter"; //$NON-NLS-1$

	private Composite main;
	private Label defaultFilter;
	
	private QueryDataModelModifiedListener dmListener;
	
	@Inject private QuerySourceProvider srcProvider;
	@Inject private MPart part;
	
	public QueryItemView() {
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		Composite outer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 2;
		layout.verticalSpacing = 2;
		layout.marginWidth = 3;
		layout.marginHeight = 3;		
		outer.setLayout(layout);
		outer.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		main = new Composite(outer, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		StackLayout stack = new StackLayout();
		stack.marginHeight = stack.marginWidth = 0;
		main.setLayout(stack);
		
		defaultFilter = new Label(main, SWT.NONE);
		defaultFilter.setText(Messages.QueryFilterView_ErrorNoFilterOptions1);

		ContextInjectionFactory.make(QueryDataModelModifiedListener.class, part.getContext());
		srcProvider.addSourceProviderListener(this);
	}

	@Override
	public void sourceChanged(int sourcePriority, String sourceName,
			Object sourceValue) {
		if (sourceName.equals(QuerySourceProvider.DEFINITION_PANEL_ID)){
			
			IQueryItemPanel panel = part.getContext()
					.get(DefinitionPanelManager.class)
					.getQueryItemPanel((String)sourceValue, 
							(IQueryType)srcProvider.getCurrentState().get(QuerySourceProvider.QUERY_TYPE));
			
			if (panel != null){
				Composite pnlComp = panel.getComposite(main);
				pnlComp.setData(panel);
				((StackLayout)main.getLayout()).topControl = pnlComp;
			}else{
				((StackLayout)main.getLayout()).topControl = defaultFilter;
			}
			main.layout();
			
			part.getContext().get(EPartService.class).bringToTop(part);
		}
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void sourceChanged(int sourcePriority, Map sourceValuesByName) {				
	}
	
	/**
	 * Refreshes all item panels that have been loaded into the view
	 */
	public void refresh(){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				for (Control c : main.getChildren()){
					if (c.getData() != null){
						if (c.getData() instanceof IQueryItemPanel){
							((IQueryItemPanel)c.getData()).refreshPanel();
						}
					}
				}
			}	
		});
		
	}
	
	@PreDestroy
	public void dispose(){
		if (dmListener != null){
			dmListener.dispose();
		}
		srcProvider.removeSourceProviderListener(this);
	}
	
	@Focus
	public void setFocus() {
		main.setFocus();
	}


	public static class QueryItemViewWrapper extends DIViewPart<QueryItemView>{
		public QueryItemViewWrapper(){
			super(QueryItemView.class);
		}
	}
}
