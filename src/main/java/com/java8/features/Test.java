package com.java8.features;

import com.java8.features.patterns.Patterns_Singleton;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.Set;

public class Test extends AbstractProcessor {
    static void main() {
        Patterns_Singleton instance = Patterns_Singleton.getInstance();
        Patterns_Singleton instance1 = Patterns_Singleton.getInstance();
        Patterns_Singleton instance2 = Patterns_Singleton.getInstance();
        Patterns_Singleton instance3 = Patterns_Singleton.getInstance();
        System.out.println(instance.hashCode());
        System.out.println(instance1.hashCode());
        System.out.println(instance2.hashCode());
        System.out.println(instance3.hashCode());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            System.out.println(annotation);
        }
        return false;
    }
}
