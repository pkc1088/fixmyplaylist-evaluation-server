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
        String csv = "id,source,target,label,reason\n"
                + "reference-1,Source song,Target song,SUCCESS,Human verified\n"
                + "reference-2,\"Source, Live\",\"Target, Studio\",FAIL,\"Different, version\"\n";

        List<ReferenceCase> cases = readFromTempClasspath("inline-promotion-test.csv", csv);

        assertEquals(2, cases.size());
        assertEquals("reference-1", cases.get(0).getReferenceCaseId());
        assertEquals("Target song", cases.get(0).getTargetTitle());
        assertEquals("Source song", cases.get(0).getSourceTitle());
        assertEquals(EvaluationLabel.SUCCESS, cases.get(0).getHumanLabel());
        assertEquals("Human verified", cases.get(0).getHumanReason());

        assertEquals("reference-2", cases.get(1).getReferenceCaseId());
        assertEquals("Target, Studio", cases.get(1).getTargetTitle());
        assertEquals("Source, Live", cases.get(1).getSourceTitle());
        assertEquals(EvaluationLabel.FAIL, cases.get(1).getHumanLabel());
        assertEquals("Different, version", cases.get(1).getHumanReason());
    }

    @Test
    void returnsEmptyListWhenCsvContainsHeaderOnly() throws Exception {
        List<ReferenceCase> cases = readFromTempClasspath("header-only.csv", "id,source,target,label,reason\n");

        assertTrue(cases.isEmpty());
    }

    @Test
    void rejectsInvalidLabelColumnValue() throws Exception {
        String csv = "id,source,target,label,reason\nreference-1,Source,Target,UNKNOWN,Human verified\n";

        assertThrows(IllegalArgumentException.class, () -> readFromTempClasspath("invalid-label.csv", csv));
    }

    @Test
    void rejectsRowWithMissingColumns() throws Exception {
        String csv = "id,source,target,label,reason\nreference-1,Source,Target,SUCCESS\n";

        assertThrows(ArrayIndexOutOfBoundsException.class, () -> readFromTempClasspath("short-row.csv", csv));
    }

    @Test
    void rejectsRowWithBlankReason() throws Exception {
        String csv = "id,source,target,label,reason\nreference-1,Source,Target,SUCCESS,   \n";

        assertThrows(IllegalArgumentException.class, () -> readFromTempClasspath("blank-reason.csv", csv));
    }

    @Test
    void missingClasspathFileFailsAsIoException() {
        CsvReaderAdapter adapter = new CsvReaderAdapter("", "missing-promotion-test.csv");

        assertThrows(java.io.IOException.class, adapter::read);
    }

    private List<ReferenceCase> readFromTempClasspath(String fileName, String csvContent) throws Exception {
        Files.writeString(tempDirectory.resolve(fileName), csvContent);
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try (URLClassLoader testClassLoader = new URLClassLoader(
                new java.net.URL[]{tempDirectory.toUri().toURL()}, originalClassLoader)) {
            Thread.currentThread().setContextClassLoader(testClassLoader);
            return new CsvReaderAdapter("", fileName).read();
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
