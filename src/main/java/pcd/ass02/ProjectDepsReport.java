package pcd.ass02;

import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;

public class ProjectDepsReport implements DepsReport<Future<PackageDepsReport>> {
  private final String projectName;
  private final List<Future<PackageDepsReport>> packagesReports;

  public ProjectDepsReport(String projectName) {
    this.projectName = projectName;
    this.packagesReports = new ArrayList<>();
  }

  @Override
  public void addElement(Future<PackageDepsReport> element) {
    this.packagesReports.add(element);
  }

  @Override
  public List<Future<PackageDepsReport>> getElements() {
    return packagesReports;
  }

  @Override
  public String getName() {
    return projectName;
  }
}
