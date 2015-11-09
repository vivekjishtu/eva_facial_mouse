/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.crea_si.eviacam.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;

import com.crea_si.eviacam.EVIACAM;
import com.crea_si.eviacam.R;
import com.crea_si.input_method_aidl.IClickableIME;

/**
 * Handles the communication with the IME
 */

public class InputMethodAction implements ServiceConnection {
    
    private static final String REMOTE_PACKAGE= "com.crea_si.eviacam.service";
    private static final String REMOTE_ACTION= "com.crea_si.softkeyboard.RemoteBinderService";
    private static final String IME_NAME= REMOTE_PACKAGE + "/com.crea_si.softkeyboard.SoftKeyboard";
    
    // period (in milliseconds) to try to rebind again to the IME
    private static final int BIND_RETRY_PERIOD = 2000;
    
    private final Context mContext;
    
    private final InputMethodManager mInputMethodManager;
    
    // binder (proxy) with the remote input method service
    private IClickableIME mRemoteService;
    
    // time stamp of the last time the thread ran
    private long mLastBindAttemptTimeStamp = 0;

    private final Handler mHandler= new Handler();

    public InputMethodAction(Context c) {
        mContext= c;
        
        mInputMethodManager= (InputMethodManager) 
                c.getSystemService (Context.INPUT_METHOD_SERVICE);

        // attempt to bind with IME
        keepBindAlive();
    }
    
    public void cleanup() {
        if (mRemoteService == null) return;
        
        mContext.unbindService(this);
        mRemoteService= null;
    }
    
    /**
     * Bind to the remote IME when needed
     * 
     * TODO: support multiple compatible IMEs
     * TODO: provide feedback to the user 
     */
    private void keepBindAlive() {
        if (mRemoteService != null) return;
        
        /**
         * no bind available, try to establish it if enough 
         * time passed since the last attempt
         */
        long tstamp= System.currentTimeMillis();
        
        if (tstamp - mLastBindAttemptTimeStamp < BIND_RETRY_PERIOD) {
            return;
        }

        mLastBindAttemptTimeStamp = tstamp;
        
        EVIACAM.debug("Attempt to bind to remote IME");
        Intent intent= new Intent(REMOTE_ACTION);
        intent.setPackage(REMOTE_PACKAGE);
        try {
            if (!mContext.bindService(intent, this, Context.BIND_AUTO_CREATE)) {
                EVIACAM.debug("Cannot bind remote IME");
            }
        }
        catch(SecurityException e) {
            EVIACAM.debug("Cannot bind remote IME. Security exception.");
        }
    }
    
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.
        EVIACAM.debug("remoteIME:onServiceConnected: " + className.toString());
        mRemoteService = IClickableIME.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        EVIACAM.debug("remoteIME:onServiceDisconnected");
        mContext.unbindService(this);
        mRemoteService = null;
        keepBindAlive();
    }
    
    public boolean click(int x, int y) {
        if (mRemoteService == null) {
            EVIACAM.debug("InputMethodAction: click: no remote service available");
            return false;
        }
        //if (!mInputMethodManager.isActive()) return false;
        
        try {
            return mRemoteService.click(x, y);
        } catch (RemoteException e) {
            return false;
        }
    }
    
    public void openIME() {
        if (!isEnabledCustomKeyboard(mContext)) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    EVIACAM.Toast(mContext, R.string.keyboard_not_enabled_toast);
                }
            });
        }

        if (mRemoteService == null) {
            EVIACAM.debug("InputMethodAction: openIME: no remote service available");
            keepBindAlive();
            return;
        }
        //if (mInputMethodManager.isActive()) return; // already open
        try {
            mRemoteService.openIME();
        } catch (RemoteException e) {
            // Nothing to be done
            EVIACAM.debug("InputMethodAction: exception while trying to open IME");
        }
    }

    public void closeIME() {
        if (mRemoteService == null) {
            EVIACAM.debug("InputMethodAction: closeIME: no remote service available");
            keepBindAlive();
            return;
        }
        // Does not check mInputMethodManager.isActive because does not mean IME is open
        try {
            mRemoteService.closeIME();
        } catch (RemoteException e) {
            // Nothing to be done
            EVIACAM.debug("InputMethodAction: exception while trying to close IME");
        }
    }

    /**
     * Check if the custom keyboard is enabled and is the default one
     * @param c context
     * @return true if enabled
     */
    public static boolean isEnabledCustomKeyboard (Context c) {
        InputMethodManager imem =
                (InputMethodManager) c.getSystemService(Context.INPUT_METHOD_SERVICE);

        String pkgName= Settings.Secure.getString(c.getContentResolver(),
                                                  Settings.Secure.DEFAULT_INPUT_METHOD);
        return pkgName.contentEquals(IME_NAME);
    }
}
