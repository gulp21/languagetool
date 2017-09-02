package org.languagetool.rules.de.neuralnetwork;

import org.languagetool.JLanguageTool;
import org.languagetool.databroker.ResourceDataBroker;
import org.languagetool.rules.Example;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class DaDasRule extends GermanNeuralNetworkRule {
    private final List<String> subjects = Arrays.asList("da", "das");

    public DaDasRule(ResourceBundle messages) {
        super(messages);

        addExamplePair(Example.wrong("Danke für <marker>da</marker> Angebot."),
                Example.fixed("Danke für <marker>das</marker> Angebot."));
        addExamplePair(Example.wrong("Wie kommt man denn <marker>das</marker> hin?"),
                Example.fixed("Wie kommt man denn <marker>da</marker> hin?"));

        ResourceDataBroker dataBroker = JLanguageTool.getDataBroker();
        final InputStream dictionaryPath = dataBroker.getFromResourceDirAsStream("/de/dictionary.txt");
        final InputStream embeddingsPath = dataBroker.getFromResourceDirAsStream("/de/final_embeddings.txt");
        final InputStream WPath = dataBroker.getFromResourceDirAsStream("/de/W_fc1.txt");
        final InputStream bPath = dataBroker.getFromResourceDirAsStream("/de/b_fc1.txt");
        classifier = new Classifier(dictionaryPath, embeddingsPath, WPath, bPath);
    }

    @Override
    public String getId() {
        return "DE_DA_VS_DAS";
    }

    @Override
    List<String> getSubjects() {
        return subjects;
    }
}