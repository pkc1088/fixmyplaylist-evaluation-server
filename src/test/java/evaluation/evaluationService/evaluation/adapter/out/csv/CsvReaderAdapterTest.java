package evaluation.evaluationService.evaluation.adapter.out.csv;

import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class CsvReaderAdapterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void readsClasspathCsvAndMapsSourceBeforeTargetColumns() throws Exception {
        String csv = "id,source,target,label,reason\n" + "reference-1,Source song,Target song,SUCCESS,Human verified\n";
        Files.writeString(tempDirectory.resolve("inline-promotion-test.csv"), csv);
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader testClassLoader = new URLClassLoader(
                new java.net.URL[]{tempDirectory.toUri().toURL()}, originalClassLoader)
        ) {
            Thread.currentThread().setContextClassLoader(testClassLoader);
            List<ReferenceCase> cases = new CsvReaderAdapter("", "inline-promotion-test.csv").read();

            assertEquals(1, cases.size());
            assertEquals("reference-1", cases.get(0).getReferenceCaseId());
            assertEquals("Target song", cases.get(0).getTargetTitle());
            assertEquals("Source song", cases.get(0).getSourceTitle());
            assertEquals(EvaluationLabel.SUCCESS, cases.get(0).getHumanLabel());
            assertEquals("Human verified", cases.get(0).getHumanReason());
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    @Test
    void missingClasspathFileFailsAsIoException() {
        CsvReaderAdapter adapter = new CsvReaderAdapter("", "missing-promotion-test.csv");

        assertThrows(java.io.IOException.class, adapter::read);
    }
}
