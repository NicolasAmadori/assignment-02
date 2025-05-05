package pcd.ass02;

import io.reactivex.rxjava3.core.Observable;

public class PackageDepsReport implements DepsReport<ClassDepsReport>{

  private final String packageName;
  private final Observable<ClassDepsReport> classesReports;

  public PackageDepsReport(final String packageName, final Observable<ClassDepsReport> classesReports) {
    this.packageName = packageName;
    this.classesReports = classesReports;
  }

  @Override
  public Observable<ClassDepsReport> getElements() {
    return classesReports;
  }

  @Override
  public String getName() {
    return packageName;
  }
}
