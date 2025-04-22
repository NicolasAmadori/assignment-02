package pcd.ass02;

import java.util.ArrayList;
import java.util.List;

public class PackageDepsReport implements DepsReport<ClassDepsReport>{

  private final String packageName;
  private final List<ClassDepsReport> classesReports;

  public PackageDepsReport(String packageName) {
    this.packageName = packageName;
    classesReports = new ArrayList<>();
  }

  @Override
  public void addElement(ClassDepsReport element) {
    this.classesReports.add(element);
  }

  @Override
  public List<ClassDepsReport> getElements() {
    return classesReports;
  }

  @Override
  public String getName() {
    return packageName;
  }
}
