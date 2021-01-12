package org.revapi.quarkus;

import static java.util.Arrays.asList;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.revapi.CompatibilityType;
import org.revapi.DifferenceSeverity;
import org.revapi.Report;
import org.revapi.yaml.YamlDifferenceAnalyzer;
import org.revapi.yaml.YamlElement;

public class QuarkusExtensionDifferenceAnalyzer extends YamlDifferenceAnalyzer {
    private static final Map<Pattern, JsonNodeType> VALID_PROPERTIES = new HashMap<>();
    static {
        VALID_PROPERTIES.put(Pattern.compile(""), JsonNodeType.OBJECT);
        VALID_PROPERTIES.put(Pattern.compile("/name"), JsonNodeType.STRING);
        VALID_PROPERTIES.put(Pattern.compile("/metadata"), JsonNodeType.OBJECT);
        VALID_PROPERTIES.put(Pattern.compile("/metadata/keywords"), JsonNodeType.ARRAY);
        VALID_PROPERTIES.put(Pattern.compile("/metadata/keywords/\\d+"), JsonNodeType.STRING);
        VALID_PROPERTIES.put(Pattern.compile("/metadata/guide"), JsonNodeType.STRING);
        VALID_PROPERTIES.put(Pattern.compile("/metadata/categories"), JsonNodeType.ARRAY);
        VALID_PROPERTIES.put(Pattern.compile("/metadata/categories/\\d+"), JsonNodeType.STRING);
        VALID_PROPERTIES.put(Pattern.compile("/metadata/status"), JsonNodeType.STRING);
    }

    @Override
    protected String valueRemovedCode() {
        return "quarkus-extension.propertyRemoved";
    }

    @Override
    protected String valueAddedCode() {
        return "quarkus-extension.propertyAdded";
    }

    @Override
    protected String valueChangedCode() {
        return "quarkus-extension.propertyChanged";
    }

    @Override
    public Report endAnalysis(YamlElement oldElement, YamlElement newElement) {
        // at least one of the elements is going to be non-null
        YamlElement representative = oldElement == null ? newElement : oldElement;

        String filePath = representative.getFilePath();
        String dataPath = representative.getPath();

        // We only detect a couple of additional conditions on top of the generic yaml analyzer.
        // So let's just detect everything in this method for now. We can abstract things out later on when/if we detect
        // even more problems...

        JsonNodeType expectedType = getExpectedType(dataPath);

        if (expectedType == null) {
            return Report.builder()
                    .withOld(oldElement)
                    .withNew(newElement)
                    .addProblem()
                    .withCode("quarkus-extension.invalidProperty")
                    .withName("Invalid Property")
                    .withDescription("The property is not valid in Quarkus extension descriptor.")
                    .addClassification(CompatibilityType.SEMANTIC, DifferenceSeverity.BREAKING)
                    .addAttachment("file", filePath)
                    .addAttachment("path", dataPath)
                    .withIdentifyingAttachments(asList("file", "path"))
                    .done()
                    .build();
        }

        if (oldElement != null && oldElement.getNode() instanceof JsonNode) {
            JsonNodeType foundType = ((JsonNode) oldElement.getNode()).getNodeType();

            if (foundType != expectedType) {
                return Report.builder()
                        .withOld(oldElement)
                        .withNew(newElement)
                        .addProblem()
                        .withCode("quarkus-extension.invalidOldPropertyType")
                        .withName("Old Property Type Invalid")
                        .withDescription("The property in the old API does not have the expected type.")
                        .addClassification(CompatibilityType.SEMANTIC, DifferenceSeverity.BREAKING)
                        .addAttachment("file", filePath)
                        .addAttachment("path", dataPath)
                        .addAttachment("expectedType", expectedType.name())
                        .addAttachment("actualType", foundType.name())
                        .withIdentifyingAttachments(asList("file", "path"))
                        .done()
                        .build();
            }
        }

        if (newElement != null && newElement.getNode() instanceof JsonNode) {
            JsonNodeType foundType = ((JsonNode) newElement.getNode()).getNodeType();

            if (foundType != expectedType) {
                return Report.builder()
                        .withOld(oldElement)
                        .withNew(newElement)
                        .addProblem()
                        .withCode("quarkus-extension.invalidNewPropertyType")
                        .withName("New Property Type Invalid")
                        .withDescription("The property in the new API does not have the expected type.")
                        .addClassification(CompatibilityType.SEMANTIC, DifferenceSeverity.BREAKING)
                        .addAttachment("file", filePath)
                        .addAttachment("path", dataPath)
                        .addAttachment("expectedType", expectedType.name())
                        .addAttachment("actualType", foundType.name())
                        .withIdentifyingAttachments(asList("file", "path"))
                        .done()
                        .build();
            }
        }

        if ("/metadata/status".equals(dataPath)) {
            String oldValue = oldElement == null ? "" : oldElement.getValueString();
            String newValue = newElement == null ? "" : newElement.getValueString();

            if (!Objects.equals(oldValue, newValue)) {
                return Report.builder()
                        .withOld(oldElement)
                        .withNew(newElement)
                        .addProblem()
                        .withCode("quarkus-extension.statusChanged")
                        .withName("Status Changed")
                        .withDescription("The extension status changed.")
                        .addClassification(CompatibilityType.SEMANTIC, DifferenceSeverity.POTENTIALLY_BREAKING)
                        .addAttachment("file", filePath)
                        .addAttachment("path", "/metadata/status")
                        .addAttachment("oldValue", oldValue)
                        .addAttachment("newValue", newValue)
                        .withIdentifyingAttachments(asList("file", "path"))
                        .done()
                        .build();
            }
        }

        // we didn't detect any Quarkus specific problem, so let's perform the generic analysis...
        return super.endAnalysis(oldElement, newElement);
    }

    private static JsonNodeType getExpectedType(String dataPath) {
        for (Map.Entry<Pattern, JsonNodeType> e : VALID_PROPERTIES.entrySet()) {
            if (e.getKey().matcher(dataPath).matches()) {
                return e.getValue();
            }
        }

        return null;
    }
}
