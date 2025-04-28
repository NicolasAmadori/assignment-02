package pcd.ass02;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    DependecyAnalyserLib analyser = new DependecyAnalyserLib(vertx);
    final String PATH1 = "test-src/main/App.java";
    final String PATH2 = "test-src/services/";
    final String PATH3 = "test-src/";

    Future.all(List.of(analyser.getClassDependencies(PATH1), analyser.getPackageDependencies(PATH2), analyser.getProjectDependencies(PATH3)))
        .onSuccess(compositeFuture -> {
            startPromise.complete();
            vertx.close();
        })
      .onFailure( err -> {
          startPromise.fail(err);
          vertx.close();
        }
      );
  }
}
