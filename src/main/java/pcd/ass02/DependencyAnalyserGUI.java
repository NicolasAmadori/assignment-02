package pcd.ass02;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import io.reactivex.rxjava3.schedulers.Schedulers;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;

public class DependencyAnalyserGUI {
  int X_OFFSET = 20;
  int Y_OFFSET = 20;
  int PACKAGE_SPACING = 300;
  int CLASS_SPACING = 60;
  int PACKAGES_PER_LINE = 3;

  private JFrame frame;
  private JButton startButton;
  private JTextField folderPathField;
  private JPanel graphPanel;
  private Map<String, Map<String, List<String>>> packClassDeps = new HashMap<>();
  private Map<String, Object> packageVertexMap = new HashMap<>();
  private Map<String, Object> classVertexMap = new HashMap<>();
  private Map<String, Map<String, Object>> edgeVertexMap = new HashMap<>();
  private mxGraph graph = new mxGraph();

  public static void main(String[] args) {
    new DependencyAnalyserGUI().createAndShowGUI();
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
    graphPanel.removeAll();

    DependecyAnalyserLib.getProjectDependencies(folderPath).getElements()
      .subscribeOn(Schedulers.io()) // run heavy analysis on background thread
      .subscribe(
        packageDepsReport -> SwingUtilities.invokeLater(() -> updatePackage(packageDepsReport)), // onSuccess
        error -> JOptionPane.showMessageDialog(frame, "Error: " + error.getMessage())
      );
  }

  private void drawPackage(Object parent, String packageName) {
    //PAINT NEW PACKAGE
    int packageNumber = packageVertexMap.size() + 1;
    int calculatedHeight = 100; //Math.max(100, classCount * CLASS_SPACING + 40);

    Object packageGroup = graph.insertVertex(
      parent, null, packageName,
      X_OFFSET + (packageNumber % PACKAGES_PER_LINE) * PACKAGE_SPACING,
      ((double) packageNumber / PACKAGES_PER_LINE) * PACKAGE_SPACING + Y_OFFSET,
      250,
      calculatedHeight,
      "fillColor=none;strokeColor=black;rounded=1"
    );
    packageVertexMap.put(packageName, packageGroup);
  }

  private void updateGraph() {
    try {
      Object parent = graph.getDefaultParent();
      graph.getModel().beginUpdate();

      //Disegno i package nuovi
      packClassDeps.keySet()
        .stream()
        .filter(packageName -> !packageVertexMap.containsKey(packageName))
        .forEach( packageName -> drawPackage(parent, packageName));

      //Disegno le nuove classi
      for (var p : packClassDeps.entrySet()) {
        String packageName = p.getKey();
        Set<String> totalClass = p.getValue().keySet();

        for (var c : p.getValue().entrySet()) {
          String className = c.getKey();
          if (classVertexMap.containsKey(className)) {
            continue;
          }
          int classNameWidth = Math.max(60, className.length() * 7 + 20);
          int classCounter = (int) totalClass.stream().filter(classVertexMap::containsKey).count();

          Object classVertex = graph.insertVertex(
            packageVertexMap.get(packageName), null, className,
            20, 20 + classCounter * CLASS_SPACING,
            classNameWidth, 30
          );
          classVertexMap.put(className, classVertex);
        }
      }

      //Disegno le nuove dependency
      for (var p : packClassDeps.entrySet()) {
        String packageName = p.getKey();

        for (var c : p.getValue().entrySet()) {
          String className = c.getKey();
          Object fromVertex = classVertexMap.get(className);
          if (fromVertex == null) {
            continue;
          }
          if (!edgeVertexMap.containsKey(className)) {
            edgeVertexMap.put(className, new HashMap<>());
          }

          for (String to : c.getValue()) {
            if (edgeVertexMap.containsKey(to)) {
              continue;
            }
            Object toVertex = classVertexMap.get(to) == null
              ? packageVertexMap.get(to)
              : classVertexMap.get(to);
            if (toVertex != null) {
              Object edgeVertex = graph.insertEdge(parent, null, "", fromVertex, toVertex);
              edgeVertexMap.get(className).put(to, edgeVertex);
            }
          }
        }
      }

      mxGraphComponent graphComponent = new mxGraphComponent(graph);
      graphPanel.add(graphComponent, BorderLayout.CENTER);
//      graphPanel.revalidate();
//      graphPanel.repaint();
    } finally {
      graph.getModel().endUpdate();
    }

  }

  private void updatePackage(PackageDepsReport packageDepsReport) {
    final Map<String, List<String>> packageMap = new HashMap<>();
    if (packClassDeps.containsKey(packageDepsReport.getName())) {
      packageMap.putAll(packClassDeps.get(packageDepsReport.getName()));
    }
    packClassDeps.put(packageDepsReport.getName(), packageMap);

    packageDepsReport.getElements().subscribeOn(Schedulers.io()) // run heavy analysis on background thread
      .subscribe(
        classDepsReport -> updateClass(classDepsReport, packageMap), // onSuccess
        error -> JOptionPane.showMessageDialog(frame, "Error: " + error.getMessage()));
    }

  private void updateClass(ClassDepsReport classDepsReport, Map<String, List<String>> packageMap) {
    final List<String> deps = new ArrayList<>();
    if (packageMap.containsKey(classDepsReport.getName())) {
      deps.addAll(packageMap.get(classDepsReport.getName()));
    }
    packageMap.put(classDepsReport.getName(), deps);

    classDepsReport.getElements().subscribeOn(Schedulers.io()) // run heavy analysis on background thread
      .subscribe(
        dep -> updateDependency(dep, deps), // onSuccess
        error -> JOptionPane.showMessageDialog(frame, "Error: " + error.getMessage()));
  }

  private void updateDependency(String dep, List<String> deps) {
    if (!deps.contains(dep)) {
      deps.add(dep);
    }
    Optional<String> depClassName = Optional.empty();
    var split = new ArrayList<>(Arrays.stream(dep.split("\\.")).toList());
    if (Character.isUpperCase(split.get(split.size() - 1).charAt(0))) {
      depClassName = Optional.of(split.remove(split.size() - 1));
    }
    String packageName = String.join(".", split);
    if (!packClassDeps.containsKey(packageName)) {
      packClassDeps.put(packageName, new HashMap<>());
    }
    depClassName.ifPresent(s -> packClassDeps.get(packageName).put(dep, Collections.emptyList()));
    SwingUtilities.invokeLater(this::updateGraph);
  }

}

