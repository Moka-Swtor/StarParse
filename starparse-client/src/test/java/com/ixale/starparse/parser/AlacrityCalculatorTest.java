package com.ixale.starparse.parser;

import com.ixale.starparse.calculator.AlacrityCalculator;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ixale.starparse.calculator.AlacrityCalculator.computeCombinations;

public class AlacrityCalculatorTest {

    @Test
    public void given_goal_1619_with_4_enhancement_and_0_augment_should_get_3_possibilities() {
        int alacGoal = 1619;
        int nbSophs = 4;        // don't try more than 6, compute time is exponentiol!
        int nbAugment = 0;

        Set<AlacrityCalculator.Combinaison> higherAlac = computeCombinations(alacGoal, nbSophs, nbAugment);
        Assert.assertEquals(3, higherAlac.size());
        AlacrityCalculator.Combinaison first = higherAlac.iterator().next();
        Assert.assertEquals(1619, first.getTotalAlac());
        Assert.assertEquals(1380, first.getTotalPow());

        higherAlac.forEach(combinaison -> System.out.println(combinaison.toString()+" alac="+combinaison.getTotalAlac()+" pow="+combinaison.getTotalPow()));
    }

}
