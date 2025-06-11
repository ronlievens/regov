package com.github.ronlievens.regov.shell.model;

public record ShellResult(
    int exitCode,
    String value
) {
}
