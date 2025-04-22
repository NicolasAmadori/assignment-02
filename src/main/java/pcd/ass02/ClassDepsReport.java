package pcd.ass02;

import java.util.ArrayList;
import java.util.List;

public class ClassDepsReport implements DepsReport<String> {
  private final String className;
  private final List<String> deps;

  public ClassDepsReport(final String className) {
    this.className = className;
    this.deps = new ArrayList<>();
  }

  @Override
  public void addElement(String element) {
    deps.add(element);
  }

  @Override
  public List<String> getElements() {
    return deps;
  }

  @Override
  public String getName() {
    return className;
  }

}
