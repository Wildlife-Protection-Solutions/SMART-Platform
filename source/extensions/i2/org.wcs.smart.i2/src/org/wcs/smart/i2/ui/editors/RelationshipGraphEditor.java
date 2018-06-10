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
package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.diagram.RelationshipGraphComposite;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.util.E3Utils;

/**
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipGraphEditor extends EditorPart {

	public static final String ID = "org.wcs.smart.i2.editor.graph"; //$NON-NLS-1$

	private List<EventHandler> handlers = null;
	private IEclipseContext parentContext;
	private RelationshipGraphComposite graphComposite;

	private Job graphUpdateJob = new Job("Updating graph input") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (WorkingSetManager.INSTANCE.isSet()) {
				try(Session s = HibernateManager.openSession()) {
					IntelWorkingSet workingset = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
					final List<IntelEntity> activeEntries = workingset.getEntities().stream().filter(e -> e.getIsVisible()).map(e -> e.getEntity()).collect(Collectors.toList());
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							graphComposite.setInput(activeEntries.toArray(new IntelEntity[] {}));
						}
					});
				}
			}
			return Status.OK_STATUS;
		}};

		@Override
		public void createPartControl(Composite parent) {
			handlers = new ArrayList<EventHandler>();

			parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
			MPart part = parentContext.get(MPart.class);
			//disable close button on map editor
			part.setCloseable(false);
			part.getTags().add(E3Utils.DO_NOT_CLOSE_TAG);

			//configure tags so editors show in both perspectives
			if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
			if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);

			GridLayout layout = new GridLayout(1, false);
			layout.marginBottom = 0;
			layout.marginHeight = 0;
			layout.marginLeft = 0;
			layout.marginRight = 0;
			layout.marginTop = 0;
			layout.marginWidth = 0;
			parent.setLayout(layout);
			parent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));

			graphComposite = new RelationshipGraphComposite(parent, new FormToolkit(Display.getCurrent()));
			
			if (WorkingSetManager.INSTANCE.isSet()) {
				try(Session s = HibernateManager.openSession()) {
					IntelWorkingSet workingset = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
					final List<IntelEntity> activeEntries = workingset.getEntities().stream().filter(e -> e.getIsVisible()).map(e -> e.getEntity()).collect(Collectors.toList());
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							graphComposite.setInput(activeEntries.toArray(new IntelEntity[] {}));
						}
					});
				}
			}

			//part.get
			EventHandler handler = new EventHandler() {
				@Override
				public void handleEvent(Event event) {
					graphUpdateJob.cancel();
					graphUpdateJob.schedule();
				}
			};
			parentContext.get(IEventBroker.class).subscribe(IntelEvents.ACTIVE_WS_SET, handler);
			handlers.add(handler);

			handler = new EventHandler() {
				@Override
				public void handleEvent(Event event) {
					IntelWorkingSet set = (IntelWorkingSet) event.getProperty(IEventBroker.DATA);
					if (set != null && set.getUuid().equals(WorkingSetManager.INSTANCE.getActiveWorkingSet())){
						graphUpdateJob.cancel();
						graphUpdateJob.schedule();
					}
				}
			};
			parentContext.get(IEventBroker.class).subscribe(IntelEvents.WS_MODIFIED, handler);
			handlers.add(handler);

			handler = new EventHandler() {
				@Override
				public void handleEvent(Event event) {
					graphUpdateJob.cancel();
					graphUpdateJob.schedule();
				}
			};
			parentContext.get(IEventBroker.class).subscribe(IntelEvents.ENTITY_MODIFIED, handler);
			handlers.add(handler);

			handler = new EventHandler() {
				@Override
				public void handleEvent(Event event) {
					graphUpdateJob.cancel();
					graphUpdateJob.schedule();
				}
			};
			parentContext.get(IEventBroker.class).subscribe(IntelEvents.ACTIVE_WS_LAYER_VISIBILITY, handler);
			handlers.add(handler);

		}

		@Override
		public void dispose() {
			super.dispose();
			IEventBroker events = parentContext.get(IEventBroker.class);
			if (handlers != null) handlers.forEach(h -> events.unsubscribe(h));
		}

		@Override
		public void setFocus() {
			graphComposite.setFocus();
		}

		@Override
		public void init(IEditorSite site, IEditorInput input) throws PartInitException {
			setSite(site);
			setInput(input);
		}

		@Override
		public void doSave(IProgressMonitor monitor) {
			//nothing
		}

		@Override
		public void doSaveAs() {
			//nothing
		}

		@Override
		public boolean isDirty() {
			return false;
		}

		@Override
		public boolean isSaveAsAllowed() {
			return false;
		}

		public static final IEditorInput GRAPHINPUT = new IEditorInput() {

			@Override
			public <T> T getAdapter(Class<T> adapter) {
				return null;
			}

			@Override
			public String getToolTipText() {
				return null;
			}

			@Override
			public IPersistableElement getPersistable() {
				return null;
			}

			@Override
			public String getName() {
				return "Relationships";
			}

			@Override
			public ImageDescriptor getImageDescriptor() {
				return null;
			}

			@Override
			public boolean exists() {
				return false;
			}
		}; 

}
