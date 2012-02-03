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
package org.wcs.smart;

import net.refractions.udig.ui.WorkbenchConfiguration;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class SmartWorkbenchConfigurator  implements WorkbenchConfiguration {

	/**
     * Called to initialise the initial workbench windows size (1023,756)
     * and define what kind of things are visible by default.
     * <ul>
     * This implementation is only a default; you can use the
     * "workbenchConfiguration" extension point when you are implementing your
     * own udig based application in order to provide a custom implementation.
     * </p>
     * Specifcally people override this class in order to:
     * <ul>
     * <li>turn on perspectives:
     * <pre><code>configurer.setShowPerspectiveBar(true)</code></pre></li>
     * <li>Change the default size to be based on the screen size</li>
     * <pre><code></code></pre>
     * </ul>
     * 
     * @param configurer IWorkbenchWindowConfigurer used for configuring the workbench window
     */
    public void configureWorkbench( IWorkbenchWindowConfigurer configurer ) {
        configurer.setShowProgressIndicator(true);
//        configurer.setInitialSize(new Point(1024, 768));
//        
//        //Rectangle bounds = Display.getDefault().getPrimaryMonitor().getClientArea();
//        //configurer.setInitialSize(new Point(bounds.width, bounds.height));
//        
//        configurer.setShowCoolBar(true);
//        configurer.setShowFastViewBars(true);
//        //configurer.setShowPerspectiveBar(true);
//        
//        // these are required for the update site if nothing else
//        configurer.setShowStatusLine(true);
//        configurer.setShowProgressIndicator(true);
    }

}