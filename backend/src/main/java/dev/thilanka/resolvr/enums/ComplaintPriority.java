package dev.thilanka.resolvr.enums;

public enum ComplaintPriority {
    LOW,
    MEDIUM,
    HIGH;

    public long getTargetHours() {
        return switch (this) {
            case LOW    -> 72;
            case MEDIUM -> 48;
            case HIGH   -> 24;
        };
    }
}
