package SmartHarvest360.ml;

import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads ARFF from classpath, trains J48, and caches models under data/ml/.
 */
public final class ModelTrainer {
    private static final Path MODEL_DIR = Path.of("data", "ml");

    private ModelTrainer() {
    }

    public static Classifier loadOrTrain(String resourceArff, String modelFileName) throws Exception {
        Files.createDirectories(MODEL_DIR);
        Path modelPath = MODEL_DIR.resolve(modelFileName);
        Instances data = loadArff(resourceArff);
        data.setClassIndex(data.numAttributes() - 1);

        if (Files.isRegularFile(modelPath)) {
            try {
                Object loaded = SerializationHelper.read(modelPath.toString());
                if (loaded instanceof Classifier classifier) {
                    // Keep cache only when instance count still matches current ARFF.
                    Path meta = MODEL_DIR.resolve(modelFileName + ".meta");
                    String expected = Integer.toString(data.numInstances());
                    if (Files.isRegularFile(meta) && expected.equals(Files.readString(meta).trim())) {
                        return classifier;
                    }
                }
            } catch (Exception ignored) {
                // Retrain if cache is corrupt or outdated.
            }
        }

        J48 tree = new J48();
        tree.buildClassifier(data);
        SerializationHelper.write(modelPath.toString(), tree);
        Files.writeString(MODEL_DIR.resolve(modelFileName + ".meta"),
                Integer.toString(data.numInstances()));
        return tree;
    }

    public static Instances loadArff(String resourceArff) throws Exception {
        String path = resourceArff.startsWith("/") ? resourceArff : "/ml/" + resourceArff;
        try (InputStream in = ModelTrainer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing ARFF resource: " + path);
            }
            DataSource source = new DataSource(in);
            Instances data = source.getDataSet();
            if (data == null || data.numInstances() == 0) {
                throw new IllegalStateException("Empty ARFF: " + path);
            }
            return data;
        }
    }

    public static Path modelDirectory() {
        return MODEL_DIR;
    }
}
