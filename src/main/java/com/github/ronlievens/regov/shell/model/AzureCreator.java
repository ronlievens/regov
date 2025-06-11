package com.github.ronlievens.regov.shell.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.URL;

@ToString
@Getter
@Setter
public class AzureCreator {
    private String displayName;
    private URL url;
    // ignore _links
}
