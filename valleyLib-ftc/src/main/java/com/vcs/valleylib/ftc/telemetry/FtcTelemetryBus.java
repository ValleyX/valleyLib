package com.vcs.valleylib.ftc.telemetry;

import com.bylazar.telemetry.PanelsTelemetry;
import com.bylazar.telemetry.TelemetryManager;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified telemetry system.
 * <p>
 * Sends the same data to:
 *  - FTC Driver Station telemetry
 *  - Panels telemetry dashboard
 * <p>
 * Prevents duplicated logging code and keeps outputs consistent.
 */
public class FtcTelemetryBus {

    private final Telemetry baseTelemetry;
    private final TelemetryManager panelsTelemetry;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public FtcTelemetryBus(Telemetry baseTelemetry, TelemetryManager panelsTelemetry) {
        this.baseTelemetry = baseTelemetry;
        this.panelsTelemetry = panelsTelemetry;
    }

    /**
     * Queues telemetry data for the current loop.
     *
     * @param key label
     * @param value value
     */
    public void put(String key, Object value) {
        data.put(key, value);
    }

    /**
     * Flushes all queued telemetry to outputs.
     * Call once per loop.
     */
    public void update() {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            baseTelemetry.addData(entry.getKey(), entry.getValue());
            panelsTelemetry.addData(entry.getKey(), entry.getValue());
        }
        baseTelemetry.update();
        panelsTelemetry.update();
        data.clear();
    }
}