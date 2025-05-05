package pcd.ass02;

import io.reactivex.rxjava3.core.Observable;

public class ClassDepsReport implements DepsReport<String> {
  private final String className;
  private final Observable<String> deps;

  public ClassDepsReport(final String className, final Observable<String> deps) {
    this.className = className;
    this.deps = deps;
  }

  @Override
  public Observable<String> getElements() {
    return deps;
  }

  @Override
  public String getName() {
    return className;
  }

}
