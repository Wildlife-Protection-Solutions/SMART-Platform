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
package org.wcs.smart.cybertracker.export;

import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;

/**
 * Class used when it is required to map certain item to certain node.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class CyberTrackerIdMap extends CyberTrackerId {
	
	private CyberTrackerId node;
	private CyberTrackerId item;
	
	public CyberTrackerIdMap(CyberTrackerId node, CyberTrackerId item) {
		this.node = node;
		this.item = item;
	}
	
	public String getItemId() {
		return item.getItemId();
	}
	public String getItemTranslatedId() {
		return item.getItemTranslatedId();
	}
	public String getNodeId() {
		return node.getNodeId();
	}
	public String getNodeTranslatedId() {
		return node.getNodeTranslatedId();
	}
	
}
