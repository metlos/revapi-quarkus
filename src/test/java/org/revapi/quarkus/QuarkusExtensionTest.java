package org.revapi.quarkus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.revapi.API;
import org.revapi.AnalysisContext;
import org.revapi.AnalysisResult;
import org.revapi.Archive;
import org.revapi.PipelineConfiguration;
import org.revapi.Revapi;
import org.revapi.base.CollectingReporter;
import org.revapi.base.InputStreamArchive;

class QuarkusExtensionTest {
    String baseExtension = "" +
            "name: \"Test extension\"\n" +
            "metadata:\n" +
            "  keywords:\n" +
            "  - \"database-connection-pool\"\n" +
            "  - \"datasource\"\n" +
            "  - \"jdbc\"\n" +
            "  categories:\n" +
            "  - \"data\"\n" +
            "  status: \"stable\"";
    String additionsExtension = "" +
            "name: \"Test extension\"\n" +
            "metadata:\n" +
            "  keywords:\n" +
            "  - \"agroal\"\n" +
            "  - \"database-connection-pool\"\n" +
            "  - \"datasource\"\n" +
            "  - \"jdbc\"\n" +
            "  guide: \"https://quarkus.io/guides/datasource\"\n" +
            "  categories:\n" +
            "  - \"data\"\n" +
            "  status: \"stable\"";
    String changesExtension = "" +
            "name: \"Test extension\"\n" +
            "metadata:\n" +
            "  keywords:\n" +
            "  - \"database-connection-pool\"\n" +
            "  - \"datasources\"\n" +
            "  - \"jdbc\"\n" +
            "  categories:\n" +
            "  - \"data\"\n" +
            "  status: \"stable\"";
    String statusChangeExtension = "" +
            "name: \"Test extension\"\n" +
            "metadata:\n" +
            "  keywords:\n" +
            "  - \"database-connection-pool\"\n" +
            "  - \"datasource\"\n" +
            "  - \"jdbc\"\n" +
            "  categories:\n" +
            "  - \"data\"\n" +
            "  status: \"unstable\"";
    String unknownPropertyExtension = "" +
            "name: \"Test extension\"\n" +
            "metadata:\n" +
            "  kachny: true\n" +
            "  keywords:\n" +
            "  - \"database-connection-pool\"\n" +
            "  - \"datasource\"\n" +
            "  - \"jdbc\"\n" +
            "  categories:\n" +
            "  - \"data\"\n" +
            "  status: \"stable\"";
    String wronglyTypedExtension = "" +
            "name: \"Test extension\"\n" +
            "metadata:\n" +
            "  keywords:\n" +
            "  - \"database-connection-pool\"\n" +
            "  - \"datasource\"\n" +
            "  - \"jdbc\"\n" +
            "  categories: true\n" +
            "  status: \"stable\"";

    API baseAPI;
    API additionsAPI;
    API changesAPI;
    API statusChangeAPI;
    API invalidPropertyAPI;
    API wronglyTypedAPI;

    {
        try {
            baseAPI = API.of(quarkusExtension(baseExtension)).build();
            additionsAPI = API.of(quarkusExtension(additionsExtension)).build();
            changesAPI = API.of(quarkusExtension(changesExtension)).build();
            statusChangeAPI = API.of(quarkusExtension(statusChangeExtension)).build();
            invalidPropertyAPI = API.of(quarkusExtension(unknownPropertyExtension)).build();
            wronglyTypedAPI = API.of(quarkusExtension(wronglyTypedExtension)).build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create the test extensions.");
        }
    }

    Revapi revapi = new Revapi(PipelineConfiguration.builder().withAllExtensionsFromThreadContextClassLoader()
            .withReporters(CollectingReporter.class).build());

