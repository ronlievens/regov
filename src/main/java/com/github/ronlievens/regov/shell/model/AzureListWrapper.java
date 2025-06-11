package com.github.ronlievens.regov.shell.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
@Setter
public class AzureListWrapper<T> {

    private Long count;
    private List<T> value;

}
