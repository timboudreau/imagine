/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.geometry;

import com.mastfrog.function.DoubleBiPredicate;
import java.util.function.Consumer;
import org.imagine.geometry.util.GeometryStrings;
import org.junit.jupiter.api.Assertions;

// Just a facade to make assertions with tolerances less
// verbose
final class CA extends SA<CornerAngle> {

    CA(double a, double b) {
        this(new CornerAngle(a, b));
    }

    CA(double ax, double ay, double sx, double sy, double bx, double by) {
        this(new CornerAngle(ax, ay, sx, sy, bx, by));
    }

    CA(CornerAngle ca) {
        super(ca);
        // Some basic "if this doesn't work everything is broken"
        // tests
        assertSaneDegrees(ca.trailingAngle(), null);
        assertSaneDegrees(ca.leadingAngle(), null);
        Assertions.assertTrue(ca.extent() > -360 && ca.extent() < 360, "insane extent " + ca.extent());
        if (!ca.isEmpty()) {
            softAssertTrue(ca.contains(ca.midAngle()), "Does not contain its " + "own mid-angle " + ca.midAngle() + " which is probably " + "miscomputed by " + ca + " norm " + ca.isNormalized() + " a " + ca.trailingAngle() + " l " + ca.leadingAngle() + " as sector " + ca.toSector() + " sector mid " + ca.toSector().midAngle());
            softAssertTrue(ca.contains(ca.quarterAngle()), "Does not contain its own 1/4-angle " + ca.quarterAngle() + ": " + ca);
            softAssertTrue(ca.contains(ca.threeQuarterAngle()), "Does not contain its own 3/4-angle " + ca.threeQuarterAngle() + ": " + ca);
        }
        double encoded = ca.encodeSigned();
        CornerAngle decoded = CornerAngle.decodeCornerAngle(encoded);
        assertFuzzyEquals(decoded, "Encoded '" + ca + "' as '" + GeometryStrings.toString(encoded)
                + "' but decoded '" + decoded);
        if (Math.abs(ca.extent()) != 0) {
            CornerAngle inv = ca.inverse();
            if (inv.leadingAngle() == ca.leadingAngle() && inv.trailingAngle() == ca.trailingAngle()) {
                Assertions.fail("Inverse of '" + ca + "' is itself: '" + inv + "'");
            }
        }
    } // Some basic "if this doesn't work everything is broken"
    // tests

    static void softAssertTrue(boolean val, String msg) {
        if (!val) {
            new AssertionError(msg).printStackTrace();
        }
    }

    public CA assertCoordinates(double a, double b) {
        return assertCoordinates(a, b, null);
    }

    public CA assertCoordinates(double a, double b, String msg) {
        assertA(msg, a);
        return assertB(msg, b);
    }

    public CA assertMidAngle(double mid) {
        return assertMidAngle(mid, null);
    }

    public CA assertMidAngle(double mid, String msg) {
        assertDouble(mid, ca.midAngle(), "1/2 angle should be " + mid + " in " + ca);
        return this;
    }

    public CA assertQuarterAngle(double qt) {
        return assertQuarterAngle(qt, null);
    }

    public CA assertQuarterAngle(double qt, String msg) {
        assertDouble(qt, ca.quarterAngle(), "1/4 angle should be " + qt + " in " + ca
                + " (3/4 angle is " + ca.threeQuarterAngle() + ")");
        return this;
    }

    public CA assertThreeQuartersAngle(double qt3) {
        return assertThreeQuartersAngle(qt3, null);
    }

    public CA assertThreeQuartersAngle(double qt3, String msg) {
        assertDouble(qt3, ca.threeQuarterAngle(), "3/4 angle should be " + qt3 + " in " + ca
                + " (1/4 angle is " + ca.quarterAngle() + ")");
        return this;
    }

    public CA assertSample(int expected, String msg, double dist, DoubleBiPredicate test) {
        int count = ca.sample(0, 0, dist, test);
        Assertions.assertEquals(expected, count, msg("Sampling at distance " + dist, msg));
        return this;
    }

    public CA assertSample(int expected, String msg, DoubleBiPredicate test) {
        int count = ca.sample(0, 0, test);
        Assertions.assertEquals(expected, count, msg("Sampling at default distance ", msg));
        return this;
    }

    public CA assertContains(double... angles) {
        for (int i = 0; i < angles.length; i++) {
            assertContains(angles[i]);
        }
        return this;
    }

    public CA assertContains(double angle) {
        return assertContains(angle, null);
    }

    public CA assertContains(double angle, String msg) {
        Assertions.assertTrue(ca.contains(angle), msg("Should contain " + angle, msg));
        return this;
    }

    public CA assertContains(Sector sector) {
        return assertContains(sector, null);
    }

    public CA assertContains(Sector sector, String msg) {
        Assertions.assertTrue(ca.contains(sector), msg("Should contain " + sector, msg));
        return this;
    }

    public CA assertDoesNotContain(double... angles) {
        for (int i = 0; i < angles.length; i++) {
            assertDoesNotContain(angles[i]);
        }
        return this;
    }

    public CA assertDoesNotContain(double angle) {
        return assertDoesNotContain(angle, null);
    }

    public CA assertDoesNotContain(double angle, String msg) {
        Assertions.assertFalse(ca.contains(angle), msg("Should not contain " + angle, msg));
        return this;
    }

    public CA assertDoesNotContain(Sector sector) {
        return assertDoesNotContain(sector, null);
    }

    public CA assertDoesNotContain(Sector sector, String msg) {
        Assertions.assertFalse(ca.contains(sector), msg("Should not contain " + sector, msg));
        return this;
    }

