package pcd.ass02;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.TypeSolver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.ParserConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DependecyAnalyserLib {

  private final Vertx vertx;
  private final FileSystem fs;

  public DependecyAnalyserLib(Vertx vertx) {
    this.vertx = vertx;
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

  private Path getSourceRoot(Path classFile, String packageName) {
    Path packagePath = Paths.get(packageName.replace('.', File.separatorChar));
    return classFile.getParent().resolveSibling(".").normalize().resolveSibling(packagePath).normalize();
  }

  private void getDependencies(final String classPath, final String classSrc, final Promise<ClassDepsReport> promise) {
    try {
      CompilationUnit cu = StaticJavaParser.parse(classSrc);
      Path srcPath = Paths.get(classPath);
      var pkg = cu.getPackageDeclaration();
      if (pkg.isPresent()) {
        String packageName = pkg.get().getNameAsString();
        srcPath = getSourceRoot(srcPath, packageName);
      } else {
        srcPath = srcPath.getParent(); //ERROR
      }
      initTypeSolver(srcPath.toString());

      ClassDepsReport classDepsReport = new ClassDepsReport(classPath);
      //              classDepsReport.addElement(e);

      cu.findAll(ClassOrInterfaceType.class).stream()
        .map(type -> {
          try {
            return type.resolve().describe();
          } catch (Exception e) {
            return "Unresolved: " + type.getNameAsString();
          }
        })
        .distinct()
        .forEach(System.out::println);
      cu.findAll(ClassOrInterfaceType.class).stream()
        .map(ClassOrInterfaceType::getNameAsString)
        .distinct()
        .forEach(System.out::println);
      cu.findAll(ImportDeclaration.class).stream()
        .map(ImportDeclaration::getNameAsString)
        .distinct()
        .forEach(System.out::println);

      promise.complete(classDepsReport);
    } catch (Exception e) {
      promise.fail(e);
    }
  }

  public Future<ClassDepsReport> getClassDependencies(final String classSrcFile) {
    Promise<ClassDepsReport> promise = Promise.promise();
    this.fs
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
    TypeSolver typeSolver = new CombinedTypeSolver(
      new ReflectionTypeSolver(), // for JDK classes
      new JavaParserTypeSolver(new File(projectSrcFolder))
    );
    ParserConfiguration config = new ParserConfiguration()
      .setSymbolResolver(new JavaSymbolSolver(typeSolver));
    StaticJavaParser.setConfiguration(config);

    Promise<ProjectDepsReport> promise = Promise.promise();
    return promise.future();
  }

}
