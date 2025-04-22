package pcd.ass02;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

import java.util.List;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    DependecyAnalyserLib analyser = new DependecyAnalyserLib(vertx);
    final String PATH1 = "test-src/main/App.java";
    analyser.getClassDependencies(PATH1)
      .onSuccess(res -> {
//        startPromise.complete();
//        vertx.close();
        System.out.println(PATH1 + " -> " + res.getElements());
      })
      .onFailure(err -> {
        startPromise.fail(err);
        vertx.close();
      });
    final String PATH2 = "test-src/services/";
    analyser.getPackageDependencies(PATH2)
      .onSuccess(res -> {
        startPromise.complete();
        vertx.close();
        List<String> totalDep = res.getElements().stream()
          .flatMap(classDepsReport -> classDepsReport.getElements().stream())
          .toList();
        System.out.println(PATH2 + " -> " + totalDep);
      })
      .onFailure(err -> {
        startPromise.fail(err);
        vertx.close();
      });
  }
}
