package com.myProject.spamDetector;

import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class SpamDetectorTrainer {

    public static void main(String[] args) throws Exception {
        // Create attribute list
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("text", (ArrayList<String>) null)); // Text attribute
        attributes.add(new Attribute("label", new ArrayList<String>() {{ // Renamed from "class" to "label"
            add("spam");
            add("ham");
        }})); // Class attribute

        // Create dataset
        Instances dataset = new Instances("SpamDataset", attributes, 0);
        dataset.setClassIndex(dataset.numAttributes() - 1);

        // Read CSV file line by line
        BufferedReader br = new BufferedReader(new FileReader("emails.csv"));
        String line;
        boolean firstLine = true;

        while ((line = br.readLine()) != null) {
            if (firstLine) { // Skip header
                firstLine = false;
                continue;
            }

            // Split by last comma to separate text and spam_or_not
            int lastCommaIndex = line.lastIndexOf(',');
            if (lastCommaIndex == -1) continue; // Skip invalid lines

            String text = line.substring(0, lastCommaIndex).trim();  // All before last comma is text
            String spamLabel = line.substring(lastCommaIndex + 1).trim().equals("1") ? "spam" : "ham"; // Last value is spam/ham

            addInstance(dataset, text, spamLabel);
        }
        br.close();

        // Create classifier pipeline
        StringToWordVector filter = new StringToWordVector();
        filter.setLowerCaseTokens(true);
        filter.setWordsToKeep(1000);

        FilteredClassifier classifier = new FilteredClassifier();
        classifier.setFilter(filter);
        classifier.setClassifier(new NaiveBayes());

        // Train model
        classifier.buildClassifier(dataset);

        // Save model
        SerializationHelper.write("src/main/resources/spam_model.model", classifier);
        System.out.println("Model trained and saved successfully!");
    }

    private static void addInstance(Instances data, String text, String cls) {
        DenseInstance instance = new DenseInstance(2);
        instance.setValue(data.attribute("text"), text);
        instance.setValue(data.attribute("label"), cls); // Updated to "label"
        data.add(instance);
    }
}
