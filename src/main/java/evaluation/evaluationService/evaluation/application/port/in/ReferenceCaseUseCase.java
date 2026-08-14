package evaluation.evaluationService.evaluation.application.port.in;

public interface ReferenceCaseUseCase {

    void loadCsvAndInitialize();

    void reindexAllFromDatabase();
}
