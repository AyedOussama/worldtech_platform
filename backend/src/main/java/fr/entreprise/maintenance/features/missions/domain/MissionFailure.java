package fr.entreprise.maintenance.features.missions.domain;

public final class MissionFailure extends RuntimeException {
    public enum Code { INVALID_MISSION_INPUT, MISSION_NOT_FOUND, MISSION_VERSION_STALE, MISSION_STATE_CONFLICT }
    private final Code code;
    public MissionFailure(Code code) { super("Mission command refused."); this.code=code; }
    public Code code() { return code; }
    public static MissionFailure invalid() { return new MissionFailure(Code.INVALID_MISSION_INPUT); }
    public static MissionFailure conflict() { return new MissionFailure(Code.MISSION_STATE_CONFLICT); }
}
