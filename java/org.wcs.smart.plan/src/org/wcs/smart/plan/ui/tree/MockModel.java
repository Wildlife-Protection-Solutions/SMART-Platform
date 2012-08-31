package org.wcs.smart.plan.ui.tree;

import java.util.ArrayList;
import java.util.List;



public class MockModel  {

  public List<FakeCategory> getCategories() {
    List<FakeCategory> categories = new ArrayList<FakeCategory>();
    FakeCategory category = new FakeCategory();
    category.setName("CA Plan Aug2012");
    categories.add(category);
    FakeItem todo = new FakeItem("Aug 2012 Poacher Lake Patrol");
    category.getTodos().add(todo);
    todo = new FakeItem("Aug 2012 Mount Baker Patrol");
    category.getTodos().add(todo);
    
    category = new FakeCategory();
    category.setName("CA Plan July 2012");
    categories.add(category);
    todo = new FakeItem("Patrol Route #66, July 2012");
    category.getTodos().add(todo);
    
    return categories;
  }

} 