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
package org.wcs.smart.report.birt.map;

import org.eclipse.emf.common.notify.Adapter;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.impl.MapImpl;

/**
 * Map extensions for rendering maps inside BIRT
 */
public class BirtMap extends MapImpl {

	@Override
	public void addDeepAdapter(Adapter adapter) {
	}

	@Override
	public void removeDeepAdapter(Adapter adapter) {
	}

	@Override
	public void lowerLayer(Layer layer) {
	}

	@Override
	public void raiseLayer(Layer layer) {
	}

	@Override
	public void sendToFrontLayer(Layer layer) {
	}

	@Override
	public void sendToBackLayer(Layer layer) {
	}

	@Override
	public void sendToIndexLayer(Layer layer, int index) {
	}
}
