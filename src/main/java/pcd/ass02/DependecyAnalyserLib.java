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
import com.github.javaparser.ParserConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class DependecyAnalyserLib {

  private final FileSystem fs;

  public DependecyAnalyserLib(Vertx vertx) {
    this.fs = vertx.fileSystem();
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

  private String getSourceRoot(String classPath, String packagePath) {
    var packageRoot = packagePath.split("\\.")[0];
    return classPath.split(packageRoot)[0];
  }

  private String getPackage(final String classSrc) throws NoSuchElementException {
    CompilationUnit cu = StaticJavaParser.parse(classSrc);
    return cu.getPackageDeclaration().get().getNameAsString();
  }

  private void getDependencies(final String classPath, final String classSrc, final Promise<ClassDepsReport> promise) {
    try {
      initTypeSolver(getSourceRoot(classPath, getPackage(classSrc)));

      CompilationUnit cu = StaticJavaParser.parse(classSrc);
      ClassDepsReport classDepsReport = new ClassDepsReport(classPath);

      cu.findAll(ImportDeclaration.class).stream()
        .map(ImportDeclaration::getNameAsString)
        .distinct()
        .forEach(classDepsReport::addElement);

      cu.findAll(ClassOrInterfaceType.class).forEach(classType -> {
        var resolvedType = classType.resolve();
        if (resolvedType.isReferenceType()) {
          var qualifiedName = resolvedType.asReferenceType().getQualifiedName();
          if (!classDepsReport.getElements().contains(qualifiedName) && !qualifiedName.startsWith("java.lang.")) {
            classDepsReport.addElement(qualifiedName);
          }
        }
      });

      promise.complete(classDepsReport);
    } catch (Exception e) {
      promise.fail(e);
    }
  }

  private Future<List<String>> findAllDirectories(String rootPath) {
    Promise<List<String>> promise = Promise.promise();
    List<String> directories = new ArrayList<>();

    fs.readDir(rootPath).compose(paths -> {
        List<Future<Void>> composedFutures = new ArrayList<>();

        for (String path : paths) {
          Future<Void> composed = fs.props(path).compose(props -> {
            if (!props.isDirectory()) {
              return Future.succeededFuture(); // non è una directory, skip
            }
            directories.add(path);
            return findAllDirectories(path)
              .compose(subDirs -> {
                directories.addAll(subDirs);
                return Future.succeededFuture();
              });
          });

          composedFutures.add(composed);
        }

        return Future.all(composedFutures).mapEmpty();
      }).onSuccess(v -> promise.complete(directories))
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
    PackageDepsReport packageDepsReport = new PackageDepsReport(packageSrcFolder);
    fs.readDir(packageSrcFolder)
      .onSuccess(paths -> {
        List<Future<ClassDepsReport>> futures = paths.stream()
          .filter(p -> p.endsWith(".java"))
          .map(p -> p.replace("\\", "/"))
          .map(p -> p.substring(p.indexOf(packageSrcFolder)))
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
    return promise.future();
  }

  public Future<ProjectDepsReport> getProjectDependencies(final String projectSrcFolder) {
    Promise<ProjectDepsReport> promise = Promise.promise();
    ProjectDepsReport projectDepsReport = new ProjectDepsReport(projectSrcFolder);
    findAllDirectories(projectSrcFolder)
      .onSuccess(packages -> {
        List<Future<PackageDepsReport>> packageFutures = packages.stream()
          .map(p -> p.replace("\\", "/"))
          .map(p -> p.substring(p.indexOf(projectSrcFolder)))
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
    return promise.future();
  }

}
