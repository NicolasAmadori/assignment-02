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
import java.util.Arrays;
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
    // Skip the first segment ("src")
    return String.join(".", Arrays.copyOfRange(parts, 1, parts.length));
  }

  private static Observable<String> findAllDirectories(String rootPath) {
    return Observable.<String>create(emitter -> {
      try (Stream<Path> paths = Files.walk(Paths.get(rootPath))) {
        paths.filter(Files::isDirectory)
          .map(Path::toString)
          .filter(path -> !path.endsWith(rootPath))
          .forEach(emitter::onNext);
        emitter.onComplete();
      } catch (IOException e) {
        emitter.onError(e);
      }
    }).subscribeOn(Schedulers.io());
  }

  private static ClassDepsReport getClassDependencies(final String classSrcFile) {
    Observable<String> deps = Observable.<String>create(emitter -> {
        try {
          var content = Files.readString(Paths.get(classSrcFile));
          initTypeSolver(getSourceRoot(classSrcFile, getPackage(content)));
          CompilationUnit cu = StaticJavaParser.parse(content);

          cu.findAll(ImportDeclaration.class).forEach(importDeclaration ->
            emitter.onNext(importDeclaration.getNameAsString())
          );

          cu.findAll(ClassOrInterfaceType.class).forEach(type -> {
            ResolvedType resolved = type.resolve();
            if (resolved.isReferenceType()) {
              String qualifiedName = resolved.asReferenceType().getQualifiedName();
              if (!qualifiedName.startsWith("java.lang.")) {
                emitter.onNext(qualifiedName);
              }
            }
          });

          emitter.onComplete();
        } catch (IOException e) {
          emitter.onError(e);
        }
      }).subscribeOn(Schedulers.io())
      .distinct();

    return new ClassDepsReport(editPath(classSrcFile), deps);
  }


  private static PackageDepsReport getPackageDependencies(final String packageSrcFolder) {
    Observable<ClassDepsReport> classes = Observable.<String>create(emitter -> {
        try {
          Stream<Path> paths = Files.list(Paths.get(packageSrcFolder));
          paths
            .map(Path::toString)
            .filter(string -> string.endsWith(".java"))
            .forEach(path -> emitter.onNext(path));

          emitter.onComplete();
        } catch (IOException e) {
          emitter.onError(e);
        }
      }).subscribeOn(Schedulers.io())
      .map(DependecyAnalyserLib::getClassDependencies);

    return new PackageDepsReport(editPath(packageSrcFolder), classes);
  }


  public static ProjectDepsReport getProjectDependencies(final String projectSrcFolder) {
    Observable<PackageDepsReport> packages = findAllDirectories(projectSrcFolder)
      .map(DependecyAnalyserLib::getPackageDependencies);
    return new ProjectDepsReport(editPath(projectSrcFolder), packages);
  }

}
