# ValleyLib

ValleyLib is a command-based robotics library with:

- `valleyLib-core`: scheduler, command, and subsystem abstractions.
- `valleyLib-ftc`: FTC-specific integration layers and utilities.

## Documentation

- Custom command guide: [`docs/custom-commands.md`](docs/custom-commands.md)
- Codebase review and recommendations: [`docs/codebase-review.md`](docs/codebase-review.md)
- FTC trigger binding guide: [`docs/ftc-trigger-bindings.md`](docs/ftc-trigger-bindings.md)
- NextFTC-style depth upgrade notes: [`docs/nextftc-depth-upgrade.md`](docs/nextftc-depth-upgrade.md)
- Advanced controls, logging, and simulation: [`docs/advanced-controls-and-simulation.md`](docs/advanced-controls-and-simulation.md)
- Pedro command-based guide: [`docs/pedro-command-based.md`](docs/pedro-command-based.md)


## Compatibility and sample starters

Recent updates add broader compatibility helpers and starter building blocks:

- `AutoDsl` now includes alias methods (`add`, `doInstant`, `waitFor`, `ifElse`) so teams coming from other command DSLs can migrate with less renaming.
- `CommandXboxLike` includes PlayStation-style aliases (`cross`, `circle`, `square`, `triangle`, `l1`, `r1`).
- `CommandGamepad.forDualShockLike(gamepad)` adds a controller preset for PlayStation-style teams.
- Sample hardware classes:
  - `SampleDriveHardware`
  - `SampleIntakeHardware`
- Sample Pedro pathing auto routines:
  - `SampleAutos.simpleTaxi(drive, taxiPath)`
  - `SampleAutos.taxiAndCycle(drive, intake, taxiPath, cyclePath)`

See `valleyLib-ftc/src/main/java/com/vcs/valleylib/ftc/samples` for copy-ready templates built around `PedroAutoDsl` and `PedroCommands`.
