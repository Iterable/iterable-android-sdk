package iterable.com.iterableapi;

import android.app.Dialog;
import android.view.View;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.iterable.iterableapi.IterableHelper;
import com.iterable.iterableapi.IterableInAppActionListener;

/**
 * IterableConstants tests the functionality in IterableInAppActionListener
 */
public class IterableInAppActionListenerUnitTest {

    @Test
    public void createInAppListener() throws Exception {
        final String resultString = "testString";
        IterableHelper.IterableActionHandler clickCallback = new IterableHelper.IterableActionHandler(){

            @Override
            public void execute(String result) {
                assertEquals(result, resultString);
            }
        };
        IterableInAppActionListener listener = new IterableInAppActionListener(null, 0, resultString, null, clickCallback);
        listener.onClick(new View(null));
    }
}