package com.github.ronlievens.regov.exceptions;

import lombok.Getter;

import java.io.Serial;

@Getter
public class ExitException extends Exception {

    @Serial
    private static final long serialVersionUID = 5376193792172947524L;
}
