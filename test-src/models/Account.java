package models;

import java.util.*;

public class Account {
  private String name;

  private List<Integer> marks;

  public Account(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
