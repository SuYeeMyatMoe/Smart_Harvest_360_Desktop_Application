package SmartHarvest360.ui;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.AmbientLight;
import javafx.scene.Camera;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lightweight, dependency-free JavaFX 3D crop visualizer. */
public final class Crop3DView extends StackPane {
    private static final double WIDTH = 266;
    private static final double HEIGHT = 230;

    private final Group world = new Group();
    private final Group plant = new Group();
    private final Group leaves = new Group();
    private final Group fruit = new Group();
    private final Cylinder stem = new Cylinder(5, 118, 16);
    private final Sphere seed = new Sphere(8, 20);
    private final List<Sphere> leafNodes = new ArrayList<>();
    private final Rotate worldRotation = new Rotate(-10, Rotate.X_AXIS);
    private double dragStartX;
    private double dragStartAngle;
    private String cropName = "Paddy";

    public Crop3DView() {
        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);
        setDepthTest(DepthTest.ENABLE);

        buildWorld();
        SubScene subScene = new SubScene(world, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.TRANSPARENT);
        subScene.setCamera(createCamera());
        getChildren().add(subScene);

        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            dragStartX = event.getSceneX();
            dragStartAngle = worldRotation.getAngle();
        });
        addEventHandler(MouseEvent.MOUSE_DRAGGED,
                event -> worldRotation.setAngle(dragStartAngle + (event.getSceneX() - dragStartX) * 0.35));

        RotateTransition idleTurn = new RotateTransition(Duration.seconds(18), plant);
        idleTurn.setAxis(Rotate.Y_AXIS);
        idleTurn.setByAngle(360);
        idleTurn.setCycleCount(RotateTransition.INDEFINITE);
        idleTurn.setInterpolator(Interpolator.LINEAR);
        idleTurn.play();

        setCrop("Paddy");
        setGrowth(0.08, false);
    }

    public void setCrop(String name) {
        cropName = name == null ? "Paddy" : name;
        rebuildCropDetails();
    }

    public void setGrowth(double progress, boolean animate) {
        double growth = Math.max(0.06, Math.min(1.0, progress));
        double targetXz = 0.30 + growth * 0.70;
        double targetY = 0.18 + growth * 0.82;
        if (animate) {
            ScaleTransition transition = new ScaleTransition(Duration.millis(520), plant);
            transition.setToX(targetXz);
            transition.setToY(targetY);
            transition.setToZ(targetXz);
            transition.setInterpolator(Interpolator.EASE_BOTH);
            transition.play();
        } else {
            plant.setScaleX(targetXz);
            plant.setScaleY(targetY);
            plant.setScaleZ(targetXz);
        }
        leaves.setVisible(growth >= 0.14);
        fruit.setVisible(growth >= 0.72);
        fruit.setOpacity(Math.max(0, (growth - 0.68) / 0.32));
        seed.setVisible(growth < 0.14);
    }

    private void buildWorld() {
        world.getTransforms().add(worldRotation);
        // With a fixed-eye camera, (0, 0) is the visual center of the SubScene.
        world.setTranslateX(0);
        world.setTranslateY(8);

        Cylinder soil = new Cylinder(96, 14, 48);
        soil.setTranslateY(73);
        soil.setMaterial(material("#765233", "#b88956"));

        Cylinder soilGlow = new Cylinder(100, 2, 48);
        soilGlow.setTranslateY(65);
        soilGlow.setMaterial(material("#2d7655", "#77d0a2"));

        stem.setTranslateY(6);
        stem.setMaterial(material("#39754d", "#80c792"));
        seed.setScaleX(1.35);
        seed.setScaleY(0.72);
        seed.setScaleZ(0.88);
        seed.setTranslateY(57);
        seed.setMaterial(material("#c89342", "#f2d18b"));
        plant.getChildren().addAll(stem, leaves, fruit);
        world.getChildren().addAll(soil, soilGlow, seed, plant,
                new AmbientLight(Color.color(0.58, 0.68, 0.62)),
                pointLight(Color.web("#ffe2a1"), -120, -160, -180),
                pointLight(Color.web("#66d6b0"), 150, -20, -80));
    }

    private Camera createCamera() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(1200);
        camera.setTranslateZ(-430);
        camera.setFieldOfView(34);
        return camera;
    }

    private void rebuildCropDetails() {
        leaves.getChildren().clear();
        leafNodes.clear();
        fruit.getChildren().clear();

        String normalized = cropName.toLowerCase(Locale.ROOT);
        if (normalized.contains("papaya")) {
            buildPapaya();
        } else if (normalized.contains("durian")) {
            buildDurian();
        } else {
            buildPaddy();
        }
    }

    private void buildPapaya() {
        stem.setRadius(5.5);
        stem.setMaterial(material("#6f8b4b", "#b6c979"));
        for (int i = 0; i < 8; i++) {
            double angle = i * 45.0;
            Sphere leaf = ellipsoid(34, 4.2, 12, "#2d9157");
            placeRadial(leaf, angle, 28, -57 + (i % 2) * 7);
            leaf.setRotationAxis(Rotate.Z_AXIS);
            leaf.setRotate(angle);
            leaves.getChildren().add(leaf);
        }
        for (int i = 0; i < 5; i++) {
            Sphere papaya = ellipsoid(5.5, 10, 5.5, i % 2 == 0 ? "#f5a33b" : "#e9c13f");
            placeRadial(papaya, i * 72, 9, -34 + (i % 2) * 9);
            fruit.getChildren().add(papaya);
        }
    }

    private void buildDurian() {
        stem.setRadius(6.5);
        stem.setMaterial(material("#6a5838", "#9c8152"));
        for (int i = 0; i < 11; i++) {
            double angle = i * (360.0 / 11.0);
            Sphere leaf = ellipsoid(31, 5, 13, i % 2 == 0 ? "#287a4c" : "#3d9861");
            placeRadial(leaf, angle, 35, -35 - (i % 3) * 15);
            leaves.getChildren().add(leaf);
        }
        for (int i = 0; i < 3; i++) {
            Sphere durian = new Sphere(9, 16);
            durian.setMaterial(material("#7b9b45", "#d1dc79"));
            placeRadial(durian, 70 + i * 120, 18, -18 + i * 13);
            fruit.getChildren().add(durian);
        }
    }

    private void buildPaddy() {
        stem.setRadius(3.5);
        stem.setMaterial(material("#4f8b4c", "#a9c86e"));
        for (int i = 0; i < 10; i++) {
            double angle = i * 36.0;
            Sphere blade = ellipsoid(3.2, 39, 2.2, i % 2 == 0 ? "#4f9a57" : "#6eaa55");
            placeRadial(blade, angle, 16 + (i % 3) * 5, -15 - (i % 2) * 14);
            blade.setRotationAxis(Rotate.Z_AXIS);
            blade.setRotate(i % 2 == 0 ? 16 : -16);
            leaves.getChildren().add(blade);
        }
        for (int i = 0; i < 8; i++) {
            Box grain = new Box(3.5, 6.5, 3.5);
            grain.setMaterial(material("#d8ad45", "#ffe08a"));
            grain.setTranslateX((i - 3.5) * 4.4);
            grain.setTranslateY(-63 + Math.abs(i - 3.5) * 2.2);
            fruit.getChildren().add(grain);
        }
    }

    private Sphere ellipsoid(double x, double y, double z, String color) {
        Sphere sphere = new Sphere(1, 16);
        sphere.setScaleX(x);
        sphere.setScaleY(y);
        sphere.setScaleZ(z);
        sphere.setMaterial(material(color, "#a7e0b4"));
        leafNodes.add(sphere);
        return sphere;
    }

    private void placeRadial(javafx.scene.Node node, double degrees, double radius, double y) {
        double radians = Math.toRadians(degrees);
        node.setTranslateX(Math.cos(radians) * radius);
        node.setTranslateZ(Math.sin(radians) * radius);
        node.setTranslateY(y);
    }

    private PointLight pointLight(Color color, double x, double y, double z) {
        PointLight light = new PointLight(color);
        light.setTranslateX(x);
        light.setTranslateY(y);
        light.setTranslateZ(z);
        return light;
    }

    private PhongMaterial material(String diffuse, String specular) {
        PhongMaterial material = new PhongMaterial(Color.web(diffuse));
        material.setSpecularColor(Color.web(specular));
        material.setSpecularPower(24);
        return material;
    }
}
