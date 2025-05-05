package pcd.ass02;

import io.reactivex.rxjava3.core.Observable;

public class ProjectDepsReport implements DepsReport<PackageDepsReport> {
  private final String projectName;
  private final Observable<PackageDepsReport> packagesReports;

  public ProjectDepsReport(final String projectName, final Observable<PackageDepsReport> packagesReports) {
    this.projectName = projectName;
    this.packagesReports = packagesReports;
  }

  @Override
  public Observable<PackageDepsReport> getElements() {
    return packagesReports;
  }

  @Override
  public String getName() {
    return projectName;
  }
}
