package pcd.ass02;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import io.reactivex.rxjava3.schedulers.Schedulers;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import com.mxgraph.layout.mxCircleLayout;

public class DependencyAnalyserGUI {
  private JFrame frame;
  private JButton startButton;
  private JTextField folderPathField;
  private JPanel graphPanel;

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new DependencyAnalyserGUI().createAndShowGUI());
  }

  private void createAndShowGUI() {
    frame = new JFrame("Dependency Analyser");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    JPanel topPanel = new JPanel(new BorderLayout());
    folderPathField = new JTextField("test-src");
    JButton browseButton = new JButton("Browse");
    browseButton.addActionListener(e -> selectFolder());
    topPanel.add(folderPathField, BorderLayout.CENTER);
    topPanel.add(browseButton, BorderLayout.EAST);

    startButton = new JButton("Start Analysis");
    startButton.addActionListener(e -> startAnalysis());

    graphPanel = new JPanel(new BorderLayout());
    graphPanel.setBorder(BorderFactory.createTitledBorder("Dependency Graph"));

    JPanel controlPanel = new JPanel(new BorderLayout());
    controlPanel.add(topPanel, BorderLayout.NORTH);
    controlPanel.add(startButton, BorderLayout.CENTER);

    frame.add(controlPanel, BorderLayout.WEST);
    frame.add(graphPanel, BorderLayout.CENTER);

    frame.setSize(1500, 1000);
    frame.setVisible(true);
  }

  private void selectFolder() {
    JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
      folderPathField.setText(chooser.getSelectedFile().getAbsolutePath());
    }
  }

  private void startAnalysis() {
    String folderPath = folderPathField.getText();

    DependecyAnalyserLib.getProjectDependencies(folderPath)
      .subscribeOn(Schedulers.io()) // run heavy analysis on background thread
      .subscribe(
        report -> SwingUtilities.invokeLater(() -> updateGraphRectangle(report)), // onSuccess
        error -> JOptionPane.showMessageDialog(frame, "Error: " + error.getMessage())
      );

//      .onSuccess(report -> {
//        SwingUtilities.invokeLater(() -> {
//          updateGraphRectangle(report);
//        });
//      })
//      .onFailure(err -> {
//          SwingUtilities.invokeLater(() ->
//            JOptionPane.showMessageDialog(frame, "Error: " + err.getMessage()));
//        }
//      );

    // Use CompletableFuture instead of RxJava
//    CompletableFuture.supplyAsync(() -> getProjectDependencies(rootFolder))
//      .thenAccept(report -> {
//        SwingUtilities.invokeLater(() -> {
//          classCountLabel.setText("Classes: " + report.getClassCount());
//          dependencyCountLabel.setText("Dependencies: " + report.getDependencyCount());
//          updateGraphRectangle(report);
//        });
//      })
//      .exceptionally(error -> {
//        SwingUtilities.invokeLater(() ->
//          JOptionPane.showMessageDialog(frame, "Error: " + error.getMessage()));
//        return null;
//      });
  }

  private void updateGraph(ProjectDepsReport report) {
    graphPanel.removeAll();

    // Create a JGraphX graph
    mxGraph graph = new mxGraph();
    Object parent = graph.getDefaultParent();

    Map<String, Map<String, List<String>>> packClassDeps = new HashMap<>();
    for (var packageReport : report.getElements()) {
      Map<String, List<String>> packageMap = new HashMap<>();
      packClassDeps.put(packageReport.getName(), packageMap);
      for (var classReport : packageReport.getElements()) {
        packageMap.put(classReport.getName(), classReport.getElements());
      }
    }

    // Start a batch of edits
    graph.getModel().beginUpdate();
    try {
      // Create vertices for each class
//      Object[] vertices = new Object[reportSize];
//      for (int i = 0; i < reportSize; i++) {
//        // Insert vertex with unique ID, label, x, y, width, height
//        vertices[i] = graph.insertVertex(parent, "class" + i, "Class" + i,
//          0, 0, 100, 40);
//      }
//
//      // Create edges for dependencies
//      for (int i = 0; i < report.getDependencyCount(); i++) {
//        int from = i % reportSize;
//        int to = (i + 1) % reportSize);
//        graph.insertEdge(parent, "edge" + i, "", vertices[from], vertices[to]);
//      }

      // Apply a circle layout
      mxCircleLayout layout = new mxCircleLayout(graph);
      layout.setX0(250);  // Set the center x-coordinate
      layout.setY0(250);  // Set the center y-coordinate
      layout.setRadius(200);  // Set the radius of the circle
      layout.execute(parent);
    } finally {
      // End the batch of edits
      graph.getModel().endUpdate();
    }

    // Create a graph component and add it to the panel
    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    graphPanel.add(graphComponent, BorderLayout.CENTER);
    graphPanel.revalidate();
    graphPanel.repaint();
  }


  private void updateGraphRectangle(ProjectDepsReport report) {
    graphPanel.removeAll();
    mxGraph graph = new mxGraph();
    Object parent = graph.getDefaultParent();

    Map<String, Object> packageVertexMap = new HashMap<>();
    Map<String, Object> classVertexMap = new HashMap<>();

    graph.getModel().beginUpdate();
    try {
      int xOffset = 20;
      int yOffset = 20;
      int packageSpacing = 300;
      int classSpacing = 60;
      int packagePerLine = 3;



      Map<String, Map<String, List<String>>> packClassDeps = new HashMap<>();
      for (var packageReport : report.getElements()) {
        Map<String, List<String>> packageMap = new HashMap<>();
        packClassDeps.put(packageReport.getName(), packageMap);
        for (var classReport : packageReport.getElements()) {
          packageMap.put(classReport.getName(), classReport.getElements());
          for (var dep : classReport.getElements()) {
            Optional<String> depClassName = Optional.empty();
            var splitted = new ArrayList<>(Arrays.stream(dep.split("\\.")).toList());

            if (Character.isUpperCase(splitted.get(splitted.size() - 1).charAt(0))) {
              depClassName = Optional.of(splitted.remove(splitted.size() - 1));
            }
            String packageName = String.join(".", splitted);
            if (!packClassDeps.containsKey(packageName)) {
              packClassDeps.put(packageName, new HashMap<>());
            }
            depClassName.ifPresent(s -> packClassDeps.get(packageName).put(dep, Collections.emptyList()));
          }
        }
      }

      int pIdx = 0;
      for (var entry : packClassDeps.entrySet()) {
        String packageName = entry.getKey();
        Map<String, List<String>> classMap = entry.getValue();

        int classCount = classMap.size();
        int calculatedHeight = Math.max(100, classCount * classSpacing + 40); // Minimum height 100

        // Create package container
        Object packageGroup = graph.insertVertex(
          parent, null, packageName,
          xOffset + (pIdx % packagePerLine) * packageSpacing,
          (pIdx / packagePerLine) * packageSpacing + yOffset,
          250, calculatedHeight,
          "fillColor=none;strokeColor=black;rounded=1"
        );
        packageVertexMap.put(packageName, packageGroup);

        int cIdx = 0;
        for (var e : classMap.entrySet()) {
          String className = e.getKey();
          int classNameWidth = Math.max(60, className.length() * 7 + 20); // Dynamically set width

          Object classVertex = graph.insertVertex(
            packageGroup, null, className,
            20, 20 + cIdx * classSpacing,
            classNameWidth, 30
          );
          classVertexMap.put(className, classVertex);
          cIdx++;
        }

        pIdx++;
      }

      // Add edges based on class dependencies
      for (PackageDepsReport packageReport : report.getElements()) {
        for (ClassDepsReport classReport : packageReport.getElements()) {
          Object fromVertex = classVertexMap.get(classReport.getName());
          if (fromVertex != null) {
            for (String dep : classReport.getElements()) {
              Object toVertex = classVertexMap.get(dep) == null
                ? packageVertexMap.get(dep)
                : classVertexMap.get(dep);
              if (toVertex != null) {
                graph.insertEdge(parent, null, "", fromVertex, toVertex);
              }
            }
          }
        }
      }
    } finally {
      graph.getModel().endUpdate();
    }

    mxGraphComponent graphComponent = new mxGraphComponent(graph);
    graphPanel.add(graphComponent, BorderLayout.CENTER);
    graphPanel.revalidate();
    graphPanel.repaint();
  }
}

//// Simple parser without RxJava
//class DependencyParser {
//  public static DummyProjectDepsReport analyseProject(File folder) {
//    // Here you'd parse files using JavaParser
//    try {
//      // Simulate processing time
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      Thread.currentThread().interrupt();
//    }
//    return new DummyProjectDepsReport(8, 12); // Dummy values for testing
//  }
//}
//
//class DummyProjectDepsReport {
//  private final int classCount;
//  private final int dependencyCount;
//
//  public DummyProjectDepsReport(int classCount, int dependencyCount) {
//    this.classCount = classCount;
//    this.dependencyCount = dependencyCount;
//  }
//
//  public int getClassCount() {
//    return classCount;
//  }
//
//  public int getDependencyCount() {
//    return dependencyCount;
//  }
//
//  @Override
//  public String toString() {
//    return "Classes: " + classCount + ", Dependencies: " + dependencyCount;
//  }
//}

