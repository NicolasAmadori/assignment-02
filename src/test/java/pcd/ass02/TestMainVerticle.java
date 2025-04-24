package pcd.ass02;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  private final Boolean doPrint = true;

  private Map<String, Map<String, List<String>>> projectDepsToMap(ProjectDepsReport projectDepsReport) {
    return projectDepsReport.getElements().stream()
      .collect(Collectors.toMap(
        PackageDepsReport::getName,
        this::packageDepsToMap
      ));
  }

  private Map<String, List<String>> packageDepsToMap(PackageDepsReport packageDepsReport) {
    return packageDepsReport.getElements().stream()
      .collect(Collectors.toMap(
        ClassDepsReport::getName,
        ClassDepsReport::getElements
      ));
  }

  @Test
  void testClassDependencies(Vertx vertx, VertxTestContext testContext) {
    final String PATH1 = "test-src/main/App.java";
    final List<String> expected = List.of("models.User", "services.UserService", "utils.Logger");
    DependecyAnalyserLib analyser = new DependecyAnalyserLib(vertx);

    analyser.getClassDependencies(PATH1).onSuccess(res -> {
      if (doPrint) System.out.println(res.getName() + " -> \t" + res.getElements());

      assertNotNull(res);
      assertEquals(res.getElements().size(), expected.size());
      assertTrue(res.getElements().containsAll(expected));
      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }

  @Test
  void testPackageDependencies(Vertx vertx, VertxTestContext testContext) {
    final String PATH2 = "test-src/services/";
    final Map<String, List<String>> expected = Map.of(
      "test-src/services/Logger.java", List.of("java.io.File"),
      "test-src/services/UserService.java", List.of("models.User", "main.utils.Logger")
    );
    DependecyAnalyserLib analyser = new DependecyAnalyserLib(vertx);

    analyser.getPackageDependencies(PATH2).onSuccess(res -> {
      if (doPrint) System.out.println(res.getName() + " -> \t" + packageDepsToMap(res));

      assertNotNull(res);
      assertNotNull(res.getElements(), "Elements should not be null");
      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }

  @Test
  void testProjectDependencies(Vertx vertx, VertxTestContext testContext) {
    final String PATH3 = "test-src/";
    final Map<String, Map<String, List<String>>> expected = Map.ofEntries(
      Map.entry("test-src/nested/sub", Map.of("test-src/nested/sub/DeepClass.java", List.of("java.util.List", "models.Account"))),
      Map.entry("test-src/models", Map.of(
        "test-src/models/User.java", Collections.emptyList(),
        "test-src/models/Account.java", Collections.emptyList())),
      Map.entry("test-src/nested", Collections.emptyMap()),
      Map.entry("test-src/main", Map.of(
        "test-src/main/Integer.java", Collections.emptyList(),
        "test-src/main/App.java", List.of("models.User", "services.UserService", "utils.Logger"),
        "test-src/main/SamePackage.java", Collections.emptyList())),
      Map.entry("test-src/services", Map.of(
        "test-src/services/Logger.java", List.of("java.io.File"),
        "test-src/services/UserService.java", List.of("models.User", "main.utils.Logger"))),
      Map.entry("test-src/main/utils", Map.of(
        "test-src/main/utils/Helper.java", List.of("models.Account"),
        "test-src/main/utils/Logger.java", Collections.emptyList()))
    );
    DependecyAnalyserLib analyser = new DependecyAnalyserLib(vertx);

    analyser.getProjectDependencies(PATH3).onSuccess(res -> {
      if (doPrint) System.out.println(res.getName() + " -> \t" + projectDepsToMap(res));

      assertNotNull(res);
      assertEquals(res.getElements().size(), expected.size());
      assertTrue(res.getElements().stream().map(PackageDepsReport::getName).toList().containsAll(expected.keySet()));
      for (PackageDepsReport report : res.getElements()) {
        var expReport = expected.get(report.getName());
        assertEquals(report.getElements().size(), expReport.size());
        assertTrue(report.getElements().stream().map(ClassDepsReport::getName).toList().containsAll(expReport.keySet()));
        for (ClassDepsReport cl : report.getElements()) {
          var expCl = expReport.get(cl.getName());
          assertEquals(cl.getElements().size(), expCl.size());
          assertTrue(cl.getElements().containsAll(expCl));
        }
      }

      testContext.completeNow();
    }).onFailure(testContext::failNow);
  }
}
