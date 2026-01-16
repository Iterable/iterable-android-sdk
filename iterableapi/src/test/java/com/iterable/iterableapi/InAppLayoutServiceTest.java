package com.iterable.iterableapi;

import android.view.Gravity;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class InAppLayoutServiceTest {

    private InAppLayoutService layoutService;

    @Before
    public void setup() {
        layoutService = new InAppLayoutService();
    }

    @Test
    public void getInAppLayout_shouldReturnFullscreen_whenNoPadding() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 0, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.FULLSCREEN, result);
    }

    @Test
    public void getInAppLayout_shouldReturnTop_whenOnlyTopPadding() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 50, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.TOP, result);
    }

    @Test
    public void getInAppLayout_shouldReturnBottom_whenOnlyBottomPadding() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 0, 0, 50);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.BOTTOM, result);
    }

    @Test
    public void getInAppLayout_shouldReturnCenter_whenBothTopAndBottomPadding() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 50, 0, 50);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.CENTER, result);
    }

    @Test
    public void getInAppLayout_shouldReturnTop_whenTopPaddingAndBottomIsZero() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 100, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.TOP, result);
    }

    @Test
    public void getInAppLayout_shouldReturnBottom_whenBottomPaddingAndTopIsZero() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 0, 0, 100);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.BOTTOM, result);
    }

    // Vertical Location Tests (Business Logic - derives from layout type)

    @Test
    public void getVerticalLocation_shouldReturnTop_whenTopLayout() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 50, 0, 0);

        // Act
        int result = layoutService.getVerticalLocation(padding);

        // Assert
        assertEquals(Gravity.TOP, result);
    }

    @Test
    public void getVerticalLocation_shouldReturnBottom_whenBottomLayout() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 0, 0, 50);

        // Act
        int result = layoutService.getVerticalLocation(padding);

        // Assert
        assertEquals(Gravity.BOTTOM, result);
    }

    @Test
    public void getVerticalLocation_shouldReturnCenterVertical_whenCenterLayout() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 50, 0, 50);

        // Act
        int result = layoutService.getVerticalLocation(padding);

        // Assert
        assertEquals(Gravity.CENTER_VERTICAL, result);
    }

    @Test
    public void getVerticalLocation_shouldReturnCenterVertical_whenFullscreenLayout() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 0, 0, 0);

        // Act
        int result = layoutService.getVerticalLocation(padding);

        // Assert
        assertEquals(Gravity.CENTER_VERTICAL, result);
    }

    // Edge Cases

    @Test
    public void getInAppLayout_shouldHandleNegativePadding() {
        // Arrange - negative padding for top with zero bottom
        InAppPadding padding = new InAppPadding(0, -10, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        // top <= 0 but bottom not > 0, so it falls through to CENTER
        assertEquals(InAppLayoutService.InAppLayout.CENTER, result);
    }

    @Test
    public void getInAppLayout_shouldHandleLargePaddingValues() {
        // Arrange
        InAppPadding padding = new InAppPadding(0, 1000, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.TOP, result);
    }
}

