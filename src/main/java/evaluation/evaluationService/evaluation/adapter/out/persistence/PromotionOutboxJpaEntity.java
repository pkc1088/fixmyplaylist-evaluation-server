package evaluation.evaluationService.evaluation.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "promotion_outbox")
public class PromotionOutboxJpaEntity implements Persistable<String> {

    @Id
    @Column(length = 100)
    private String referenceCaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = true, length = 100)
    private String lastFailureType;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, INDEXED, FAILED
    }


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

    @PrePersist
    public void prePersist() {
        LocalDateTime localDateTime = LocalDateTime.now();
        this.createdAt = localDateTime;
        this.updatedAt = localDateTime;
    }


    public static PromotionOutboxJpaEntity createPending(String referenceCaseId) {
        return PromotionOutboxJpaEntity.builder()
                .referenceCaseId(referenceCaseId)
                .status(Status.PENDING)
                .attempts(0)
                .isNew(true)
                .build();
    }

    void markIndexed() {
        this.status = Status.INDEXED;
        this.lastFailureType = null;
        this.updatedAt = LocalDateTime.now();
    }

    void markFailed(String failureType) {
        if (this.status == Status.INDEXED) return;

        this.status = Status.FAILED;
        this.attempts++;
        this.lastFailureType = failureType;
        this.updatedAt = LocalDateTime.now();
    }
}
