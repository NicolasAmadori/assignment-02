package pcd.ass02;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private Map<String, Map<String, List<String>>> projectDepsToMap(ProjectDepsReport projectDepsReport) {
    return projectDepsReport.getElements().stream()
      .collect(Collectors.toMap(
        PackageDepsReport::getName,
        this::packageDepsToMap
      ));
  }

  private Map<String, List<String>> packageDepsToMap(PackageDepsReport packageDepsReport) {
    return packageDepsReport.getElements().stream()
      .collect(Collectors.toMap(
        ClassDepsReport::getName,
        ClassDepsReport::getElements
      ));
  }

  @Override
  public void start(Promise<Void> startPromise) {
    DependecyAnalyserLib analyser = new DependecyAnalyserLib(vertx);
    final String PATH1 = "test-src/main/App.java";
    analyser.getClassDependencies(PATH1)
      .onSuccess(res -> {
//        startPromise.complete();
//        vertx.close();
        System.out.println(PATH1 + " -> \t" + res.getElements());
      })
      .onFailure(err -> {
        startPromise.fail(err);
        vertx.close();
      });
    final String PATH2 = "test-src/services/";
    analyser.getPackageDependencies(PATH2)
      .onSuccess(res -> {
//        startPromise.complete();
//        vertx.close();
        System.out.println(res.getName() + " -> \t" + packageDepsToMap(res));
      })
      .onFailure(err -> {
        startPromise.fail(err);
        vertx.close();
      });
    final String PATH3 = "test-src/";
    analyser.getProjectDependencies(PATH3)
      .onSuccess(res -> {
        startPromise.complete();
        vertx.close();
        System.out.println(res.getName() + " -> \t" + projectDepsToMap(res));
      })
      .onFailure(err -> {
        startPromise.fail(err);
        vertx.close();
      });
  }
}
