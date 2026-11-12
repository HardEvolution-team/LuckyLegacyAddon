package com.electrifiedded.luckylegacyaddon.convert.fixers;

import com.electrifiedded.luckylegacyaddon.convert.Converted;

public interface Fixer {
    String name();

    void fix(Converted addon);
}
