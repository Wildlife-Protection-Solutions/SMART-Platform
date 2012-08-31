package org.wcs.smart.plan.ui.tree;

import java.util.ArrayList;
import java.util.List;


public class FakeCategory {
  private String name;
  private int sort;
  private List<FakeItem> todos = new ArrayList<FakeItem>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getSort() {
    return sort;
  }

  public void setSort(int sort) {
    this.sort = sort;
  }

  public List<FakeItem> getTodos() {
    return todos;
  }
} 

