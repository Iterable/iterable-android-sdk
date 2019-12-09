package com.iterable.iterableapi;

import com.iterable.iterableapi.unit.TestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(TestRunner.class)
public class IterableInAppFileStorageTest {

    @Test
    public void testInAppPersistence() throws Exception {
        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        IterableInAppMessage testInAppMessage = InAppTestUtils.getTestInAppMessage();

        assertEquals(0, storage.getMessages().size());

        storage.addMessage(testInAppMessage);
        assertEquals(1, storage.getMessages().size());
        assertEquals(storage.getMessage(testInAppMessage.getMessageId()), testInAppMessage);
        testInAppMessage.setProcessed(true);
        testInAppMessage.setConsumed(true);

        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);

        assertEquals(1, storage.getMessages().size());
        JSONAssert.assertEquals(testInAppMessage.toJSONObject(), storage.getMessages().get(0).toJSONObject(), JSONCompareMode.STRICT);
        assertTrue(storage.getMessages().get(0).isProcessed());
        assertTrue(storage.getMessages().get(0).isConsumed());

        storage.removeMessage(storage.getMessage(testInAppMessage.getMessageId()));
        assertEquals(0, storage.getMessages().size());
        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(0, storage.getMessages().size());
    }

    @Test
    public void testInAppImplicitSave() throws Exception {
        // Persist a message
        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        IterableInAppMessage testInAppMessage = InAppTestUtils.getTestInAppMessage();
        storage.addMessage(testInAppMessage);

        // Test that the message attributes are stored properly without an explicit save call
        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(1, storage.getMessages().size());
        testInAppMessage = storage.getMessages().get(0);
        testInAppMessage.setProcessed(true);
        testInAppMessage.setConsumed(true);

        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(1, storage.getMessages().size());
        testInAppMessage = storage.getMessages().get(0);
        JSONAssert.assertEquals(testInAppMessage.toJSONObject(), storage.getMessages().get(0).toJSONObject(), JSONCompareMode.STRICT);
        assertTrue(storage.getMessages().get(0).isProcessed());
        assertTrue(storage.getMessages().get(0).isConsumed());

        storage.removeMessage(storage.getMessage(testInAppMessage.getMessageId()));
        assertEquals(0, storage.getMessages().size());
        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(0, storage.getMessages().size());
    }

    @Test
    public void testHTMLForMigration() throws Exception {
        File folder = IterableUtil.getSdkCacheDir(RuntimeEnvironment.application);
        assertTrue(folder.isDirectory());
        File oldJsonStorageFile = new File(folder, "itbl_inapp.json");
        assertTrue(!oldJsonStorageFile.exists());

        Boolean fileWriteOperation = IterableUtil.writeFile(oldJsonStorageFile, IterableTestUtils.getResourceString("inapp_payload_multiple.json"));
        assertTrue(fileWriteOperation);

        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        List<IterableInAppMessage> messages = storage.getMessages();
        IterableInAppMessage message = messages.get(0);
        String messageID1 = message.getMessageId();
        String messageID2 = messages.get(1).getMessageId();
        
        message.setProcessed(true);
        storage = new IterableInAppFileStorage(RuntimeEnvironment.application);

        assertNotNull(storage.getHTML(messageID1));
        assertEquals(message.getContent().html,storage.getHTML(messageID1));
        assertNotNull(storage.getHTML(messageID2));
    }

    @Test
    public void loadMessagesWithNoJson() {
        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(0, storage.getMessages().size());
    }

    @Test
    public void loadMessagesWithJsonInCache() throws Exception {
        File folder = IterableUtil.getSdkCacheDir(RuntimeEnvironment.application);
        File oldJsonStorageFile = new File(folder, "itbl_inapp.json");
        Boolean fileWriteResult = IterableUtil.writeFile(oldJsonStorageFile, IterableTestUtils.getResourceString("inapp_payload_multiple.json"));
        assertTrue(fileWriteResult);
        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertTrue(storage.getMessages().size() > 1);

        // Simulate message update and check if new json file is created in SDK directory
        storage.onInAppMessageChanged(storage.getMessages().get(0));
        File sdkFilesDirectory = IterableUtil.getSDKFilesDirectory(RuntimeEnvironment.application);
        File inAppDirectory = IterableUtil.getDirectory(sdkFilesDirectory, "IterableInAppFileStorage");
        File inAppJsonFile = new File(inAppDirectory, "itbl_inapp.json");
        assertTrue(inAppJsonFile.exists());
    }

    @Test
    public void loadMessagesWithJsonInFilesDirectory() throws Exception {
        File sdkFilesDirectory = IterableUtil.getSDKFilesDirectory(RuntimeEnvironment.application);
        File inAppDirectory = IterableUtil.getDirectory(sdkFilesDirectory, "IterableInAppFileStorage");
        File inAppJsonFile = new File(inAppDirectory, "itbl_inapp.json");
        Boolean fileWriteResult = IterableUtil.writeFile(inAppJsonFile, IterableTestUtils.getResourceString("inapp_payload_single.json"));
        assertTrue(fileWriteResult);

        IterableInAppFileStorage storage = new IterableInAppFileStorage(RuntimeEnvironment.application);
        assertEquals(1, storage.getMessages().size());
    }

}
