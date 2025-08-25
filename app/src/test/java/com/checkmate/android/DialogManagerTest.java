package com.checkmate.android;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;

import com.checkmate.android.util.DialogManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the DialogManager to ensure no window leaks
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DialogManagerTest {

    @Mock
    private Activity mockActivity;

    @Mock
    private AlertDialog mockDialog;

    private DialogManager dialogManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockActivity.isFinishing()).thenReturn(false);
        when(mockActivity.isDestroyed()).thenReturn(false);
        
        dialogManager = new DialogManager(mockActivity);
    }

    @Test
    public void testDialogManagerInitialization() {
        assertNotNull("Dialog manager should be initialized", dialogManager);
        assertTrue("Activity should be valid initially", dialogManager.isActivityValid());
        assertFalse("Should have no active dialogs initially", dialogManager.hasActiveDialogs());
    }

    @Test
    public void testActivityStateValidation() {
        assertTrue("Activity should be valid", dialogManager.isActivityValid());
        
        when(mockActivity.isFinishing()).thenReturn(true);
        assertFalse("Activity should be invalid when finishing", dialogManager.isActivityValid());
    }

    @Test
    public void testDialogTracking() {
        when(mockDialog.isShowing()).thenReturn(true);
        
        dialogManager.showDialog("test_dialog", mockDialog);
        assertTrue("Should have active dialogs", dialogManager.hasActiveDialogs());
        assertEquals("Should have one active dialog", 1, dialogManager.getActiveDialogCount());
        
        dialogManager.dismissDialog("test_dialog");
        assertFalse("Should have no active dialogs after dismissal", dialogManager.hasActiveDialogs());
    }

    @Test
    public void testCleanupPreventsWindowLeaks() {
        when(mockDialog.isShowing()).thenReturn(true);
        
        dialogManager.showDialog("test_dialog", mockDialog);
        assertTrue("Should have active dialogs", dialogManager.hasActiveDialogs());
        
        dialogManager.cleanup();
        assertFalse("Should have no active dialogs after cleanup", dialogManager.hasActiveDialogs());
        assertFalse("Activity should be invalid after cleanup", dialogManager.isActivityValid());
    }

    @Test
    public void testDismissAllDialogs() {
        when(mockDialog.isShowing()).thenReturn(true);
        
        dialogManager.showDialog("dialog1", mockDialog);
        dialogManager.showDialog("dialog2", mockDialog);
        assertEquals("Should have two active dialogs", 2, dialogManager.getActiveDialogCount());
        
        dialogManager.dismissAllDialogs();
        assertFalse("Should have no active dialogs", dialogManager.hasActiveDialogs());
    }
}