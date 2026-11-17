package fr.entreprise.maintenance.features.assets.domain;

public final class AssetFailure extends RuntimeException {
    public enum Code { INVALID_ASSET_INPUT, ASSET_NOT_FOUND, ASSET_VERSION_STALE }
    private final Code code;
    public AssetFailure(Code code) { super("Asset command refused."); this.code=code; }
    public Code code() { return code; }
}
