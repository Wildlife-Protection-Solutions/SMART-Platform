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
package org.wcs.smart.i2.ui.views;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.EntitySearchJob;

public class EntitySearchView {

	public static final String ID = "org.wcs.smart.i2.view.entitysearch";
	@Inject
	private EPartService partService;

	private EntityListComposite entityList;
	private FormToolkit toolkit;
	
	private EntitySearchJob searchJob = new EntitySearchJob() {
		
		@Override
		public void onLoaded(List<IntelEntity> entities) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					entityList.setEntities(entities);
				}
			});
			
		}
		
		@Override
		public void onError(Exception ex) {
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
			entityList.setEntities(new ArrayList<IntelEntity>());
		}
	};
	
	
	
	public EntitySearchView() {
		super();
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		
		Button btnRefresh = new Button(parent, SWT.NONE);
		btnRefresh.setText("REFRESH");
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				searchJob.schedule();
			}
			
		});
		
		toolkit = new FormToolkit(parent.getDisplay());
		entityList = new EntityListComposite(parent, toolkit);
		entityList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		searchJob.schedule();
	}

	// @Optional
	// @Inject
	// private void
	// dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object
	// data){
	// }

	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
		toolkit.dispose();
	}
	
	public static class EntitySearchViewWrapper extends DIViewPart<EntitySearchView>{
		public EntitySearchViewWrapper() {
			super(EntitySearchView.class);
		}
	}

}