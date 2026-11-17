package fr.entreprise.maintenance.features.assets.domain;

import java.time.ZoneId;
import java.util.List;

public record SiteProfile(String name,String address,String timeZone,List<Contact> contacts,String accessInstructions) {
    public record Contact(String name,String phone,String email) {
        public Contact {
            name=AssetText.text(name,100); AssetText.required(name);
            phone=AssetText.text(phone,40); email=AssetText.text(email,254);
            if(phone!=null && !phone.matches("[+0-9() .-]{6,40}")) { throw AssetText.invalid(); }
            if(email!=null && !email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}")) { throw AssetText.invalid(); }
        }
        @Override public String toString() { return "Contact[redacted]"; }
    }
    public SiteProfile {
        name=AssetText.text(name,120); address=AssetText.text(address,500); timeZone=AssetText.text(timeZone,100);
        accessInstructions=AssetText.text(accessInstructions,2000);
        if(timeZone!=null && !ZoneId.getAvailableZoneIds().contains(timeZone)) { throw AssetText.invalid(); }
        if(contacts!=null) {
            if(contacts.size()>10 || contacts.stream().anyMatch(c->c==null)) { throw AssetText.invalid(); }
            contacts=List.copyOf(contacts);
        }
    }
    public SiteProfile complete() { AssetText.required(name); AssetText.required(address); AssetText.required(timeZone); return new SiteProfile(name,address,timeZone,contacts==null?List.of():contacts,accessInstructions); }
    public SiteProfile merge(SiteProfile current) { return new SiteProfile(name==null?current.name:name,address==null?current.address:address,timeZone==null?current.timeZone:timeZone,contacts==null?current.contacts:contacts,accessInstructions==null?current.accessInstructions:accessInstructions).complete(); }
    @Override public String toString() { return "SiteProfile[redacted]"; }
}
