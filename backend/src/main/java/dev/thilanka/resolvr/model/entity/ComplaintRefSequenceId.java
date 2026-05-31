package dev.thilanka.resolvr.model.entity;

import java.io.Serializable;
import java.util.Objects;

public class ComplaintRefSequenceId implements Serializable {

    private Long districtId;
    private String yearMonth;

    public ComplaintRefSequenceId() {}

    public ComplaintRefSequenceId(Long districtId, String yearMonth) {
        this.districtId = districtId;
        this.yearMonth = yearMonth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ComplaintRefSequenceId that)) return false;
        return Objects.equals(districtId, that.districtId) &&
                Objects.equals(yearMonth, that.yearMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(districtId, yearMonth);
    }
}
