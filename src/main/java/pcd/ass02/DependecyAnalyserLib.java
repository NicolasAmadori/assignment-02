package pcd.ass02;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.buffer.Buffer;

public class DependecyAnalyserLib {

//  loop {
//    Event ev = waitForEvent(eventQueue)
//    Handler handler = selectHandler(ev)
//    execute(handler)
//  }
  private final Vertx vertx;
  private final FileSystem fs;

  public DependecyAnalyserLib(Vertx vertx) {
    this.vertx = vertx;
    this.fs = vertx.fileSystem();
  }

  public Future<ClassDepsReport> getClassDependencies(final String classSrcFile) {
    Promise<ClassDepsReport> promise = Promise.promise();
    this.fs
      .readFile(classSrcFile)
      .onSuccess(res -> {
        try {
          CompilationUnit cu = StaticJavaParser.parse(res.toString());
          ClassDepsReport classDepsReport = new ClassDepsReport(classSrcFile);
          //              classDepsReport.addElement(e);
          cu.findAll(ClassOrInterfaceType.class).stream()
            .map(ClassOrInterfaceType::getNameAsString)
            .distinct()
            .forEach(System.out::println);
          promise.complete(classDepsReport);
        } catch (Exception e) {
          promise.fail(e);
        }
      })
      .onFailure(err -> {
        System.err.println("Error while class reading: " + err.getMessage());
        promise.fail(err);
      });

    return promise.future();
  }

  public Future<PackageDepsReport> getPackageDependencies(final String packageSrcFolder) {
    Promise<PackageDepsReport> promise = Promise.promise();
    this.fs
      .readFile(packageSrcFolder)
      .onSuccess(res -> {

      })
      .onFailure(err -> {
        System.err.println("Error while package reading: " + err.getMessage());
        promise.fail(err);
      });
    return promise.future();
  }

  public Future<ProjectDepsReport> getProjectDependencies(final String projectSrcFolder) {
    Promise<ProjectDepsReport> promise = Promise.promise();
    return promise.future();
  }

}
