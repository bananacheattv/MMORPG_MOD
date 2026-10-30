package com.mmorpg.replay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayExportPlanTest {
    @Test void fiveMinutesAtSixtyFpsIs18000Frames(){assertEquals(18000,ReplayExportPlan.frames(300,0,1,300));}
    @Test void slowMotionUsesOutputDurationAndNeverReadsPastEnd(){
        assertEquals(18000,ReplayExportPlan.frames(300,0,.25,300));
        assertEquals(2400,ReplayExportPlan.frames(300,290,.25,300));
        assertEquals(600,ReplayExportPlan.frames(300,290,1,300));
        assertEquals(0,ReplayExportPlan.frames(300,300,1,300));
        assertEquals(87,ReplayExportPlan.frames(1.45,0,1,300));
    }
    @Test void refusesInvalidDurationsAndNonFiniteTime(){
        assertThrows(IllegalArgumentException.class,()->ReplayExportPlan.frames(300,0,0,300));
        assertThrows(IllegalArgumentException.class,()->ReplayExportPlan.frames(Double.NaN,0,1,300));
        assertThrows(IllegalArgumentException.class,()->ReplayExportPlan.frames(300,0,1,301));
    }
    @Test void streamBudgetDoesNotRequire18000UncompressedPngs(){
        long stream=ReplayExportPlan.diskBudget(1920,1080,18000,true),png=ReplayExportPlan.diskBudget(1920,1080,18000,false);
        assertTrue(stream<png/30);assertTrue(stream>64L*1024*1024);
    }
}
