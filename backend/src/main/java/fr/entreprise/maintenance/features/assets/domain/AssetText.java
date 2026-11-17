package fr.entreprise.maintenance.features.assets.domain;

final class AssetText {
    private AssetText() { }
    static String text(String value,int max) {
        if(value!=null && value.chars().anyMatch(Character::isISOControl)) { throw invalid(); }
        if(value==null || value.isBlank()) { return null; }
        String result=value.strip();
        if(result.length()>max || result.chars().anyMatch(Character::isISOControl)) { throw invalid(); }
        return result;
    }
    static void required(String value) { if(value==null) { throw invalid(); } }
    static AssetFailure invalid() { return new AssetFailure(AssetFailure.Code.INVALID_ASSET_INPUT); }
}
