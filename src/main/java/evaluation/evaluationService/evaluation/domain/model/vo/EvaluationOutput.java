package evaluation.evaluationService.evaluation.domain.model.vo;

import evaluation.evaluationService.evaluation.domain.model.enums.HumanLabel;

import java.util.List;

public record EvaluationOutput(
        String id,
        HumanLabel humanLabel,

        HumanLabel zeroShotLabel,
        double zeroShotConfidence,
        String zeroShotReason,

        HumanLabel ragLabel,
        double ragConfidence,
        String ragReason,

        List<String> retrievedCaseInfo
) {

    public static EvaluationOutput from(
            RecoveryCase testCase,
            EvaluationResult zeroShot,
            EvaluationResult rag,
            List<RetrievedCase> retrievedCases
    ) {
        return new EvaluationOutput(
                testCase.id(),
                testCase.humanLabel(),

                zeroShot.label(),
                zeroShot.confidence(),
                zeroShot.reason(),

                rag.label(),
                rag.confidence(),
                rag.reason(),

                retrievedCases.stream()
                        .map(rc -> String.format("%s (%.4f)", rc.recoveryCase().id(), rc.score()))
                        //.map(RecoveryCase::id)
                        .toList()
        );
    }
}