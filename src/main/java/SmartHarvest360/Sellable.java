package SmartHarvest360;

 // Interface representing anything that can be sold.This OOP Concept represents Abstraction (Interface)
public interface Sellable {

    // Method declaration for selling a quantity of crops.
    // The implementation will be provided by the Crop class.
    double sell(double quantity);

}