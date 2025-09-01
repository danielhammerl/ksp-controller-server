package de.danielhammerl.datastructs.subsystem0;

import de.danielhammerl.datastructs.DataStruct;

import java.nio.ByteBuffer;


public class DataStructSubsystem0 implements DataStruct {
    public DataStructSubsystem0() {
        toRead = new ToRead();
        toWrite = new ToWrite();
    }
    private ToRead toRead;
    private ToWrite toWrite;

    public void setToRead(ToRead toRead) {
        this.toRead = toRead;
    }

    public ToRead getToRead() {
        return toRead;
    }

    public ToWrite getToWrite() {
        return toWrite;
    }

    public void setToWrite(ToWrite write) {
        this.toWrite = write;
    }

    @Override
    public void fromBytes(byte[] bytes) {
        toRead = new ToRead();

        ByteBuffer buf = ByteBuffer.wrap(bytes);
        toRead.ping = buf.get() != 0;
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putDouble(toWrite.altitude);
        return buf.array();
    }

    public static class ToRead {
        public boolean ping;
    }

    public static class ToWrite {
        public double altitude;
    }
}
