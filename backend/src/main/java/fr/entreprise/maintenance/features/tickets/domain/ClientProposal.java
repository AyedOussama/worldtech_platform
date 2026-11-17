package fr.entreprise.maintenance.features.tickets.domain;

import java.math.BigDecimal;

/** Client intent only, never a published technician price, invoice or implicit contract price. */
public record ClientProposal(BigDecimal remuneration, BigDecimal expenses) {
    private static final BigDecimal MAX=new BigDecimal("9999999999.99");
    public ClientProposal { remuneration=checked(remuneration); expenses=checked(expenses); }
    public static ClientProposal parse(String remuneration, String expenses) { return new ClientProposal(amount(remuneration),amount(expenses)); }
    private static BigDecimal amount(String text) {
        if(text==null) { return null; }
        if(!text.matches("(0|[1-9][0-9]{0,9})(\\.[0-9]{1,2})?")) { throw TicketFailure.invalid(); }
        return new BigDecimal(text);
    }
    private static BigDecimal checked(BigDecimal amount) {
        if(amount==null) { return null; }
        if(amount.signum()<0 || amount.scale()>2 || amount.compareTo(MAX)>0) { throw TicketFailure.invalid(); }
        return amount.setScale(2);
    }
    @Override public String toString() { return "ClientProposal[redacted]"; }
}
