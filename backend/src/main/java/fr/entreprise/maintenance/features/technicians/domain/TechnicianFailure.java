package fr.entreprise.maintenance.features.technicians.domain;

public final class TechnicianFailure extends RuntimeException {
    public enum Code { INVALID_TECHNICIAN_INPUT, TECHNICIAN_DOSSIER_NOT_FOUND, TECHNICIAN_STATE_CONFLICT,
        TECHNICIAN_VERSION_STALE, TECHNICIAN_PROFILE_INCOMPLETE, TECHNICIAN_DOCUMENTS_REQUIRED }
    private final Code code;
    public TechnicianFailure(Code code) { super("Technician command refused."); this.code=code; }
    public Code code() { return code; }
}
