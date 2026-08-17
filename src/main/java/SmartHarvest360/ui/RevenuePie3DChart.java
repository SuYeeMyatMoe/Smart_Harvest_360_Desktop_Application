package SmartHarvest360.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import java.util.LinkedHashMap;
import java.util.Map;

/** True extruded 3D revenue pie chart rendered with JavaFX TriangleMesh. */
public final class RevenuePie3DChart extends VBox {
    private static final Color[] COLORS = {
            Color.web("#239467"), Color.web("#e3a54d"), Color.web("#54a9a0"),
            Color.web("#93b35f"), Color.web("#7e75b7")
    };
    private final Group slices = new Group();
    private final Group world = new Group();
    private final FlowPane legend = new FlowPane(12, 7);
    private final Rotate rotateY = new Rotate(-10, Rotate.Y_AXIS);
    private double dragX;
    private double startAngle;

    public RevenuePie3DChart() {
        setAlignment(Pos.CENTER);
        setSpacing(4);
        setPadding(new Insets(2, 4, 4, 4));
        getStyleClass().add("report-3d-chart");

        world.getTransforms().addAll(new Rotate(-18, Rotate.X_AXIS), rotateY);
        world.getChildren().addAll(slices, new AmbientLight(Color.color(0.62, 0.68, 0.65)),
                light(Color.WHITE, -160, -180, -260));
        SubScene scene3d = new SubScene(world, 430, 245, true, SceneAntialiasing.BALANCED);
        scene3d.setFill(Color.TRANSPARENT);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-520);
        camera.setNearClip(0.1);
        camera.setFarClip(1500);
        camera.setFieldOfView(34);
        scene3d.setCamera(camera);
        scene3d.setOnMousePressed(this::startDrag);
        scene3d.setOnMouseDragged(this::drag);

        legend.setAlignment(Pos.CENTER);
        getChildren().addAll(scene3d, legend);
        setData(Map.of("No sales", 1.0));
    }

    public void setData(Map<String, Double> values) {
        Map<String, Double> data = values == null || values.isEmpty()
                ? Map.of("No sales", 1.0) : new LinkedHashMap<>(values);
        slices.getChildren().clear();
        legend.getChildren().clear();
        double total = data.values().stream().mapToDouble(value -> Math.max(0, value)).sum();
        if (total <= 0) total = 1;
        double start = -90;
        int index = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double sweep = Math.max(1.5, Math.max(0, entry.getValue()) / total * 360.0);
            Color color = COLORS[index % COLORS.length];
            MeshView wedge = new MeshView(createWedge(start, sweep, 90, 18));
            PhongMaterial material = new PhongMaterial(color);
            material.setSpecularColor(color.brighter());
            material.setSpecularPower(28);
            wedge.setMaterial(material);
            wedge.setOnMouseEntered(event -> wedge.setTranslateZ(-8));
            wedge.setOnMouseExited(event -> wedge.setTranslateZ(0));
            slices.getChildren().add(wedge);

            Label item = new Label("●  " + entry.getKey() + "  "
                    + String.format("%.0f%%", entry.getValue() / total * 100));
            item.setTextFill(color);
            item.getStyleClass().add("chart-3d-legend");
            legend.getChildren().add(item);
            start += sweep;
            index++;
        }
    }

    private TriangleMesh createWedge(double startDeg, double sweepDeg, double radius, double depth) {
        int steps = Math.max(3, (int) Math.ceil(sweepDeg / 7.0));
        TriangleMesh mesh = new TriangleMesh();
        float front = (float) (-depth / 2);
        float back = (float) (depth / 2);
        mesh.getPoints().addAll(0, 0, front, 0, 0, back);
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(startDeg + sweepDeg * i / steps);
            float x = (float) (Math.cos(a) * radius);
            float y = (float) (Math.sin(a) * radius);
            mesh.getPoints().addAll(x, y, front, x, y, back);
        }
        mesh.getTexCoords().addAll(0, 0);
        for (int i = 0; i < steps; i++) {
            int f1 = 2 + i * 2, b1 = f1 + 1, f2 = f1 + 2, b2 = f2 + 1;
            mesh.getFaces().addAll(0,0,f1,0,f2,0, 1,0,b2,0,b1,0);
            mesh.getFaces().addAll(f1,0,b1,0,b2,0, f1,0,b2,0,f2,0);
        }
        int firstF = 2, firstB = 3, lastF = 2 + steps * 2, lastB = lastF + 1;
        mesh.getFaces().addAll(0,0,firstB,0,firstF,0, 0,0,lastF,0,lastB,0);
        return mesh;
    }

    private void startDrag(MouseEvent event) { dragX = event.getSceneX(); startAngle = rotateY.getAngle(); }
    private void drag(MouseEvent event) { rotateY.setAngle(startAngle + (event.getSceneX() - dragX) * 0.45); }
    private PointLight light(Color color, double x, double y, double z) {
        PointLight light = new PointLight(color);
        light.setTranslateX(x); light.setTranslateY(y); light.setTranslateZ(z);
        return light;
    }
}
