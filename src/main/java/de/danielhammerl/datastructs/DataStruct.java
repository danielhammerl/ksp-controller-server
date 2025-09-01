package de.danielhammerl.datastructs;

public interface DataStruct {
    void fromBytes(byte[] bytes);
    byte[] toBytes();
}
