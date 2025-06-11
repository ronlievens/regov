package com.github.ronlievens.regov.task.rewrite;

import com.github.ronlievens.regov.exceptions.ExitException;

public interface RewriteRunnableTask {

    void run(RewriteContext rewriteContext) throws ExitException;
}
