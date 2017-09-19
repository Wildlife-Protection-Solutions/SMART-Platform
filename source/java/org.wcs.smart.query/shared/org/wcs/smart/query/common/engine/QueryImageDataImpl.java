package org.wcs.smart.query.common.engine;

import java.util.UUID;

import org.wcs.smart.common.attachment.ISmartAttachment;

public class QueryImageDataImpl implements IQueryImageData {

	private String description;
	private ISmartAttachment attachment;
	
	private UUID uuid;
	
	public QueryImageDataImpl(String description, ISmartAttachment attachment, UUID sourceUuid) {
		this.description = description;
		this.attachment = attachment;
		this.uuid = sourceUuid;
	}
	
	public UUID getSourceUuid() {
		return this.uuid;
	}
	
	@Override
	public ISmartAttachment getAttachments() {
		return this.attachment;
	}

	@Override
	public String getHeaderString() {
		return description;
	}

}
