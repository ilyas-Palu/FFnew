package com.ssn.simulation.plugin.zFTS1;

import com.ssi.wasoc.api.cfg.WaSocConnectionCfg;
import com.ssi.wasoc.internal.nio.protocol.ProtocolAdapter;

public class ASCIIConnectionCfg extends WaSocConnectionCfg {

    protected ByteHandler byteHandler;
    protected ProtocolAdapterCfg config;

    public ASCIIConnectionCfg(String connectionName, String host, int port, boolean serverSocket, ByteHandler byteHandler, ProtocolAdapterCfg config) {
        super(connectionName, host, port, serverSocket);
        this.byteHandler = byteHandler;
        this.config = config;
    }

    @Override
    public ProtocolAdapter createProtocolAdapter() {
        return new ASCIIProtocolAdapter(this.byteHandler, this.config);
    }
    
}