    public CA withSelf(Consumer<CA> c) {
        c.accept(this);
        return this;
    }

    public CA sector(Consumer<SA<Sector>> c) {
        c.accept(new SA(ca.toSector()));
        return this;
    }

    public CA opposite(Consumer<CA> c) {
        c.accept(opposite());
        return this;
    }

    public CA normalized(Consumer<CA> c) {
        c.accept(normalized());
        return this;
    }

    public CA inverse(Consumer<CA> c) {
        c.accept(inverse());
        return this;
    }

    public CA assertEquals(CA expected) {
        return assertEquals(expected, null);
    }

    public CA assertEquals(CA expected, String msg) {
        Assertions.assertEquals(expected.ca, ca, msg(msgs(msg, ca + " vs. " + expected.ca)));
        return this;
    }

    public CA assertEquals(CornerAngle other) {
        return assertEquals(other, null);
    }

    public CA assertEquals(CornerAngle expected, String msg) {
        Assertions.assertEquals(expected, ca, msg(msgs(msg, ca + " vs. " + expected)));
        return this;
    }

    public CA assertClockwise() {
        return assertClockwise(null);
    }

    public CA assertClockwise(String msg) {
        return assertDirection(RotationDirection.CLOCKWISE, msgs("Should be clockwise", msg));
    }

    public CA assertCounterClockwise() {
        return assertCounterClockwise(null);
    }

    public CA assertCounterClockwise(String msg) {
        return assertDirection(RotationDirection.COUNTER_CLOCKWISE, msgs("Should be counter-clockwise", msg));
    }

    public CA assertDirection(RotationDirection dir) {
        return assertDirection(dir, null);
    }

    public CA assertDirection(RotationDirection dir, String msg) {
        Assertions.assertSame(dir, ca.direction(), msg(msgs("Wrong direction", msg)));
        return this;
    }

    public CA assertRight() {
        return assertRight(null);
    }

    public CA assertRight(String msg) {
        Assertions.assertTrue(ca.isRightAngle(), msg(msgs("expected right angle", msg)));
        return this;
    }

    public CA assertNotNormalized() {
        Assertions.assertFalse(ca.isNormalized(), "Claims to be normalized '" + ca + "'");
        return this;
    }

    public CA assertNotNormalized(String msg) {
        Assertions.assertFalse(ca.isNormalized(), msg(msg, "Claims to be normalized '" + ca + "'"));
        return this;
    }

    public CA assertNormalized() {
        Assertions.assertTrue(ca.isNormalized(), "Claims NOT to be normalized '" + ca + "'");
        return this;
    }

    public CA assertNormalized(String msg) {
        Assertions.assertTrue(ca.isNormalized(), msg(msg, "Claims NOT to be normalized '" + ca + "'"));
        return this;
    }

    @Override
    public CA assertExtent(double val) {
        return assertExtent(null, val);
    }

    @Override
    public CA assertExtent(String msg, double val) {
        assertDouble(val, ca.extent(), msg("Wrong extent", msg));
        return this;
    }

    public CA assertA(double a) {
        return assertA(null, a);
    }

    public CA assertA(String msg, double a) {
        assertSaneDegrees(ca.trailingAngle(), msg);
        assertDouble(a, ca.trailingAngle(), msg("Wrong angle A", msg));
        return this;
    }

    public CA assertB(double b) {
        return assertB(null, b);
    }

    public CA assertNotEmpty() {
        Assertions.assertFalse(ca.isEmpty(), "Should not claim to be empty: " + "'" + ca + "'");
        return this;
    }

    public CA assertEmpty() {
        Assertions.assertTrue(ca.isEmpty(), "Should be empty: " + "'" + ca + "'");
        return this;
    }

    public CA assertB(String msg, double b) {
        assertSaneDegrees(ca.leadingAngle(), msg);
        assertDouble(b, ca.leadingAngle(), msg("Wrong angle A", msg));
        return this;
    }

    double extent() {
        return ca.extent();
    }

    public String toShortString() {
        return ca.toShortString();
    }

    public strictfp double trailingAngle() {
        return ca.trailingAngle();
    }

    public strictfp double leadingAngle() {
        return ca.leadingAngle();
    }

    public RotationDirection direction() {
        return ca.direction();
    }

    public Sector toSector() {
        return ca.toSector();
    }

    public CA assertFuzzyEquals(CornerAngle ca) {
        return assertFuzzyEquals(ca, 0.01);
    }

    public CA assertFuzzyEquals(CornerAngle ca, String msg) {
        return assertFuzzyEquals(ca, 0.01, msg);
    }

    public CA assertFuzzyEquals(CornerAngle ca, double tolerance) {
        return assertFuzzyEquals(ca, tolerance, null);
    }

    public CA assertFuzzyEquals(CornerAngle other, double tolerance, String msg) {
        Assertions.assertEquals(ca.leadingAngle(), other.leadingAngle(), tolerance,
                msg(msg, "Leading angles not within tolerance"));
        Assertions.assertEquals(ca.trailingAngle(), other.trailingAngle(), tolerance,
                msg(msg, "Trailing angles not within tolerance"));
        return this;
    }

    public CA normalized() {
        return new CA(ca.normalized());
    }

    public CA opposite() {
        return new CA(ca.opposite());
    }

    public CA inverse() {
        return new CA(ca.inverse());
    }

    @Override
    public String toString() {
        return ca.toString();
    }

    public double midAngle() {
        return ca.midAngle();
    }

}
