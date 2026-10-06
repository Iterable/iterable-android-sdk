package com.iterable.iterableapi;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.util.ReflectionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(TestRunner.class)
public class IterableTaskStorageTest extends BaseTest {
    private IterableTaskStorage storage;

    @Before
    public void setUp() {
        ReflectionHelpers.setStaticField(
                IterableTaskStorage.class,
                "sharedInstance",
                null
        );
        storage = IterableTaskStorage.sharedInstance(getContext());
        storage.deleteAllTasks();
    }

    @After
    public void tearDown() {
        storage.deleteAllTasks();
        ReflectionHelpers.setStaticField(
                IterableTaskStorage.class,
                "sharedInstance",
                null
        );
    }

    @Test
    public void equalScheduledTimesUseInsertionOrderAndClaimedTasksAreSkipped() {
        String firstId = storage.createTask("first", IterableTaskType.API, "{}");
        String secondId = storage.createTask("second", IterableTaskType.API, "{}");
        assertNotNull(firstId);
        assertNotNull(secondId);

        SQLiteDatabase database = ReflectionHelpers.getField(storage, "database");
        ContentValues scheduled = new ContentValues();
        scheduled.put(IterableTaskStorage.SCHEDULED_AT, 1000L);
        database.update(
                IterableTaskStorage.ITERABLE_TASK_TABLE_NAME,
                scheduled,
                null,
                null
        );

        assertEquals(firstId, storage.getNextScheduledTask().id);
        assertTrue(storage.markTaskProcessingIfAvailable(firstId));
        assertFalse(storage.markTaskProcessingIfAvailable(firstId));
        assertEquals(secondId, storage.getNextScheduledTask().id);

        assertTrue(storage.updateIsProcessing(firstId, false));
        assertEquals(firstId, storage.getNextScheduledTask().id);
    }
}
