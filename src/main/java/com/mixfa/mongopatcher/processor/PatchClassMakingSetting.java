package com.mixfa.mongopatcher.processor;

import javax.lang.model.element.Element;

public interface PatchClassMakingSetting {

    record InnerFieldTweak(String innerFieldName) implements PatchClassMakingSetting {
        public String change(String originalFieldName) {
            return innerFieldName + "." + originalFieldName;
        }
    }

    record ReplaceOriginalClass(
            Element originalClass
    ) implements PatchClassMakingSetting {

    }
}
