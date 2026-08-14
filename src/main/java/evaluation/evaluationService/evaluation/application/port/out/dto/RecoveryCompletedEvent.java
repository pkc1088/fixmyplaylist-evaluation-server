package evaluation.evaluationService.evaluation.application.port.out.dto;

public record RecoveryCompletedEvent(
        String eventId, // 아마도 "MDC 작업 UUID + ActionLog PK 번호"
        String targetTitle,
        String sourceTitle
        // 향후 searchQuery, candidateInfo/Score, target/sourceDescription, target/sourceChannel, target/sourceLabel 추가 가능
) {
}
