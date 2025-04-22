package pcd.ass02;

import java.util.ArrayList;
import java.util.List;

public class ProjectDepsReport implements DepsReport<PackageDepsReport> {
  private final String projectName;
  private final List<PackageDepsReport> packagesReports;

  public ProjectDepsReport(String projectName) {
    this.projectName = projectName;
    this.packagesReports = new ArrayList<>();
  }

  @Override
  public void addElement(PackageDepsReport element) {
    this.packagesReports.add(element);
  }

  @Override
  public List<PackageDepsReport> getElements() {
    return packagesReports;
  }

  @Override
  public String getName() {
    return projectName;
  }
}
