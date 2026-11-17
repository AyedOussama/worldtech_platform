package fr.entreprise.maintenance.features.assets.domain;

import java.util.Map;

public record EquipmentProfile(String label,String category,String internalReference,String brand,String model,String serialNumber,String location,String conditionDescription,Map<String,String> attributes) {
    public EquipmentProfile {
        label=AssetText.text(label,120); category=AssetText.text(category,80); internalReference=AssetText.text(internalReference,100);
        brand=AssetText.text(brand,100); model=AssetText.text(model,100); serialNumber=AssetText.text(serialNumber,100);
        location=AssetText.text(location,200); conditionDescription=AssetText.text(conditionDescription,200);
        if(attributes!=null) {
            if(attributes.size()>20 || attributes.entrySet().stream().anyMatch(e->e.getKey()==null || !e.getKey().matches("[A-Za-z][A-Za-z0-9_-]{0,39}") || e.getValue()==null || e.getValue().isBlank() || e.getValue().length()>200 || e.getValue().chars().anyMatch(Character::isISOControl))) { throw AssetText.invalid(); }
            attributes=Map.copyOf(attributes);
        }
    }
    public EquipmentProfile complete() { AssetText.required(label); AssetText.required(category); return new EquipmentProfile(label,category,internalReference,brand,model,serialNumber,location,conditionDescription,attributes==null?Map.of():attributes); }
    public EquipmentProfile merge(EquipmentProfile c) { return new EquipmentProfile(label==null?c.label:label,category==null?c.category:category,internalReference==null?c.internalReference:internalReference,brand==null?c.brand:brand,model==null?c.model:model,serialNumber==null?c.serialNumber:serialNumber,location==null?c.location:location,conditionDescription==null?c.conditionDescription:conditionDescription,attributes==null?c.attributes:attributes).complete(); }
    @Override public String toString() { return "EquipmentProfile[redacted]"; }
}
