package fr.entreprise.maintenance.features.technicians.domain;

import java.util.List;

public record TechnicianProfile(String firstName, String lastName, String phone, String baseCity, Integer radiusKm, List<String> declaredSkills) {
    public TechnicianProfile {
        firstName=text(firstName,100); lastName=text(lastName,100); phone=text(phone,40); baseCity=text(baseCity,200);
        if (phone!=null && !phone.matches("[+0-9() .-]{6,40}")) { throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT); }
        if (radiusKm!=null && (radiusKm<10 || radiusKm>200)) { throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT); }
        declaredSkills=declaredSkills==null ? List.of() : declaredSkills;
        if (declaredSkills.size()>20 || declaredSkills.stream().anyMatch(s -> s==null || s.isBlank() || s.length()>80 || s.chars().anyMatch(Character::isISOControl))) {
            throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT);
        }
        declaredSkills=declaredSkills.stream().map(String::strip).distinct().sorted().toList();
    }
    public void requireComplete() {
        if (firstName==null || lastName==null || phone==null || baseCity==null || radiusKm==null || declaredSkills.isEmpty()) {
            throw new TechnicianFailure(TechnicianFailure.Code.TECHNICIAN_PROFILE_INCOMPLETE);
        }
    }
    private static String text(String value,int max) {
        if (value==null || value.isBlank()) { return null; }
        String result=value.strip();
        if (result.length()>max || result.chars().anyMatch(Character::isISOControl)) { throw new TechnicianFailure(TechnicianFailure.Code.INVALID_TECHNICIAN_INPUT); }
        return result;
    }
    @Override public String toString() { return "TechnicianProfile[redacted]"; }
}
