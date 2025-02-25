package com.myProject.spamDetector;

import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import java.util.ArrayList;

public class SpamClassifier {
    private FilteredClassifier classifier;
    private Instances datasetStructure;

    public SpamClassifier() {
        try {
            classifier = (FilteredClassifier) SerializationHelper.read(
                getClass().getClassLoader().getResourceAsStream("spam_model.model"));
            
            // Manually reconstruct the dataset structure
            ArrayList<Attribute> attributes = new ArrayList<>();
            attributes.add(new Attribute("text", (ArrayList<String>) null));
            attributes.add(new Attribute("class", new ArrayList<String>() {{
                add("spam");
                add("ham");
            }}));
            
            datasetStructure = new Instances("Prediction", attributes, 0);
            datasetStructure.setClassIndex(1);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load model", e);
        }
    }

    public boolean isSpam(String emailText) throws Exception {

        DenseInstance instance = new DenseInstance(2);
        instance.setValue(datasetStructure.attribute(0), emailText);
        instance.setDataset(datasetStructure);
        
        double prediction = classifier.classifyInstance(instance);
        return datasetStructure.classAttribute().value((int) prediction).equals("spam");
    }
}