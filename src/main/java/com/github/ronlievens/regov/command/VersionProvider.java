package com.github.ronlievens.regov.command;

import lombok.val;
import picocli.CommandLine;

class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        val pkg = VersionProvider.class.getPackage();
        return new String[]{
            String.join(" ",
                pkg.getImplementationTitle(),
                pkg.getImplementationVersion()
            )
        };
    }
}
