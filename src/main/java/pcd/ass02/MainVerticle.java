package pcd.ass02;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    DependecyAnalyserLib analyser = new DependecyAnalyserLib(vertx);

    analyser.getClassDependencies("test-src/main/App.java")
      .onSuccess(res -> {
        startPromise.complete();
        vertx.close();
      })
      .onFailure(err -> {
        startPromise.fail(err);
        vertx.close();
      });
  }
}
