package com.vcs.valleylib.ftc.input;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * FTC Gamepad implementation for Xbox-layout controllers.
 *
 * Logitech F310 in XInput mode maps naturally to this layout.
 */
public class CommandGamepad extends CommandXboxLike {

    public CommandGamepad(Gamepad gamepad) {
        super(
                () -> gamepad.a,
                () -> gamepad.b,
                () -> gamepad.x,
                () -> gamepad.y,
                () -> gamepad.left_bumper,
                () -> gamepad.right_bumper,
                () -> gamepad.dpad_up,
                () -> gamepad.dpad_down,
                () -> gamepad.dpad_left,
                () -> gamepad.dpad_right,
                () -> gamepad.left_stick_x,
                () -> gamepad.left_stick_y,
                () -> gamepad.right_stick_x,
                () -> gamepad.right_stick_y,
                () -> gamepad.left_trigger,
                () -> gamepad.right_trigger
        );
    }

    public static CommandGamepad forLogitechF310(Gamepad gamepad) {
        return (CommandGamepad) new CommandGamepad(gamepad)
                .withStickDeadband(0.08)
                .withStickExponent(1.7)
                .withTriggerDeadband(0.05)
                .withTriggerExponent(1.5);
    }


    /**
     * Preset for PlayStation-style drivers using the same FTC gamepad mapping.
     */
    public static CommandGamepad forDualShockLike(Gamepad gamepad) {
        return (CommandGamepad) new CommandGamepad(gamepad)
                .withStickDeadband(0.07)
                .withStickExponent(1.6)
                .withTriggerDeadband(0.04)
                .withTriggerExponent(1.4);
    }

}
