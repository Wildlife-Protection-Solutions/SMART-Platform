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
package org.wcs.smart.ui;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;

/**
 * Simple search text box that mimics the text box from the
 * filter tree viewer.  
 * 
 * @author Emily
 *
 */
public class SearchTextBox extends Composite {
	
	private static final String INITIAL_TEXT = Messages.SearchTextBox_SearchInfoTest;
	/**
	 * Image descriptor for enabled clear button.
	 */
	private static final String CLEAR_ICON = "org.eclipse.ui.internal.dialogs.CLEAR_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor for disabled clear button.
	 */
	private static final String DISABLED_CLEAR_ICON= "org.eclipse.ui.internal.dialogs.DCLEAR_ICON"; //$NON-NLS-1$

	/**
	 * Get image descriptors for the clear button.
	 */
	static {
		ImageDescriptor descriptor = AbstractUIPlugin
				.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,
						"$nl$/icons/full/etool16/clear_co.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			SmartPlugIn.getDefault().getImageRegistry().put(CLEAR_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				PlatformUI.PLUGIN_ID, "$nl$/icons/full/dtool16/clear_co.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			SmartPlugIn.getDefault().getImageRegistry().put(DISABLED_CLEAR_ICON, descriptor);
		}
	}
	
	private Text filterText;
	private Label clearButtonControl;
	private Job refreshJob;
	private String lastSearchString; 
	
	/**
	 * Creates new search box.
	 * @param parent
	 * @param style
	 */
	public SearchTextBox(Composite parent, int style) {
		super(parent, !useNativeSearchField(parent) ? SWT.BORDER | style : style);
		
		if (!useNativeSearchField(parent)) {
			setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		}
		
		GridLayout filterLayout= new GridLayout(2, false);
		filterLayout.marginHeight= 0;
		filterLayout.marginWidth= 0;
		setLayout(filterLayout);
		
		createFilterText(this);
		createClearTextNew(this);
		if (clearButtonControl != null) {
			// initially there is no text to clear
			clearButtonControl.setVisible(false);
		}
		
		setFilterText(INITIAL_TEXT);
		textChanged();
	}
	
	/**
	 * Creates the text control for entering the filter text. 
	 * 
	 */
	private Text doCreateFilterText(Composite parent) {
		if (useNativeSearchField(parent)) {
			return new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH
					| SWT.ICON_CANCEL);
		}
		return new Text(parent, SWT.SINGLE);
	}
	
	/**
	 * Sets the filter text
	 * 
	 * @param text
	 */
	protected void setFilterText(String text){
		if (filterText != null) {
			filterText.setText(text);
			filterText.selectAll();
		}
	}
	
	/**
	 * Creates the filter text and adds listeners.
	 */
	private void createFilterText(Composite parent) {
		filterText = doCreateFilterText(parent);
		
		filterText.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
			}

			public void focusLost(FocusEvent e) {
				if (filterText.getText().equals(INITIAL_TEXT)) {
					setFilterText(""); //$NON-NLS-1$
					textChanged();
				}
				if (filterText.getText().isEmpty()){
					filterText.setText(INITIAL_TEXT);
					textChanged();
				}
			}
		});

		
		filterText.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent e) {
					if (filterText.getText().equals(INITIAL_TEXT)) {
						setFilterText(""); //$NON-NLS-1$
						textChanged();
					}
				}
			});
		

		filterText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (getFocusControl() != null && e.keyCode == SWT.ARROW_DOWN) {
					getFocusControl().setFocus();
					return;
				}
			}
		});

		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				textChanged();
			}
		});

		// if we're using a field with built in cancel we need to listen for
		// default selection changes (which tell us the cancel button has been
		// pressed)
		if ((filterText.getStyle() & SWT.ICON_CANCEL) != 0) {
			filterText.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetDefaultSelected(SelectionEvent e) {
					if (e.detail == SWT.ICON_CANCEL){
						setFilterText(""); //$NON-NLS-1$
						textChanged();
					}
				}
			});
		}

		GridData gridData= new GridData(SWT.FILL, SWT.CENTER, true, false);
		// if the text widget supported cancel then it will have it's own
		// integrated button. We can take all of the space.
		if ((filterText.getStyle() & SWT.ICON_CANCEL) != 0)
			gridData.horizontalSpan = 2;
		filterText.setLayoutData(gridData);
		
	}
	
	private void createClearTextNew(Composite parent) {
		// only create the button if the text widget doesn't support one
		// natively
		if ((filterText.getStyle() & SWT.ICON_CANCEL) == 0) {
			final Image inactiveImage= SmartPlugIn.getDefault().getImageRegistry().getDescriptor(DISABLED_CLEAR_ICON).createImage();
			final Image activeImage= SmartPlugIn.getDefault().getImageRegistry().getDescriptor(CLEAR_ICON).createImage();
			final Image pressedImage= new Image(getDisplay(), activeImage, SWT.IMAGE_GRAY);
			
			final Label clearButton= new Label(parent, SWT.NONE);
			clearButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			clearButton.setImage(inactiveImage);
			clearButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			clearButton.setToolTipText(Messages.SearchTextBox_ClearSearchTooltip);
			clearButton.addMouseListener(new MouseAdapter() {
				private MouseMoveListener fMoveListener;

				public void mouseDown(MouseEvent e) {
					clearButton.setImage(pressedImage);
					fMoveListener= new MouseMoveListener() {
						private boolean fMouseInButton= true;

						public void mouseMove(MouseEvent e) {
							boolean mouseInButton= isMouseInButton(e);
							if (mouseInButton != fMouseInButton) {
								fMouseInButton= mouseInButton;
								clearButton.setImage(mouseInButton ? pressedImage : inactiveImage);
							}
						}
					};
					clearButton.addMouseMoveListener(fMoveListener);
				}

				public void mouseUp(MouseEvent e) {
					if (fMoveListener != null) {
						clearButton.removeMouseMoveListener(fMoveListener);
						fMoveListener= null;
						boolean mouseInButton= isMouseInButton(e);
						clearButton.setImage(mouseInButton ? activeImage : inactiveImage);
						if (mouseInButton) {
							setFilterText(""); //$NON-NLS-1$
							textChanged();
							filterText.setFocus();
						}
					}
				}
				
				private boolean isMouseInButton(MouseEvent e) {
					Point buttonSize = clearButton.getSize();
					return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y && e.y < buttonSize.y;
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
						e.detail= ACC.ROLE_PUSHBUTTON;
					}
			});
			this.clearButtonControl= clearButton;
		}
	}
	
	/**
	 * 
	 * @return the filter string or null if none entered
	 */
	public String getFilterString(){
		String txt = this.filterText.getText();
		if (txt.isEmpty() || txt.equals(INITIAL_TEXT)){
			return null;
		}
		return txt;
	}
	
	/**
	 * Update the receiver after the text has changed.
	 */
	private void textChanged() {
		if ((lastSearchString == null && getFilterString() == null) || 
				(lastSearchString != null && lastSearchString.equals(getFilterString()))){
			//nothing has changed; we don't want to rerun our search
			return;
		}
	
		//configure ui
		boolean isInit = filterText.getText().equals(INITIAL_TEXT);
		if (isInit){
			filterText.setForeground(filterText.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		}else{
			filterText.setForeground(filterText.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		}
		if (clearButtonControl != null) clearButtonControl.setVisible(!isInit);
		
		//run search
		lastSearchString = getFilterString();
		if (refreshJob != null){
			refreshJob.cancel();
			refreshJob.schedule(getRefreshJobDelay());
		}
	}
	
	/**
	 * The Job to run whenever the text has been modified.  This is
	 * not run in the Display thread.
	 * 
	 * @param refreshJob
	 */
	public void setRefreshJob(Job refreshJob){
		this.refreshJob = refreshJob;
	}
	
	/**
	 * Return the time delay that should be used when scheduling the
	 * filter refresh job.  Subclasses may override.
	 * 
	 * @return a time delay in milliseconds before the job should run
	 * 
	 * @since 3.5
	 */
	protected long getRefreshJobDelay() {
		return 200;
	}
	
	/**
	 * Return the control which should get focus
	 * on return pressed in the search box.  Can return
	 * null if nothing should happen.
	 * Subclasses may override.
	 * @return null
	 */
	protected Composite getFocusControl(){
		return null;
	}
	
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
