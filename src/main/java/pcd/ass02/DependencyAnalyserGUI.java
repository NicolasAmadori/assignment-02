package pcd.ass02;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
  private JTextField folderPathField;
  private JPanel graphPanel;
  private JTextField classCountField;
  private JTextField dependencyCountField;
  ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<String>>> packClassDeps = new ConcurrentHashMap<>();
  private Map<String, Object> packageVertexMap;
  private Map<String, Object> classVertexMap;
  private Map<String, Map<String, Object>> edgeVertexMap;
  private mxGraph graph = new mxGraph();
  private int analysedClassesCounter;

  public static void main(String[] args) {
    new DependencyAnalyserGUI().createAndShowGUI();
  }

  private void createAndShowGUI() {
    frame = new JFrame("Dependency Analyser");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    JPanel controlPanel = new JPanel(new BorderLayout());

    JPanel topRowPanel = new JPanel(new BorderLayout());
    folderPathField = new JTextField("test-src");
    folderPathField.setPreferredSize(new Dimension(600, 30)); // Make it wider

    JButton browseButton = new JButton("Browse");
    browseButton.setPreferredSize(new Dimension(100, 30));
    browseButton.addActionListener(e -> selectFolder());

    JButton startButton = new JButton("Start Analysis");
    startButton.setPreferredSize(new Dimension(150, 30));
    startButton.addActionListener(e -> startAnalysis());

    topRowPanel.add(browseButton, BorderLayout.WEST);
    topRowPanel.add(folderPathField, BorderLayout.CENTER);
    topRowPanel.add(startButton, BorderLayout.EAST);

    controlPanel.add(topRowPanel, BorderLayout.NORTH);

    graphPanel = new JPanel(new BorderLayout());
    graphPanel.setBorder(BorderFactory.createTitledBorder("Dependency Graph"));

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    classCountField = new JTextField("Classes/Interfaces: 0");
    classCountField.setEditable(false);
    classCountField.setPreferredSize(new Dimension(200, 30));

    dependencyCountField = new JTextField("Dependencies: 0");
    dependencyCountField.setEditable(false);
    dependencyCountField.setPreferredSize(new Dimension(200, 30));

    bottomPanel.add(classCountField);
    bottomPanel.add(dependencyCountField);

    frame.add(bottomPanel, BorderLayout.SOUTH);


    frame.add(controlPanel, BorderLayout.NORTH);
    frame.add(graphPanel, BorderLayout.CENTER);

    frame.setSize(1800, 1000);
    frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Fullscreen
    frame.setUndecorated(false); // Keep title bar (set true for borderless fullscreen)
    frame.setResizable(false); // Disable resizing
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
    packClassDeps = new ConcurrentHashMap<>();
    packageVertexMap = new HashMap<>();
    classVertexMap = new HashMap<>();
    edgeVertexMap = new HashMap<>();
    graph = new mxGraph();
    graph.setAutoSizeCells(true);
    graph.setCellsMovable(true); // false, if you want to lock
    graph.setAllowDanglingEdges(false);
    graph.setCellsResizable(false);
    String folderPath = folderPathField.getText();
    analysedClassesCounter = 0;

    graphPanel.removeAll();
    graphPanel.revalidate();

    DependecyAnalyserLib.getProjectDependencies(folderPath).getElements()
      .subscribeOn(Schedulers.io()) // run heavy analysis on background thread
      .subscribe(
        packageDepsReport -> SwingUtilities.invokeLater(() -> updatePackage(packageDepsReport)), // onSuccess
        error -> SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Error: " + error))
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

  private void drawClass(Object parent, String className, int classCounter) {
    int classNameWidth = Math.max(60, className.length() * 7 + 20);

    Object classVertex = graph.insertVertex(
      parent, null, className,
      20, 20 + classCounter * CLASS_SPACING,
      classNameWidth, 30
    );
    classVertexMap.put(className, classVertex);
  }

  private void updateGraph(final Map<String, Map<String, List<String>>> packClassDeps, final int analysedClassesCounter) {
    try {
      Object parent = graph.getDefaultParent();
      graph.getModel().beginUpdate();

      packClassDeps.keySet()
        .stream()
        .filter(packageName -> !packageVertexMap.containsKey(packageName))
        .forEach( packageName -> drawPackage(parent, packageName));

      for (var p : packClassDeps.entrySet()) {
        String packageName = p.getKey();
        Set<String> totalClass = p.getValue().keySet();

        for (var c : p.getValue().entrySet()) {
          String className = c.getKey();
          if (classVertexMap.containsKey(className)) {
            continue;
          }
          int classCounter = (int) totalClass.stream().filter(classVertexMap::containsKey).count();
          drawClass(packageVertexMap.get(packageName), className, classCounter);
        }
      }

      for (var p : packClassDeps.entrySet()) {
        String packageName = p.getKey();
        Set<String> totalClass = p.getValue().keySet();

        for (var c : p.getValue().entrySet()) {
          String className = c.getKey();
          Object fromVertex = classVertexMap.get(className);
          if (fromVertex == null) {
            System.out.println("Class " + className + " not found. Creating");
            int classCounter = (int) totalClass.stream().filter(classVertexMap::containsKey).count();
            drawClass(packageVertexMap.get(packageName), className, classCounter);
            fromVertex = classVertexMap.get(className);
          }

          if (!edgeVertexMap.containsKey(className)) {
            edgeVertexMap.put(className, new HashMap<>());
          }

          for (String to : c.getValue()) {
            if (edgeVertexMap.get(className).containsKey(to)) {
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
      graphPanel.revalidate();
      graphPanel.repaint();
    } finally {
      int totalDeps = edgeVertexMap.values().stream().mapToInt(Map::size).sum();

      classCountField.setText("Classes/Interfaces: " + analysedClassesCounter);
      dependencyCountField.setText("Dependencies: " + totalDeps);
      graph.getModel().endUpdate();
    }

  }

  private void updatePackage(PackageDepsReport packageDepsReport) {
    final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> packageMap =
      packClassDeps.computeIfAbsent(packageDepsReport.getName(), k -> new ConcurrentHashMap<>());

    packageDepsReport.getElements()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.single())
      .subscribe(
        classDepsReport -> {
          updateClass(classDepsReport, packageMap);
          var packClassDepsCopy = deepImmutableCopy(this.packClassDeps);
          var temp = analysedClassesCounter;
          SwingUtilities.invokeLater(() -> this.updateGraph(packClassDepsCopy, temp));
        },
        error -> SwingUtilities.invokeLater(() ->
          JOptionPane.showMessageDialog(frame, "Error: " + error))
      );
  }

  private void updateClass(ClassDepsReport classDepsReport,
                           ConcurrentHashMap<String, CopyOnWriteArrayList<String>> packageMap) {
    analysedClassesCounter++;
    CopyOnWriteArrayList<String> deps = packageMap.computeIfAbsent(classDepsReport.getName(), k -> new CopyOnWriteArrayList<>());

    classDepsReport.getElements()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.single())
      .subscribe(
        dep -> {
          updateDependency(dep, deps);
          var packClassDepsCopy = deepImmutableCopy(this.packClassDeps);
          var temp = analysedClassesCounter;
          SwingUtilities.invokeLater(() -> this.updateGraph(packClassDepsCopy, temp));
        },
        error -> SwingUtilities.invokeLater(() ->
          JOptionPane.showMessageDialog(frame, "Error: " + error))
      );
  }

  private void updateDependency(String dep, CopyOnWriteArrayList<String> deps) {
    if (!deps.contains(dep)) {
      deps.add(dep);
    }
    boolean isClass = false;
    var split = new ArrayList<>(Arrays.asList(dep.split("\\.")));
    if (!split.isEmpty() && Character.isUpperCase(split.get(split.size() - 1).charAt(0))) {
      split.remove(split.size() - 1);
      isClass = true;
    }
    String packageName = String.join(".", split);

    ConcurrentHashMap<String, CopyOnWriteArrayList<String>> targetPackage =
      packClassDeps.computeIfAbsent(packageName, k -> new ConcurrentHashMap<>());

    if (isClass) {
      targetPackage.putIfAbsent(dep, new CopyOnWriteArrayList<>());
    }
  }

  private Map<String, Map<String, List<String>>> deepImmutableCopy(
    ConcurrentHashMap<String, ConcurrentHashMap<String, CopyOnWriteArrayList<String>>> original) {
    Map<String, Map<String, List<String>>> copy = new HashMap<>();

    for (Map.Entry<String, ConcurrentHashMap<String, CopyOnWriteArrayList<String>>> pkgEntry : original.entrySet()) {
      Map<String, List<String>> classMap = new HashMap<>();
      for (Map.Entry<String, CopyOnWriteArrayList<String>> classEntry : pkgEntry.getValue().entrySet()) {
        classMap.put(classEntry.getKey(), List.copyOf(classEntry.getValue()));
      }
      copy.put(pkgEntry.getKey(), Collections.unmodifiableMap(classMap));
    }

    return Collections.unmodifiableMap(copy);
  }



}

