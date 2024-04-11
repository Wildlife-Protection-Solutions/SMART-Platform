/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.observation.model;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Interface for attachments that can have a tags type
 * 
 * @author Emily
 * @since 8.0
 *
 */
public interface ITaggedAttachment {

	/**
	 * Links to associated tags
	 * @return
	 */
	public List<AttachmentTagLink> getAttachmentTags();
	
	/**
	 * set tag links
	 * @param tags
	 */
	public void setAttachmentTags(List<AttachmentTagLink> tags);
	
	/**
	 * 
	 * @return sorted comma delimited list of tags associated with the attachment
	 */
	public default String getTagsAsString() {
		if (getAttachmentTags() == null) return "";  //$NON-NLS-1$
		
		List<String> tags = new ArrayList<>();
		
		getAttachmentTags().forEach(e->tags.add(e.getTag().getName()));
		tags.sort((a,b)->Collator.getInstance().compare(a, b));
		
		StringJoiner sj = new StringJoiner(", "); //$NON-NLS-1$
		tags.forEach(t->sj.add(t));
		
		return sj.toString();
	}
}
