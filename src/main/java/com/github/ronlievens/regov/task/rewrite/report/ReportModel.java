package com.github.ronlievens.regov.task.rewrite.report;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.TreeSet;

@ToString
@Getter
@Setter
public class ReportModel {

    private String ticket;
    private TreeSet<ReportRepositoryModel> repositories;

    public ReportModel() {
        repositories = new TreeSet<>();
    }
}
