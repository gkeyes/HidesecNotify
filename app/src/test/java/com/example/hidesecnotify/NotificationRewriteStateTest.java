package com.example.hidesecnotify;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NotificationRewriteStateTest {
    @Test
    public void tracksOnlyTheBuilderWhoseSecondSpaceTextWasRewritten() {
        NotificationRewriteState state = new NotificationRewriteState();
        Object secondSpaceBuilder = new Object();
        Object unrelatedAppBuilder = new Object();

        state.updateField(secondSpaceBuilder, "setContentText", true);

        assertFalse(state.hasRewrittenField(unrelatedAppBuilder));
        assertTrue(state.hasRewrittenField(secondSpaceBuilder));

        state.updateField(secondSpaceBuilder, "setContentText", false);
        assertFalse(state.hasRewrittenField(secondSpaceBuilder));
    }

    @Test
    public void keepsOtherRewrittenFieldsWhenOneFieldChanges() {
        NotificationRewriteState state = new NotificationRewriteState();
        Object secondSpaceBuilder = new Object();

        state.updateField(secondSpaceBuilder, "setContentTitle", true);
        state.updateField(secondSpaceBuilder, "setContentText", false);

        assertTrue(state.hasRewrittenField(secondSpaceBuilder));
    }
}
