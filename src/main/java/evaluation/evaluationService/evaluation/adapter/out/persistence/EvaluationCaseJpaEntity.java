package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationStatus;
import evaluation.evaluationService.evaluation.domain.model.vo.ReferenceTrace;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "evaluation_cases")
public class EvaluationCaseJpaEntity implements Persistable<String> {

    @Id
    @Column(length = 100) // = Kafka 이벤트 ID, Inbox 패턴 de dup key
    private String evaluationCaseId;

    @Column(nullable = false, length = 255)
    private String targetTitle;

    @Column(nullable = false, length = 255)
    private String sourceTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private EvaluationLabel aiLabel;

    @Column(nullable = true)
    private Double aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationStatus evaluationStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private EvaluationLabel humanLabel;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String humanReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private LocalDateTime evaluatedAt;

    @Column(nullable = true)
    private LocalDateTime reviewedAt;


    @JdbcTypeCode(SqlTypes.JSON)
    @SuppressWarnings("JpaAttributeTypeInspection")
    @Column(nullable = true, columnDefinition = "json")
    private List<ReferenceTrace> retrievedInfo;


    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Override
    public String getId() {
        return this.evaluationCaseId;
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}