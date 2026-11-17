package fr.entreprise.maintenance.features.clients.domain;

public record ClientProfile(Type accountType, String firstName, String lastName, String phone, String address, String organizationName) {
    public enum Type { INDIVIDUAL, BUSINESS }
    public ClientProfile {
        firstName = text(firstName, 100); lastName = text(lastName, 100); phone = text(phone, 40);
        address = text(address, 500); organizationName = text(organizationName, 200);
        if (phone != null && !phone.matches("[+0-9() .-]{6,40}")) { throw new ClientFailure(ClientFailure.Code.INVALID_CLIENT_INPUT); }
        if (accountType == Type.INDIVIDUAL && organizationName != null) { throw new ClientFailure(ClientFailure.Code.INVALID_CLIENT_INPUT); }
    }
    public void requireComplete() {
        if (accountType == null || firstName == null || lastName == null || phone == null || address == null
                || (accountType == Type.BUSINESS && organizationName == null)) { throw new ClientFailure(ClientFailure.Code.CLIENT_PROFILE_INCOMPLETE); }
    }
    private static String text(String value, int max) {
        if (value == null || value.isBlank()) { return null; }
        String result = value.strip();
        if (result.length() > max || result.chars().anyMatch(Character::isISOControl)) { throw new ClientFailure(ClientFailure.Code.INVALID_CLIENT_INPUT); }
        return result;
    }
    @Override public String toString() { return "ClientProfile[redacted]"; }
}