    static Archive quarkusExtension(String descriptor) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("META-INF/quarkus-extension.yaml"));
            zip.write(descriptor.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        return new InputStreamArchive("api", () -> new ByteArrayInputStream(bytes.toByteArray()));
    }

    @Test
    void testDetectsAdditions() throws Exception {
        try (AnalysisResult res = revapi.analyze(AnalysisContext.builder().withOldAPI(baseAPI).withNewAPI(additionsAPI).build())) {
            res.throwIfFailed();
            CollectingReporter reporter = res.getExtensions().getFirstExtension(CollectingReporter.class, null);

            assertEquals(2, reporter.getReports().size());
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                    && r.getDifferences().get(0).code.equals("quarkus-extension.propertyAdded")
                    && "/metadata/keywords/0".equals(r.getDifferences().get(0).attachments.get("path"))));
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.propertyAdded")
                            && "/metadata/guide".equals(r.getDifferences().get(0).attachments.get("path"))));
        }
    }

    @Test
    void testDetectsRemovals() throws Exception {
        try (AnalysisResult res = revapi.analyze(AnalysisContext.builder().withOldAPI(additionsAPI).withNewAPI(baseAPI).build())) {
            res.throwIfFailed();
            CollectingReporter reporter = res.getExtensions().getFirstExtension(CollectingReporter.class, null);

            assertEquals(2, reporter.getReports().size());
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.propertyRemoved")
                            && "/metadata/keywords/0".equals(r.getDifferences().get(0).attachments.get("path"))));
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.propertyRemoved")
                            && "/metadata/guide".equals(r.getDifferences().get(0).attachments.get("path"))));
        }
    }

    @Test
    void testDetectsChanges() throws Exception {
        try (AnalysisResult res = revapi.analyze(AnalysisContext.builder().withOldAPI(baseAPI).withNewAPI(changesAPI).build())) {
            res.throwIfFailed();
            CollectingReporter reporter = res.getExtensions().getFirstExtension(CollectingReporter.class, null);

            assertEquals(1, reporter.getReports().size());
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.propertyChanged")
                            && "/metadata/keywords/1".equals(r.getDifferences().get(0).attachments.get("path"))));
        }
    }

    @Test
    void testDetectsStatusChanges() throws Exception {
        try (AnalysisResult res = revapi.analyze(AnalysisContext.builder().withOldAPI(baseAPI).withNewAPI(statusChangeAPI).build())) {
            res.throwIfFailed();
            CollectingReporter reporter = res.getExtensions().getFirstExtension(CollectingReporter.class, null);

            assertEquals(1, reporter.getReports().size());
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.statusChanged")
                            && "/metadata/status".equals(r.getDifferences().get(0).attachments.get("path"))));
        }
    }

    @Test
    void testDetectsInvalidProperties() throws Exception {
        try (AnalysisResult res = revapi.analyze(AnalysisContext.builder().withOldAPI(baseAPI).withNewAPI(invalidPropertyAPI).build())) {
            res.throwIfFailed();
            CollectingReporter reporter = res.getExtensions().getFirstExtension(CollectingReporter.class, null);

            assertEquals(1, reporter.getReports().size());
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.invalidProperty")
                            && "/metadata/kachny".equals(r.getDifferences().get(0).attachments.get("path"))));
        }
    }

    @Test
    void testDetectsWronglyTypedProperties() throws Exception {
        try (AnalysisResult res = revapi.analyze(AnalysisContext.builder().withOldAPI(baseAPI).withNewAPI(wronglyTypedAPI).build())) {
            res.throwIfFailed();
            CollectingReporter reporter = res.getExtensions().getFirstExtension(CollectingReporter.class, null);

            assertEquals(2, reporter.getReports().size());
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.propertyRemoved")
                            && "/metadata/categories/0".equals(r.getDifferences().get(0).attachments.get("path"))));
            assertTrue(reporter.getReports().stream()
                    .anyMatch(r -> r.getDifferences().size() == 1
                            && r.getDifferences().get(0).code.equals("quarkus-extension.invalidNewPropertyType")
                            && "/metadata/categories".equals(r.getDifferences().get(0).attachments.get("path"))));
        }
    }
}
