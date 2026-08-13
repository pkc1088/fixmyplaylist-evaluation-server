package evaluation.evaluationService.evaluation.application.port.in;

public interface ReferenceCaseSetupUseCase {

    void loadCsvAndInitialize(Integer limit);

    void reindexAllFromDatabase();
}
