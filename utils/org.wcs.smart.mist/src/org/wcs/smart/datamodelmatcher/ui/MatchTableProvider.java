package org.wcs.smart.datamodelmatcher.ui;


import java.util.ArrayList;
import java.util.List;

public enum MatchTableProvider {
  INSTANCE;

  private List<MatchRow> rows;

  private MatchTableProvider() {
    rows = new ArrayList<MatchRow>();

    rows.add(new MatchRow(false, new MistItem("humanactivities.land.cleared.peopleconfronted.localpeople."), new SmartItem("") ));
    
  }

  public List<MatchRow> getRows() {
    return rows;
  }

} 