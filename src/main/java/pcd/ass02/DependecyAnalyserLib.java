package pcd.ass02;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.File;
import java.io.IOException;
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

//  private static ClassDepsReport getDependencies(final String classPath, final String classSrc) {
//    initTypeSolver(getSourceRoot(classPath, getPackage(classSrc)));
//    CompilationUnit cu = StaticJavaParser.parse(classSrc);
//    Observable<String> obs = Observable.fromIterable(() ->
//      Stream.concat(
//        cu.findAll(ImportDeclaration.class).stream()
//          .map(ImportDeclaration::getNameAsString),
//        cu.findAll(ClassOrInterfaceType.class).stream()
//          .map(ClassOrInterfaceType::resolve)
//          .filter(ResolvedType::isReferenceType)
//          .map(rt -> rt.asReferenceType().getQualifiedName())
//          .filter(name -> !name.startsWith("java.lang."))
//      ).distinct().iterator()
//    );
//    return new ClassDepsReport(editPath(classPath), obs);
//  }

  private static Observable<String> findAllDirectories(String rootPath) {
    return Observable.fromIterable(() -> {
      try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
        return paths.filter(Files::isDirectory)
          .map(Path::toString)
          .filter(path -> !path.endsWith(rootPath))
          .iterator();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).subscribeOn(Schedulers.io());
//    return Single.fromCallable(() -> {
//      List<String> directories = new ArrayList<>();
//      try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
//        paths.filter(Files::isDirectory)
//          .map(Path::toString)
//          .filter(path -> !path.endsWith(rootPath))
//          .forEach(directories::add);
//      }
//      return directories;
//    }).subscribeOn(Schedulers.io());
  }

  private static ClassDepsReport getClassDependencies(final String classSrcFile) throws IOException {
    var content = Files.readString(Paths.get(classSrcFile));
    initTypeSolver(getSourceRoot(classSrcFile, getPackage(content)));
    CompilationUnit cu = StaticJavaParser.parse(content);

    Observable<String> imports = Observable.fromIterable(cu.findAll(ImportDeclaration.class))
      .map(ImportDeclaration::getNameAsString);

    Observable<String> types = Observable.fromIterable(cu.findAll(ClassOrInterfaceType.class))
      .flatMap(type -> {
        ResolvedType resolved = type.resolve();
        if (resolved.isReferenceType()) {
          String qualifiedName = resolved.asReferenceType().getQualifiedName();
          if (!qualifiedName.startsWith("java.lang.")) {
            return Observable.just(qualifiedName);
          }
        }
        return Observable.empty();
      });

    Observable<String> deps = Observable.merge(imports, types)
      .distinct();

    return new ClassDepsReport(editPath(classSrcFile), deps);
  }

  private static PackageDepsReport getPackageDependencies(final String packageSrcFolder) {
    Observable<ClassDepsReport> classes = Observable.fromIterable(() -> {
      try (Stream<Path> paths = Files.list(Paths.get(packageSrcFolder))) {
        return paths
          .map(Path::toString)
          .filter(string -> string.endsWith(".java"))
          .iterator();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).subscribeOn(Schedulers.io())
      .map(DependecyAnalyserLib::getClassDependencies);
    return new PackageDepsReport(editPath(packageSrcFolder), classes);
//     Single.fromCallable(() -> {
//        try (Stream<Path> paths = Files.list(Paths.get(packageSrcFolder))) {
//          return paths
//            .map(Path::toString)
//            .filter(string -> string.endsWith(".java"))
//            .toList();
//        }
//      }).subscribeOn(Schedulers.io())
//      .flatMap(files -> Observable.fromIterable(files)
//        .flatMapSingle(DependecyAnalyserLib::getClassDependencies)
//        .toList()
//        .map(classDeps -> {
//          PackageDepsReport report = new PackageDepsReport(editPath(packageSrcFolder));
//          classDeps.forEach(report::addElement);
//          return report;
//        }));
  }

  public static ProjectDepsReport getProjectDependencies(final String projectSrcFolder) {
    Observable<PackageDepsReport> packages = findAllDirectories(projectSrcFolder)
      .map(DependecyAnalyserLib::getPackageDependencies);

    return new ProjectDepsReport(editPath(projectSrcFolder), packages);
  }

}
