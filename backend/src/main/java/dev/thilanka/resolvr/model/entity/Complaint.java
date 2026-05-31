package dev.thilanka.resolvr.model.entity;

import dev.thilanka.resolvr.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Reference & Location ────────────────────────────────────
    @Column(name = "ref_number", nullable = false, unique = true, length = 30)
    private String refNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    // ── Status & Priority ───────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComplaintStatus status = ComplaintStatus.NOT_ASSIGNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ComplaintPriority priority;

    @Column(name = "target_date", nullable = false)
    private LocalDateTime targetDate;

    // ── Ownership ───────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "raised_by", nullable = false, length = 150)
    private String raisedBy;

    // ── Customer Information ────────────────────────────────────
    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "contact_number", nullable = false, length = 30)
    private String contactNumber;

    @Column(nullable = false, length = 500)
    private String msisdns;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_category", nullable = false, length = 20)
    private IssueCategory issueCategory;

    @Column(name = "issue_description", nullable = false, columnDefinition = "TEXT")
    private String issueDescription;

    @Column(name = "issue_duration", length = 100)
    private String issueDuration;

    @Column(name = "last_experienced")
    private LocalDate lastExperienced;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private TechnologyType technology;

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    // ── Device Information ──────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 10)
    private DeviceType deviceType;

    @Column(name = "signal_bars")
    private Short signalBars;

    @Column(name = "using_vpn_apn")
    private Boolean usingVpnApn;

    // ── Analysis (filled after starting) ───────────────────────
    @Column(name = "serving_sites_cells", length = 500)
    private String servingSitesCells;

    @Column(name = "coverage_quality", length = 500)
    private String coverageQuality;

    // ── Customer feedback ───────────────────────────────────────
    @Column(name = "customer_feedback_taken", nullable = false)
    @Builder.Default
    private boolean customerFeedbackTaken = false;

    // ── Child collections ───────────────────────────────────────
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AnalysisEntry> analysisEntries = new ArrayList<>();

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<SolutionEntry> solutionEntries = new ArrayList<>();

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<AuditLog> auditLogs = new ArrayList<>();

    // ── Timestamps ──────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
