package dev.thilanka.resolvr.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "complaint_ref_sequences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(ComplaintRefSequenceId.class)
public class ComplaintRefSequence {

    @Id
    @Column(name = "district_id", nullable = false)
    private Long districtId;

    @Id
    @Column(name = "year_month", nullable = false, columnDefinition = "varchar(6)")
    private String yearMonth;

    @Column(name = "last_seq", nullable = false)
    @Builder.Default
    private int lastSeq = 0;
}
