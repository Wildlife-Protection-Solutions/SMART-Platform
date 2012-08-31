package org.wcs.smart.plan.ui.tree;

public class FakeItem {

	  private String summary = "";
	  private String description = "";

	  public FakeItem(String summary) {
	    this.summary = summary;
	  }

	  public FakeItem(String summary, String description) {
	    this.summary = summary;
	    this.description = description;

	  }

	  public String getSummary() {
	    return summary;
	  }

	  public void setSummary(String summary) {
	    this.summary = summary;
	  }

	  public String getDescription() {
	    return description;
	  }

	  public void setDescription(String description) {
	    this.description = description;
	  }

	} 