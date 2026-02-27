package com.vcs.valleylib.ftc.input;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Xbox-layout command input helper with deadband and response-curve shaping.
 */
public class CommandXboxLike {

    private final BooleanSupplier a;
    private final BooleanSupplier b;
    private final BooleanSupplier x;
    private final BooleanSupplier y;
    private final BooleanSupplier leftBumper;
    private final BooleanSupplier rightBumper;
    private final BooleanSupplier dpadUp;
    private final BooleanSupplier dpadDown;
    private final BooleanSupplier dpadLeft;
    private final BooleanSupplier dpadRight;

    private final DoubleSupplier leftXRaw;
    private final DoubleSupplier leftYRaw;
    private final DoubleSupplier rightXRaw;
    private final DoubleSupplier rightYRaw;
    private final DoubleSupplier leftTriggerRaw;
    private final DoubleSupplier rightTriggerRaw;

    private double stickDeadband = 0.08;
    private double stickExponent = 1.0;
    private double triggerDeadband = 0.05;
    private double triggerExponent = 1.0;

    public CommandXboxLike(
            BooleanSupplier a,
            BooleanSupplier b,
            BooleanSupplier x,
            BooleanSupplier y,
            BooleanSupplier leftBumper,
            BooleanSupplier rightBumper,
            BooleanSupplier dpadUp,
            BooleanSupplier dpadDown,
            BooleanSupplier dpadLeft,
            BooleanSupplier dpadRight,
            DoubleSupplier leftXRaw,
            DoubleSupplier leftYRaw,
            DoubleSupplier rightXRaw,
            DoubleSupplier rightYRaw,
            DoubleSupplier leftTriggerRaw,
            DoubleSupplier rightTriggerRaw
    ) {
        this.a = a;
        this.b = b;
        this.x = x;
        this.y = y;
        this.leftBumper = leftBumper;
        this.rightBumper = rightBumper;
        this.dpadUp = dpadUp;
        this.dpadDown = dpadDown;
        this.dpadLeft = dpadLeft;
        this.dpadRight = dpadRight;
        this.leftXRaw = leftXRaw;
        this.leftYRaw = leftYRaw;
        this.rightXRaw = rightXRaw;
        this.rightYRaw = rightYRaw;
        this.leftTriggerRaw = leftTriggerRaw;
        this.rightTriggerRaw = rightTriggerRaw;
    }

    public CommandXboxLike withStickDeadband(double deadband) {
        this.stickDeadband = deadband;
        return this;
    }

    public CommandXboxLike withStickExponent(double exponent) {
        this.stickExponent = exponent;
        return this;
    }

    public CommandXboxLike withTriggerDeadband(double deadband) {
        this.triggerDeadband = deadband;
        return this;
    }

    public CommandXboxLike withTriggerExponent(double exponent) {
        this.triggerExponent = exponent;
        return this;
    }

    public Trigger a() { return new Trigger(a); }
    public Trigger b() { return new Trigger(b); }
    public Trigger x() { return new Trigger(x); }
    public Trigger y() { return new Trigger(y); }
    public Trigger leftBumper() { return new Trigger(leftBumper); }
    public Trigger rightBumper() { return new Trigger(rightBumper); }
    public Trigger dpadUp() { return new Trigger(dpadUp); }
    public Trigger dpadDown() { return new Trigger(dpadDown); }
    public Trigger dpadLeft() { return new Trigger(dpadLeft); }
    public Trigger dpadRight() { return new Trigger(dpadRight); }

    // PlayStation-style aliases for broader controller vocabulary.
    public Trigger cross() { return a(); }
    public Trigger circle() { return b(); }
    public Trigger square() { return x(); }
    public Trigger triangle() { return y(); }
    public Trigger l1() { return leftBumper(); }
    public Trigger r1() { return rightBumper(); }

    public Trigger leftTriggerButton(double threshold) {
        return new Trigger(() -> leftTrigger() >= threshold);
    }

    public Trigger rightTriggerButton(double threshold) {
        return new Trigger(() -> rightTrigger() >= threshold);
    }

    public double leftX() {
        return shapeSigned(leftXRaw.getAsDouble(), stickDeadband, stickExponent);
    }

    public double leftY() {
        return shapeSigned(leftYRaw.getAsDouble(), stickDeadband, stickExponent);
    }

    public double rightX() {
        return shapeSigned(rightXRaw.getAsDouble(), stickDeadband, stickExponent);
    }

    public double rightY() {
        return shapeSigned(rightYRaw.getAsDouble(), stickDeadband, stickExponent);
    }

    public double leftTrigger() {
        return shapeUnsigned(leftTriggerRaw.getAsDouble(), triggerDeadband, triggerExponent);
    }

    public double rightTrigger() {
        return shapeUnsigned(rightTriggerRaw.getAsDouble(), triggerDeadband, triggerExponent);
    }

    static double shapeSigned(double value, double deadband, double exponent) {
        double magnitude = Math.abs(value);
        if (magnitude <= deadband) {
            return 0.0;
        }

        double normalized = (magnitude - deadband) / (1.0 - deadband);
        double curved = Math.pow(normalized, exponent);
        return Math.copySign(curved, value);
    }

    static double shapeUnsigned(double value, double deadband, double exponent) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        if (clamped <= deadband) {
            return 0.0;
        }

        double normalized = (clamped - deadband) / (1.0 - deadband);
        return Math.pow(normalized, exponent);
    }
}
