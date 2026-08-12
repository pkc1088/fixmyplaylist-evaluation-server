//package evaluation.evaluationService.evaluation.domain.model.vo;
//
//import evaluation.evaluationService.evaluation.domain.model.EvaluationCase;
//import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
//
//import java.util.List;
//
//public record EvaluationOutput(
//        String id,
//        EvaluationLabel humanLabel,
//
//        EvaluationLabel zeroShotLabel,
//        double zeroShotConfidence,
//
//        EvaluationLabel ragLabel,
//        double ragConfidence,
//
//        List<String> retrievedCaseInfo
//) {
//
//    public static EvaluationOutput from(
//            EvaluationCase testCase,
//            EvaluationResult zeroShot,
//            EvaluationResult rag,
//            List<RetrievedCase> retrievedCases
//    ) {
//        return new EvaluationOutput(
//                testCase.getRecoveryId(),
//                testCase.getHumanLabel(),
//
//                zeroShot.label(),
//                zeroShot.confidence(),
//
//                rag.label(),
//                rag.confidence(),
//
//                retrievedCases.stream()
//                        .map(rc -> String.format("%s (%.4f)",
//                                rc.referenceCase().getReferenceCaseId(),
//                                rc.score()))
//                        .toList()
//        );
//    }
//}