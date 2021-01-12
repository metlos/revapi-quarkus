package org.revapi.quarkus;

import java.io.Reader;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.revapi.AnalysisContext;
import org.revapi.ArchiveAnalyzer;
import org.revapi.DifferenceAnalyzer;
import org.revapi.yaml.YamlApiAnalyzer;
import org.revapi.yaml.YamlElement;

public class QuarkusExtensionAnalyzer extends YamlApiAnalyzer {
    @Override
    public String getExtensionId() {
        return "quarkus.extension";
    }

    @Override
    public Reader getJSONSchema() {
        return null;
    }

    @Override
    public void initialize(AnalysisContext analysisContext) {
        // our superclass accepts configuration that we want to just hardcode for the quarkus extension, because the
        // location of the descriptor is fixed.
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        config.put("pathRegex", "META-INF/quarkus-extension\\.ya?ml");

        super.initialize(analysisContext.copyWithConfiguration(config));
    }

    public DifferenceAnalyzer<YamlElement> getDifferenceAnalyzer(ArchiveAnalyzer<YamlElement> oldArchive, ArchiveAnalyzer<YamlElement> newArchive) {
        return new QuarkusExtensionDifferenceAnalyzer();
    }
}
