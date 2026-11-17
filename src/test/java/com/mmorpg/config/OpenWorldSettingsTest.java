package com.mmorpg.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpenWorldSettingsTest {
    @Test void existingServersKeepTheirOriginalScaleAndSafeArea() {
        var settings = new Gson().fromJson("{}", GeneralConfig.class).openWorld();
        assertTrue(settings.isSafe(120, 160));
        assertFalse(settings.isSafe(201, 0));
        assertEquals(.5, settings.distanceRatio(1250, 0), 1e-9);
    }

    @Test void smallMapVillagesAndProgressionAreIndependentOfTheCentralCircle() {
        var settings = new Gson().fromJson("""
            {"mapRadius":400,"safeRadius":58,"safeAreas":[{"name":"Clairbois","x":-230,"z":45,"radius":51}]}
            """, OpenWorldSettings.class);
        assertTrue(settings.isSafe(58, 0));
        assertFalse(settings.isSafe(59, 0));
        assertTrue(settings.isSafe(-281, 45));
        assertFalse(settings.isSafe(-282, 45));
        assertFalse(settings.isSafe(-150, 100));
        assertEquals(.5, settings.distanceRatio(200, 0), 1e-9);
        assertEquals(1, settings.distanceRatio(399, 399));
    }

    @Test void displacedCentersWorkWithoutMovingVillageCoordinates() {
        var settings = new OpenWorldSettings();
        settings.centerX = 100; settings.centerZ = -50; settings.safeRadius = 10; settings.mapRadius = 400;
        assertTrue(settings.isSafe(106, -42));
        assertFalse(settings.isSafe(0, 0));
        assertEquals(.5, settings.distanceRatio(300, -50), 1e-9);
    }

    @Test void malformedOptionalSettingsDoNotDisableProtectionOrProduceNanLevels() {
        var settings = new OpenWorldSettings();
        settings.centerX = Double.NaN; settings.mapRadius = 0; settings.safeRadius = -1; settings.safeAreas = null;
        assertTrue(settings.isSafe(0, 0));
        assertFalse(settings.isSafe(201, 0));
        assertEquals(.5, settings.distanceRatio(1250, 0), 1e-9);
        var config = new Gson().fromJson("{\"openWorld\":null}", GeneralConfig.class);
        assertTrue(config.openWorld().isSafe(0, 0));
    }
}
