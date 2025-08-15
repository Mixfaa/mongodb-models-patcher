package com.mixfa.mongopatcher;

import com.mixfa.mongopatcher.patchfunc.PatchFunc0;
import com.mixfa.mongopatcher.patchfunc.PatchFunc1;
import com.mixfa.mongopatcher.patchfunc.PatchFunc2;
import org.springframework.data.mongodb.core.query.Update;

public class Patch<CT> extends Update {
    public Patch<CT> apply(PatchFunc0<CT> func) {
        return func.apply(this);
    }

    public <T> Patch<CT> apply(PatchFunc1<CT, T> func, T param) {
        return func.apply(this, param);
    }

    public <T1, T2> Patch<CT> apply(PatchFunc2<CT, T1, T2> func, T1 param1, T2 param2) {
        return func.apply(this, param1, param2);
    }
}
