package com.ixale.starparse.calculator;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AlacrityCalculator {
    static class Sophistication {
        String name;
        int pow;
        int alac;

        public int getPow() {
            return pow;
        }

        public int getAlac() {
            return alac;
        }

        @Override
        public String toString() {
            return name;
        }

        public Sophistication(int index, int pow, int alac) {
            this.pow = pow;
            this.alac = alac;
            this.name = "80R-" + index;
        }
    }

    static Sophistication[] sophistications = {
            new Sophistication(1, 313, 431),
            new Sophistication(2, 316, 429),
            new Sophistication(3, 318, 427),
            new Sophistication(4, 321, 424),
            new Sophistication(5, 324, 422),
            new Sophistication(6, 326, 420),
            new Sophistication(7, 329, 418),
            new Sophistication(8, 331, 416),
            new Sophistication(9, 334, 414),
            new Sophistication(10, 337, 412),
            new Sophistication(11, 339, 409),
            new Sophistication(12, 342, 407),
            new Sophistication(13, 344, 405),
            new Sophistication(14, 347, 403),
            new Sophistication(15, 349, 401),
            new Sophistication(16, 352, 398),
            new Sophistication(17, 354, 396),
            new Sophistication(18, 357, 394),
            new Sophistication(19, 359, 392),
            new Sophistication(20, 361, 389)
    };

    public static class Combinaison {
        List<Sophistication> sophistications = new ArrayList<>();
        int nbAmelio = 0;

        public Combinaison(int nbAmelio) {
            this.nbAmelio = nbAmelio;
        }

        public int getTotalAlac() {
            return 108 * nbAmelio + sophistications.stream().mapToInt(Sophistication::getAlac).sum();
        }

        public int getTotalPow() {
            return 144 * nbAmelio + sophistications.stream().mapToInt(Sophistication::getPow).sum();
        }

        Combinaison copyWithAddedSophistication(Sophistication sophistication) {
            Combinaison combinaison = new Combinaison(this.nbAmelio);
            combinaison.sophistications = new ArrayList<>(this.sophistications);
            combinaison.sophistications.add(sophistication);
            return combinaison;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Combinaison that = (Combinaison) o;

            String thisNames = this.sophistications.stream().map(Sophistication::toString).sorted().collect(Collectors.joining());
            String thatNames = that.sophistications.stream().map(Sophistication::toString).sorted().collect(Collectors.joining());

            return nbAmelio == that.nbAmelio && Objects.equals(thisNames, thatNames);
        }

        @Override
        public int hashCode() {
            List<String> names = sophistications.stream().map(Sophistication::toString).sorted().collect(Collectors.toList());
            return Objects.hash(names, nbAmelio);
        }

        @Override
        public String toString() {
            return "Combinaison{" +
                    "sophistications=" + sophistications +
                    ", nbAmelio=" + nbAmelio +
                    '}';
        }

        public String[] toStringArray() {
            return sophistications.stream().map(Sophistication::toString).collect(Collectors.toList()).toArray(new String[sophistications.size()]);
        }
    }

    public static Set<String[]> computeCombinationsAsString(int alacGoal, int nbSophs, int nbAugment) {
        return computeCombinations(alacGoal, nbSophs, nbAugment).stream().map(Combinaison::toStringArray).collect(Collectors.toSet());
    }

    public static Set<Combinaison> computeCombinations(int alacGoal, int nbSophs, int nbAugment) {
        Set<Combinaison> admissibles = new HashSet<>();
        int maxPow = 0;

        Set<Combinaison> combinaisons = new HashSet<>(Set.of(new Combinaison(nbAugment)));
        for (int i = 0; i < nbSophs; i++) {
            Set<Combinaison> currentSet = new HashSet<>(combinaisons);
            currentSet.forEach(combinaison -> {
                combinaisons.addAll(Stream.of(sophistications).map(combinaison::copyWithAddedSophistication).collect(Collectors.toSet()));
            });
        }

        for (Combinaison combinaison : combinaisons) {
            int totalAlac = combinaison.getTotalAlac();
            if (totalAlac >= alacGoal) {
                int totalPow = combinaison.getTotalPow();
                if (totalPow > maxPow) {
                    maxPow = totalPow;
                    admissibles.clear();
                    admissibles.add(combinaison);
                } else if (totalPow == maxPow) {
                    admissibles.add(combinaison);
                }
            }
        }

        Set<Combinaison> higherAlac = admissibles.stream().filter(combinaison -> combinaison.getTotalAlac() > alacGoal).collect(Collectors.toSet());
        if (higherAlac.isEmpty()) {

            higherAlac = admissibles;
        }
        return higherAlac;
    }
}
