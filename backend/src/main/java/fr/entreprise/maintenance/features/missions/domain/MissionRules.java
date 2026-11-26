package fr.entreprise.maintenance.features.missions.domain;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

final class MissionRules {
    private static final BigDecimal MAX_AMOUNT=new BigDecimal("9999999999.99");
    private MissionRules() { }
    static String text(String value,int max) {
        String checked=optionalText(value,max);
        if(checked==null) { throw MissionFailure.invalid(); }
        return checked;
    }
    static String optionalText(String value,int max) {
        if(value==null) { return null; }
        if(value.isBlank() || value.length()>max || value.chars().anyMatch(c->Character.isISOControl(c) && c!='\n' && c!='\t')) { throw MissionFailure.invalid(); }
        return value.strip();
    }
    static List<String> skills(List<String> values) {
        var checked=optionalSkills(values);
        if(checked.isEmpty()) { throw MissionFailure.invalid(); }
        return checked;
    }
    static List<String> optionalSkills(List<String> values) {
        if(values==null) { return List.of(); }
        if(values.size()>10) { throw MissionFailure.invalid(); }
        var unique=new LinkedHashMap<String,String>();
        for(String value:values) {
            String checked=text(value,80);
            unique.putIfAbsent(checked.toLowerCase(Locale.ROOT),checked);
        }
        return unique.values().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
    static BigDecimal money(String text,boolean required) {
        if(text==null) { if(required) { throw MissionFailure.invalid(); } return null; }
        if(!text.matches("(0|[1-9][0-9]{0,9})(\\.[0-9]{1,2})?")) { throw MissionFailure.invalid(); }
        return money(new BigDecimal(text));
    }
    static BigDecimal money(BigDecimal value) {
        if(value==null) { throw MissionFailure.invalid(); }
        if(value.signum()<0 || value.stripTrailingZeros().scale()>2 || value.compareTo(MAX_AMOUNT)>0) { throw MissionFailure.invalid(); }
        return value.setScale(2);
    }
    static BigDecimal coordinate(BigDecimal value,BigDecimal min,BigDecimal max) {
        if(value==null || value.compareTo(min)<0 || value.compareTo(max)>0 || value.stripTrailingZeros().scale()>6) { throw MissionFailure.invalid(); }
        return value.setScale(6);
    }
}
