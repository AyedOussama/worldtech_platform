package fr.entreprise.maintenance.shared.idempotency;

/** Unambiguous length-prefixed input for receipts; only its digest is persisted. Never log it. */
public final class CommandFingerprint {
    private CommandFingerprint() { }
    public static String of(String... fields) {
        var result=new StringBuilder();
        for(String field:fields) { if(field==null) { result.append("-1:"); } else { result.append(field.length()).append(':').append(field); } }
        return result.toString();
    }
}
