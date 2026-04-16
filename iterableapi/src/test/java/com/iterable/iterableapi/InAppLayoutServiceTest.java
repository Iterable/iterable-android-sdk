package com.iterable.iterableapi;

import static org.junit.Assert.assertEquals;

import android.view.Gravity;

import org.junit.Before;
import org.junit.Test;

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
    public void getInAppLayout_shouldReturnTop_whenBottomIsAutoExpand() {
        // Arrange - top=0, bottom=-1 (AutoExpand sentinel from decodePadding)
        InAppPadding padding = new InAppPadding(0, 0, 0, -1);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.TOP, result);
    }

    @Test
    public void getInAppLayout_shouldReturnBottom_whenTopIsAutoExpand() {
        // Arrange - top=-1 (AutoExpand), bottom=0
        InAppPadding padding = new InAppPadding(0, -1, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.BOTTOM, result);
    }

    @Test
    public void getInAppLayout_shouldReturnCenter_whenBothTopAndBottomHavePadding() {
        // Arrange - both have positive percentage padding
        InAppPadding padding = new InAppPadding(0, 50, 0, 50);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.CENTER, result);
    }

    @Test
    public void getInAppLayout_shouldReturnCenter_whenBothAutoExpand() {
        // Arrange - both are AutoExpand (-1)
        InAppPadding padding = new InAppPadding(0, -1, 0, -1);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.CENTER, result);
    }

    @Test
    public void getInAppLayout_shouldReturnCenter_whenPositivePaddingOnBothEdges() {
        // Arrange - both edges have positive percentage values
        InAppPadding padding = new InAppPadding(0, 100, 0, 100);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.CENTER, result);
    }

    // Vertical Location Tests (Business Logic - derives from layout type)

    @Test
    public void getVerticalLocation_shouldReturnTop_whenTopLayout() {
        // Arrange - top=0, bottom=-1 (AutoExpand) → TOP layout
        InAppPadding padding = new InAppPadding(0, 0, 0, -1);

        // Act
        int result = layoutService.getVerticalLocation(padding);

        // Assert
        assertEquals(Gravity.TOP, result);
    }

    @Test
    public void getVerticalLocation_shouldReturnBottom_whenBottomLayout() {
        // Arrange - top=-1 (AutoExpand), bottom=0 → BOTTOM layout
        InAppPadding padding = new InAppPadding(0, -1, 0, 0);

        // Act
        int result = layoutService.getVerticalLocation(padding);

        // Assert
        assertEquals(Gravity.BOTTOM, result);
    }

    @Test
    public void getVerticalLocation_shouldReturnCenterVertical_whenCenterLayout() {
        // Arrange - both have positive padding → CENTER layout
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
    public void getInAppLayout_shouldReturnBottom_whenTopIsNegativeAndBottomIsZero() {
        // Arrange - any negative top value with zero bottom → BOTTOM
        InAppPadding padding = new InAppPadding(0, -10, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.BOTTOM, result);
    }

    @Test
    public void getInAppLayout_shouldReturnCenter_whenTopIsPositiveAndBottomIsZero() {
        // Arrange - positive top with zero bottom: neither edge is auto-expand
        InAppPadding padding = new InAppPadding(0, 1000, 0, 0);

        // Act
        InAppLayoutService.InAppLayout result = layoutService.getInAppLayout(padding);

        // Assert
        assertEquals(InAppLayoutService.InAppLayout.CENTER, result);
    }
}

