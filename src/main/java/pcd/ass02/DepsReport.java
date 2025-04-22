package pcd.ass02;

import java.util.List;

public interface DepsReport<T> {

  void addElement(T element);

  List<T> getElements();

  String getName();
}
