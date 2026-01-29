package com.iterable.iterableapi;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InAppOrientationServiceTest {

    private InAppOrientationService orientationService;

    @Before
    public void setup() {
        orientationService = new InAppOrientationService();
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn0_when0Degrees() {
        // Act
        int result = orientationService.roundToNearest90Degrees(0);

        // Assert
        assertEquals(0, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn0_when44Degrees() {
        // Arrange - 44 is closer to 0 than 90
        int orientation = 44;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(0, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn90_when45Degrees() {
        // Arrange - 45 is the boundary, rounds up to 90
        int orientation = 45;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(90, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn90_when90Degrees() {
        // Act
        int result = orientationService.roundToNearest90Degrees(90);

        // Assert
        assertEquals(90, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn90_when89Degrees() {
        // Arrange - 89 is closer to 90 than 0
        int orientation = 89;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(90, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn90_when134Degrees() {
        // Arrange - 134 is closer to 90 than 180
        int orientation = 134;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(90, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn180_when135Degrees() {
        // Arrange - 135 is the boundary, rounds up to 180
        int orientation = 135;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(180, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn180_when180Degrees() {
        // Act
        int result = orientationService.roundToNearest90Degrees(180);

        // Assert
        assertEquals(180, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn270_when225Degrees() {
        // Arrange - 225 is the boundary, rounds up to 270
        int orientation = 225;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(270, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn270_when270Degrees() {
        // Act
        int result = orientationService.roundToNearest90Degrees(270);

        // Assert
        assertEquals(270, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn270_when314Degrees() {
        // Arrange - 314 is closer to 270 than 360/0
        int orientation = 314;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(270, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn0_when315Degrees() {
        // Arrange - 315 is the boundary, rounds up to 360 which wraps to 0
        int orientation = 315;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(0, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldReturn0_when359Degrees() {
        // Arrange - 359 is very close to 360, which wraps to 0
        int orientation = 359;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert
        assertEquals(0, result);
    }

    // Edge Cases

    @Test
    public void roundToNearest90Degrees_shouldHandleNegativeValues() {
        // Arrange - negative values (although unusual for orientation)
        int orientation = -10;

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert - The modulo will handle this, expect 350 rounded
        // (-10 + 45) / 90 * 90 = 35 / 90 * 90 = 0 * 90 = 0, then % 360 = 0
        assertEquals(0, result);
    }

    @Test
    public void roundToNearest90Degrees_shouldHandleValuesOver360() {
        // Arrange - values over 360 (sensor may provide these)
        int orientation = 405; // 405 = 45 + 360

        // Act
        int result = orientationService.roundToNearest90Degrees(orientation);

        // Assert - (405 + 45) / 90 * 90 % 360 = 450 / 90 * 90 % 360 = 5 * 90 % 360 = 450 % 360 = 90
        assertEquals(90, result);
    }

    // Comprehensive boundary tests

    @Test
    public void roundToNearest90Degrees_allBoundaries() {
        // Test all 4 boundaries systematically
        assertEquals("Boundary at 45 degrees", 90, orientationService.roundToNearest90Degrees(45));
        assertEquals("Boundary at 135 degrees", 180, orientationService.roundToNearest90Degrees(135));
        assertEquals("Boundary at 225 degrees", 270, orientationService.roundToNearest90Degrees(225));
        assertEquals("Boundary at 315 degrees", 0, orientationService.roundToNearest90Degrees(315));
    }

    @Test
    public void roundToNearest90Degrees_allCardinalDirections() {
        // Test all 4 cardinal directions
        assertEquals("Portrait (0°)", 0, orientationService.roundToNearest90Degrees(0));
        assertEquals("Landscape right (90°)", 90, orientationService.roundToNearest90Degrees(90));
        assertEquals("Portrait inverted (180°)", 180, orientationService.roundToNearest90Degrees(180));
        assertEquals("Landscape left (270°)", 270, orientationService.roundToNearest90Degrees(270));
    }
}

