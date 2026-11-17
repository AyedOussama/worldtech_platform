package fr.entreprise.maintenance.features.contracts.domain;

public final class ContractFailure extends RuntimeException {
    public enum Code { INVALID_CONTRACT_INPUT, CONTRACT_NOT_FOUND, CONTRACT_VERSION_STALE, CONTRACT_NOT_APPLICABLE }
    private final Code code;
    public ContractFailure(Code code) { super("Contract command refused."); this.code=code; }
    public Code code() { return code; }
    public static ContractFailure invalid() { return new ContractFailure(Code.INVALID_CONTRACT_INPUT); }
}
