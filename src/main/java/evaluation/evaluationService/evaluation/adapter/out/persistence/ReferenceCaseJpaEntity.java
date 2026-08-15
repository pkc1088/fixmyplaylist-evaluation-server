package evaluation.evaluationService.evaluation.adapter.out.persistence;

import evaluation.evaluationService.evaluation.domain.model.enums.EvaluationLabel;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "reference_cases")
public class ReferenceCaseJpaEntity implements Persistable<String> {

    @Id
    @Column(length = 100)
    private String referenceCaseId;

    @Column(nullable = false, length = 255)
    private String targetTitle;

    @Column(nullable = false, length = 255)
    private String sourceTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvaluationLabel humanLabel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String humanReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;


    @Transient
    @Builder.Default
    private boolean isNew = false;

    @Override
    public String getId() {
        return this.referenceCaseId;
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