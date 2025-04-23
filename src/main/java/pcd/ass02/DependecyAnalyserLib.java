package pcd.ass02;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.TypeSolver;
import io.vertx.core.*;
import io.vertx.core.file.FileSystem;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.ParserConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DependecyAnalyserLib {

  private final Vertx vertx;
  private final FileSystem fs;

  public DependecyAnalyserLib(Vertx vertx) {
    this.vertx = vertx;
    fs = vertx.fileSystem();
  }

  private void initTypeSolver(final String packagePath) {
    TypeSolver typeSolver = new CombinedTypeSolver(
      new ReflectionTypeSolver(), // for JDK classes
      new JavaParserTypeSolver(new File(packagePath))
    );
    ParserConfiguration config = new ParserConfiguration()
      .setSymbolResolver(new JavaSymbolSolver(typeSolver));
    StaticJavaParser.setConfiguration(config);
  }

  private Path getSourceRoot(Path classFile, String packageName) {
    Path packagePath = Paths.get(packageName.replace('.', File.separatorChar));
    return classFile.getParent().resolveSibling(".").normalize().resolveSibling(packagePath).normalize();
  }

  private void getDependencies(final String classPath, final String classSrc, final Promise<ClassDepsReport> promise) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(classSrc);
      ClassDepsReport classDepsReport = new ClassDepsReport(classPath); // TODO: fix src

      cu.findAll(ImportDeclaration.class).stream()
        .map(ImportDeclaration::getNameAsString)
        .distinct()
        .forEach(classDepsReport::addElement);

      promise.complete(classDepsReport);
    } catch (Exception e) {
      promise.fail(e);
    }
  }

  private Future<List<String>> findAllDirectories(String rootPath) {
    Promise<List<String>> promise = Promise.promise();
    List<String> directories = new ArrayList<>();

    fs.readDir(rootPath)
      .onSuccess(paths -> {
        List<Future<Void>> futures = new ArrayList<>();
        for (String path : paths) {
          if (!path.contains(".")) { // Check if it's a directory
            directories.add(path);
            futures.add(findAllDirectories(path).onSuccess(directories::addAll).mapEmpty());
          }
        }
        Future.all(futures)
          .onSuccess(v -> promise.complete(directories))
          .onFailure(promise::fail);
      })
      .onFailure(promise::fail);

    return promise.future();
  }

  public Future<ClassDepsReport> getClassDependencies(final String classSrcFile) {
    Promise<ClassDepsReport> promise = Promise.promise();
    fs
      .readFile(classSrcFile)
      .onSuccess(res -> {
        getDependencies(classSrcFile, res.toString(), promise);
      })
      .onFailure(err -> {
        System.err.println("Error while class reading: " + err.getMessage());
        promise.fail(err);
      });

    return promise.future();
  }

  public Future<PackageDepsReport> getPackageDependencies(final String packageSrcFolder) {
    Promise<PackageDepsReport> promise = Promise.promise();
    PackageDepsReport packageDepsReport = new PackageDepsReport(packageSrcFolder); // TODO: fix src
    try {
      fs.readDir(packageSrcFolder)
        .onSuccess(paths -> {
          List<Future<ClassDepsReport>> futures = paths.stream()
            .filter(p -> p.endsWith(".java"))
            .map(this::getClassDependencies)
            .toList();

          Future.all(futures)
            .onSuccess(classDepsReports -> {
              for (int i = 0; i < classDepsReports.size(); i++) {
                packageDepsReport.addElement(classDepsReports.resultAt(i));
              }
              promise.complete(packageDepsReport);
            })
            .onFailure(promise::fail);
        })
        .onFailure(promise::fail);
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  public Future<ProjectDepsReport> getProjectDependencies(final String projectSrcFolder) {
    Promise<ProjectDepsReport> promise = Promise.promise();
    ProjectDepsReport projectDepsReport = new ProjectDepsReport(projectSrcFolder);
    try {
//      fs.readDir(projectSrcFolder)
//        .onSuccess(paths -> {
////          paths.forEach(System.out::println);
//          List<Future<PackageDepsReport>> packageFutures = paths.stream()
//            .filter(p -> !p.contains("."))
//            .map(this::getPackageDependencies)
//            .toList();
      findAllDirectories(projectSrcFolder)
        .onSuccess(paths -> {
          List<Future<PackageDepsReport>> packageFutures = paths.stream()
          .filter(p -> !p.contains("."))
          .map(this::getPackageDependencies)
          .toList();

          Future.all(packageFutures)
            .onSuccess(packageDepsReports -> {
              for (int i = 0; i < packageDepsReports.size(); i++) {
                projectDepsReport.addElement(packageDepsReports.resultAt(i));
              }
              promise.complete(projectDepsReport);
            })
            .onFailure(promise::fail);
        })
        .onFailure(promise::fail);
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

}
