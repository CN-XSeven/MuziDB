package top.lixiaobo.muzidb.client;

import top.lixiaobo.muzidb.transport.Package;
import top.lixiaobo.muzidb.transport.Packager;

public class RoundTripper {
    private Packager packager;

    public RoundTripper(Packager packager) {
        this.packager = packager;
    }

    public Package roundTrip(Package pkg) throws Exception {
        packager.send(pkg);
        return packager.receive();
    }

    public void close() throws Exception {
        packager.close();
    }
}
