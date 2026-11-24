package evaluation.evaluationService.evaluation.application.service;

import com.opencsv.exceptions.CsvException;
import evaluation.evaluationService.evaluation.application.port.out.CommandReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.CsvPort;
import evaluation.evaluationService.evaluation.application.port.out.QueryReferenceCasePort;
import evaluation.evaluationService.evaluation.application.port.out.RetrieveReferenceCasePort;
import evaluation.evaluationService.evaluation.domain.exception.ReferenceCaseSetupException;
import evaluation.evaluationService.evaluation.domain.model.ReferenceCase;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class ReferenceCaseSetupServiceTest {

    @Mock private RetrieveReferenceCasePort retrieveReferenceCasePort;
    @Mock private CommandReferenceCasePort commandReferenceCasePort;
    @Mock private QueryReferenceCasePort queryReferenceCasePort;
    @Mock private CsvPort csvPort;
    @InjectMocks private ReferenceCaseSetupService service;

    @Test
    void initializesDatabaseBeforeIndexingCsvCases() throws Exception {
        List<ReferenceCase> cases = List.of(reference("1"), reference("2"));

        when(csvPort.read())
                .thenReturn(cases);

        service.loadCsvAndInitialize();

        InOrder order = inOrder(commandReferenceCasePort, retrieveReferenceCasePort);
        order.verify(commandReferenceCasePort).saveAll(cases);
        order.verify(retrieveReferenceCasePort).index(cases);
    }

    @Test
    void wrapsCsvReadFailuresAndDoesNotWriteOrIndex() throws Exception {
        IOException cause = new IOException("unavailable");
        when(csvPort.read()).thenThrow(cause);

        ReferenceCaseSetupException thrown = assertThrows(ReferenceCaseSetupException.class,
                () -> service.loadCsvAndInitialize());

        assertSame(cause, thrown.getCause());
        verifyNoInteractions(commandReferenceCasePort, retrieveReferenceCasePort);
    }

    @Test
    void wrapsCsvParseFailures() throws Exception {
        CsvException cause = new CsvException("bad CSV");
        when(csvPort.read()).thenThrow(cause);

        ReferenceCaseSetupException thrown = assertThrows(ReferenceCaseSetupException.class,
                () -> service.loadCsvAndInitialize());

        assertSame(cause, thrown.getCause());
    }

    private ReferenceCase reference(String id) {
        return ReferenceCase.create(id, "Target", "Source", EvaluationLabel.SUCCESS, "reason");
    }
}
