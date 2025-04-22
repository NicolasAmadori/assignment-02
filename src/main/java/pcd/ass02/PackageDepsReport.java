package pcd.ass02;

import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;

public class PackageDepsReport implements DepsReport<Future<ClassDepsReport>>{

  private final String packageName;
  private final List<Future<ClassDepsReport>> classesReports;

  public PackageDepsReport(String packageName) {
    this.packageName = packageName;
    classesReports = new ArrayList<>();
  }

  @Override
  public void addElement(Future<ClassDepsReport> element) {
    this.classesReports.add(element);
  }

  @Override
  public List<Future<ClassDepsReport>> getElements() {
    return classesReports;
  }

  @Override
  public String getName() {
    return packageName;
  }
}
