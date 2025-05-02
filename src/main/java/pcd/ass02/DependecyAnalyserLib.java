package pcd.ass02;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.nio.file.*;
import java.util.stream.Stream;

public class DependecyAnalyserLib {

  private DependecyAnalyserLib() {}

  private static void initTypeSolver(final String packagePath) {
    TypeSolver typeSolver = new CombinedTypeSolver(
      new ReflectionTypeSolver(), // for JDK classes
      new JavaParserTypeSolver(new File(packagePath))
    );
    ParserConfiguration config = new ParserConfiguration()
      .setSymbolResolver(new JavaSymbolSolver(typeSolver));
    StaticJavaParser.setConfiguration(config);
  }

  private static String getSourceRoot(String classPath, String packagePath) {
    var packageRoot = packagePath.split("\\.")[0];
    return classPath.split(packageRoot)[0];
  }

  private static String getPackage(final String classSrc) throws NoSuchElementException {
    CompilationUnit cu = StaticJavaParser.parse(classSrc);
    return cu.getPackageDeclaration().get().getNameAsString();
  }

  private static String editPath(String path) {
    // Normalize slashes and strip ".java"
    String normalized = path.replace("\\", "/").replace(".java", "");
    String[] parts = normalized.split("/");

    // Skip the first segment (e.g., "test-src")
    return String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
  }

  private static Single<ClassDepsReport> getDependencies(final String classPath, final String classSrc) {
    return Single.create(emitter -> {
      try {
        initTypeSolver(getSourceRoot(classPath, getPackage(classSrc)));

        CompilationUnit cu = StaticJavaParser.parse(classSrc);
        ClassDepsReport classDepsReport = new ClassDepsReport(editPath(classPath));

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

        emitter.onSuccess(classDepsReport);
      } catch (Exception e) {
        emitter.onError(e);
      }
    });
  }

  private static Single<List<String>> findAllDirectories(String rootPath) {
    return Single.fromCallable(() -> {
      List<String> directories = new ArrayList<>();
      try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
        paths.filter(Files::isDirectory)
          .map(Path::toString)
          .filter(path -> !path.endsWith(rootPath))
//          .map(path -> path.replace(rootPath, ""))
//          .map(path -> path.replace("\\", "."))
          .forEach(directories::add);
      }
      return directories;
    }).subscribeOn(Schedulers.io());
  }

  private static Single<ClassDepsReport> getClassDependencies(final String classSrcFile) {
    return Single.fromCallable(() -> Files.readString(Paths.get(classSrcFile)))
      .subscribeOn(Schedulers.io())
      .flatMap(content -> getDependencies(classSrcFile, content));
  }

  private static Single<PackageDepsReport> getPackageDependencies(final String packageSrcFolder) {
    return Single.fromCallable(() -> {
        try (Stream<Path> paths = Files.list(Paths.get(packageSrcFolder))) {
          return paths
            .map(Path::toString)
            .filter(string -> string.endsWith(".java"))
            .toList();
        }
      }).subscribeOn(Schedulers.io())
      .flatMap(files -> Observable.fromIterable(files)
        .flatMapSingle(DependecyAnalyserLib::getClassDependencies)
        .toList()
        .map(classDeps -> {
          PackageDepsReport report = new PackageDepsReport(editPath(packageSrcFolder));
          classDeps.forEach(report::addElement);
          return report;
        }));
  }

  public static  Single<ProjectDepsReport> getProjectDependencies(final String projectSrcFolder) {
    return findAllDirectories(projectSrcFolder)
      .flatMap(directories -> Observable.fromIterable(directories)
        .flatMapSingle(DependecyAnalyserLib::getPackageDependencies)
        .toList()
        .map(packageDeps -> {
          ProjectDepsReport report = new ProjectDepsReport(editPath(projectSrcFolder));
          packageDeps.forEach(report::addElement);
          return report;
      }));
  }

}
