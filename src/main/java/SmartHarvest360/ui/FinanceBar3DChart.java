package SmartHarvest360.ui;

import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;

import java.util.Locale;

/** Perspective 3D financial columns with values and interactive rotation. */
public final class FinanceBar3DChart extends VBox {
    private final Group bars = new Group();
    private final Group world = new Group();
    private final HBox values = new HBox(22);
    private final Rotate rotateY = new Rotate(-12, Rotate.Y_AXIS);
    private double dragX;
    private double startAngle;

    public FinanceBar3DChart() {
        setAlignment(Pos.CENTER);
        setSpacing(3);
        getStyleClass().add("report-3d-chart");
        world.getTransforms().addAll(new Rotate(-7, Rotate.X_AXIS), rotateY);
        world.getChildren().addAll(bars, new AmbientLight(Color.color(0.62, 0.67, 0.65)),
                light(Color.WHITE, -170, -180, -250));
        SubScene scene3d = new SubScene(world, 470, 245, true, SceneAntialiasing.BALANCED);
        scene3d.setFill(Color.TRANSPARENT);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-620);
        camera.setTranslateY(-10);
        camera.setNearClip(0.1);
        camera.setFarClip(1800);
        camera.setFieldOfView(31);
        scene3d.setCamera(camera);
        scene3d.setOnMousePressed(event -> { dragX = event.getSceneX(); startAngle = rotateY.getAngle(); });
        scene3d.setOnMouseDragged(event -> rotateY.setAngle(startAngle + (event.getSceneX() - dragX) * 0.35));
        values.setAlignment(Pos.CENTER);
        getChildren().addAll(scene3d, values);
        setValues(0, 0, 0);
    }

    public void setValues(double revenue, double cost, double profit) {
        bars.getChildren().clear();
        values.getChildren().clear();
        double[] data = {Math.max(0, revenue), Math.max(0, cost), Math.abs(profit)};
        String[] names = {"Revenue", "Cost", profit >= 0 ? "Profit" : "Loss"};
        Color[] colors = {Color.web("#239467"), Color.web("#e3a54d"), Color.web("#54a9a0")};
        double max = Math.max(1, Math.max(data[0], Math.max(data[1], data[2])));
        Box floor = new Box(390, 8, 130);
        floor.setTranslateY(78);
        floor.setMaterial(material(Color.web("#dce9e2")));
        bars.getChildren().add(floor);
        for (int i = 0; i < 3; i++) {
            double height = 26 + data[i] / max * 150;
            Box bar = new Box(72, height, 72);
            bar.setTranslateX((i - 1) * 120);
            bar.setTranslateY(74 - height / 2);
            bar.setMaterial(material(colors[i]));
            bar.setOnMouseEntered(event -> bar.setScaleX(1.07));
            bar.setOnMouseExited(event -> bar.setScaleX(1.0));
            bars.getChildren().add(bar);
            Label label = new Label(names[i] + "\n" + String.format(Locale.US, "RM %,.0f", data[i]));
            label.setAlignment(Pos.CENTER);
            label.setTextFill(colors[i].darker());
            label.getStyleClass().add("chart-3d-value");
            values.getChildren().add(label);
        }
    }

    private PhongMaterial material(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(color.brighter());
        material.setSpecularPower(30);
        return material;
    }
    private PointLight light(Color color, double x, double y, double z) {
        PointLight light = new PointLight(color);
        light.setTranslateX(x); light.setTranslateY(y); light.setTranslateZ(z);
        return light;
    }
}
