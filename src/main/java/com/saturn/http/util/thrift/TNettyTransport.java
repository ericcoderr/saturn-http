package com.saturn.http.util.thrift;

import io.netty.buffer.ByteBuf;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class TNettyTransport extends TTransport {

    private ByteBuf inputBuffer;
    private ByteBuf outputBuffer;

    public TNettyTransport(ByteBuf input, ByteBuf output) {
        this.inputBuffer = input;
        this.outputBuffer = output;
    }

    @Override
    public boolean isOpen() {
        // Buffer is always open
        return true;
    }

    @Override
    public void open() throws TTransportException {
        // Buffer is always open
    }

    @Override
    public void close() {
        // Buffer is always open
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws TTransportException {
        int readableBytes = inputBuffer.readableBytes();
        int bytesToRead = length > readableBytes ? readableBytes : length;

        inputBuffer.readBytes(buffer, offset, bytesToRead);
        return bytesToRead;
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws TTransportException {
        outputBuffer.writeBytes(buffer, offset, length);
    }

    public ByteBuf getInputBuffer() {
        return inputBuffer;
    }

    public ByteBuf getOutputBuffer() {
        return outputBuffer;
    }
}
