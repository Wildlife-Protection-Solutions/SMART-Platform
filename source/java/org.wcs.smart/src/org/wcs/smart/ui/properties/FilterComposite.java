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
package org.wcs.smart.ui.properties;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.WorkbenchJob;
import org.wcs.smart.internal.Messages;

/**
 * A filter text box with clear option.
 * 
 * @author Emily
 *
 */
public class FilterComposite extends Composite {
	private Text txtFilter;
	private Control clearButtonContro;

	private List<ChangeListener> listeners = new ArrayList<ChangeListener>();
	
	private String initialText = Messages.FilterComposite_Default_SearchText;
	private String patternFilter = null;
	
	private Job filterJob = null;
	
	/**
	 * Image descriptor for enabled clear button.
	 */
	private static final String CLEAR_ICON = "org.wsc.smart.CLEAR_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor for disabled clear button.
	 */
	private static final String DISABLED_CLEAR_ICON= "org.wsc.smart.DCLEAR_ICON"; //$NON-NLS-1$
	
	
	/**
	 * Get image descriptors for the clear button.
	 */
	static {
		ImageDescriptor descriptor = AbstractUIPlugin
				.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,
						"$nl$/icons/full/etool16/clear_co.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CLEAR_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				PlatformUI.PLUGIN_ID, "$nl$/icons/full/dtool16/clear_co.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(DISABLED_CLEAR_ICON, descriptor);
		}
	}
	
	/**
	 * Create a new filter composite
	 * @param parent 
	 * @param style
	 */
	public FilterComposite(Composite parent, int style) {
		super(parent, style | SWT.NONE);
		
		GridLayout layout = new GridLayout(1, false);
		layout.marginBottom = 0;
		layout.marginLeft = 0;
		layout.marginHeight = 0;
		layout.marginRight = 0;
		layout.marginTop = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		
		
		
		if (useNativeSearchField(this)){
			txtFilter = new Text(this, SWT.SINGLE | SWT.SEARCH | SWT.ICON_CANCEL);
			txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		}else{
			Composite tmp = new Composite(this, SWT.BORDER);
			GridLayout layout1 = new GridLayout(2, false);
			layout1.marginBottom = 0;
			layout1.marginLeft = 0;
			layout1.marginHeight = 0;
			layout1.marginRight = 0;
			layout1.marginTop = 0;
			layout1.marginWidth = 0;
			tmp.setLayout(layout1);
			tmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						
			tmp.setBackground(parent.getDisplay().getSystemColor(
					SWT.COLOR_LIST_BACKGROUND));
			
			txtFilter = new Text(tmp, SWT.SINGLE);
			txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			createClearTextNew(tmp);
		}
		
		super.setBackground(txtFilter.getBackground());
		
		txtFilter.addTraverseListener(new TraverseListener() {	
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					Display.getCurrent().beep();
					e.doit = false;
				}
			}
		});
		
		txtFilter.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				if (!txtFilter.isDisposed()){
					if (txtFilter.getText().isEmpty()){
						clearText(true);
					}
				}
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				/*
				 * Running in an asyncExec because the selectAll() does not appear to work when
				 * using mouse to give focus to text.
				 */
				Display display= txtFilter.getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						if (!txtFilter.isDisposed()) {
							if (txtFilter.getText().equals(initialText)){
								txtFilter.setText(""); //$NON-NLS-1$
							}
						}
					}
				});
				return;			
			}
		});
		
		txtFilter.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				filterJob.cancel();
				filterJob.schedule();
				txtFilter.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_BLACK));
			}
		});
	
		
		filterJob = doCreateRefreshJob();
		clearText(true);
	}

	
	/**
	 * Clears the text in the text box
	 * @param init if the text box should be replaced with original text
	 */
	private void clearText(boolean init){
		if (init){
			txtFilter.setText(initialText);
		}else{
			txtFilter.setText(""); //$NON-NLS-1$
		}
		txtFilter.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY));
		txtFilter.setMessage(initialText);
	}
	
	/**
	 * Initializes the text box with the given text
	 * @param text
	 */
	public void setText(String text){
		txtFilter.setText(text);
	}
	
	/**
	 * Adds a change listener.
	 * <p>
	 * Listeners are fired when the text changes.
	 * </p>
	 * @param listener
	 */
	public void addChangeListener(ChangeListener listener){
		listeners.add(listener);
	} 
	/**
	 * Removes a change listener.
	 * @param listener
	 */
	public void removeChangeListener(ChangeListener listener){
		listeners.remove(listener);
	}
	private void fireChangeListeners(){
		ChangeEvent evt = new ChangeEvent(this);
		for (ChangeListener listener : listeners) {
			listener.stateChanged(evt);
		}
	}
	
	private String getText(){
		return this.txtFilter.getText();
	}

	/**
	 * 
	 * @return the filter pattern entered by the user
	 */
	public String getPatternFilter(){
		return this.patternFilter;
	}
	private void setPattern(String pattern){
		this.patternFilter = pattern;
	}
	
	/**
	 * 
	 * @return a job that updates the ui and fires
	 * all the registered change listeners.
	 */
	private WorkbenchJob doCreateRefreshJob() {

		return new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
			private String lastSearch = null;
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (txtFilter.isDisposed()) {
					return Status.CANCEL_STATUS;
				}

				String text = getText();
				String pattern = null;
				if (text == null || text.equals(initialText) || text.isEmpty()) {
					pattern = null;
				} else if (text != null) {
					pattern = text;
				}
				if ( (pattern == null && lastSearch == null) || (pattern != null && pattern.equals(lastSearch))){
					//search has not changed
					return Status.OK_STATUS;
				}
				lastSearch = pattern;
				setPattern(pattern);
				fireChangeListeners();
				if (text == null || text.equals(initialText) || text.isEmpty()) {
					updateToolbar(false);
				}else{
					updateToolbar(true);
				}
				
				return Status.OK_STATUS;
			}

		};
	}

	/**
	 * Updates the clear icon button
	 * @param visible
	 */
	private void updateToolbar(boolean visible) {
		if (clearButtonContro!= null) {
			clearButtonContro.setVisible(visible);
		}
	}
	
	/**
	 * Creates the clear icon button
	 * @param parent
	 */
	private void createClearTextNew(Composite parent) {
		// only create the button if the text widget doesn't support one
		// natively

		final Image inactiveImage = JFaceResources.getImageRegistry().getDescriptor(DISABLED_CLEAR_ICON).createImage();
		final Image activeImage = JFaceResources.getImageRegistry().getDescriptor(CLEAR_ICON).createImage();
		final Image pressedImage = new Image(getDisplay(), activeImage, SWT.IMAGE_GRAY);

		final Label clearButton = new Label(parent, SWT.NONE);
		clearButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER,
				false, false));
		clearButton.setImage(inactiveImage);
		clearButton.setBackground(parent.getDisplay().getSystemColor(
				SWT.COLOR_LIST_BACKGROUND));
		clearButton.setToolTipText(Messages.FilterComposite_ClearButton_Tooltip);
		clearButton.addMouseListener(new MouseAdapter() {
			private MouseMoveListener fMoveListener;

			public void mouseDown(MouseEvent e) {
				clearButton.setImage(pressedImage);
				fMoveListener = new MouseMoveListener() {
					private boolean fMouseInButton = true;

					public void mouseMove(MouseEvent e) {
						boolean mouseInButton = isMouseInButton(e);
						if (mouseInButton != fMouseInButton) {
							fMouseInButton = mouseInButton;
							clearButton.setImage(mouseInButton ? pressedImage
									: inactiveImage);
						}
					}
				};
				clearButton.addMouseMoveListener(fMoveListener);
			}

			public void mouseUp(MouseEvent e) {
				if (fMoveListener != null) {
					clearButton.removeMouseMoveListener(fMoveListener);
					fMoveListener = null;
					boolean mouseInButton = isMouseInButton(e);
					clearButton.setImage(mouseInButton ? activeImage
							: inactiveImage);
					if (mouseInButton) {
						clearText(false);
						txtFilter.setFocus();
					}
				}
			}

			private boolean isMouseInButton(MouseEvent e) {
				Point buttonSize = clearButton.getSize();
				return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y
						&& e.y < buttonSize.y;
			}
		});
		clearButton.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent e) {
				clearButton.setImage(activeImage);
			}

			public void mouseExit(MouseEvent e) {
				clearButton.setImage(inactiveImage);
			}

			public void mouseHover(MouseEvent e) {
			}
		});
		clearButton.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				inactiveImage.dispose();
				activeImage.dispose();
				pressedImage.dispose();
			}
		});

		clearButton.getAccessible().addAccessibleControlListener(
				new AccessibleControlAdapter() {
					public void getRole(AccessibleControlEvent e) {
						e.detail = ACC.ROLE_PUSHBUTTON;
					}
				});
		this.clearButtonContro = clearButton;
	}
	
	@Override
	public void setEnabled (boolean enabled) {
		super.setEnabled(enabled);
		txtFilter.setEnabled(enabled);
	}
	/**
	 * determines if the current client can use the native
	 * search field or not
	 */
	private static Boolean useNativeSearchField = null;
	private static boolean useNativeSearchField(Composite composite) {
		if (useNativeSearchField == null) {
			useNativeSearchField = Boolean.FALSE;
			Text testText = null;
			try {
				testText = new Text(composite, SWT.SEARCH | SWT.ICON_CANCEL);
				useNativeSearchField = new Boolean((testText.getStyle() & SWT.ICON_CANCEL) != 0);
			} finally {
				if (testText != null) {
					testText.dispose();
				}
			}
				
		}
		return useNativeSearchField.booleanValue();
	}
	
}
