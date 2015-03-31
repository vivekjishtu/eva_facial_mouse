package com.crea_si.eviacam.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

class ContextMenuView extends LinearLayout {
    
    private class ActionButton {
        final int action;
        final Button button;
        ActionButton (int a, Button b) { 
            action= a;
            button= b;
        }
    }
    
    // track last used mask to avoid unnecessary operations
    private int mActionsMask= 0;
    
    // references to actions and buttons pairs
    List<ActionButton> mActionButtons= new ArrayList<ActionButton>();
    
    public ContextMenuView(Context c) {
        super(c);
        setOrientation(LinearLayout.VERTICAL);
    }
    
    /*
     * create button, make invisible (gone), add to layout and store in 
     * collection for further reference 
     */
    public void populateAction (int action, int labelId) {
        Button b= new Button (getContext());
        b.setText(getResources().getString(labelId));
        b.setVisibility(View.GONE);
        
        addView(b);
        
        mActionButtons.add(new ActionButton(action, b));
    }
    
    void updateButtons (int actions) {
        if (actions == mActionsMask) return;
        
        for (ActionButton ab : mActionButtons) {
            if ((actions & ab.action) != 0) {
                ab.button.setVisibility(View.VISIBLE);
            }
            else {
                ab.button.setVisibility(View.GONE);                
            }
        }

        // measure how much space will need
        measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        
        mActionsMask= actions;
    }

    public int testClick (Point p)  {
        for (ActionButton ab : mActionButtons) {
            if (ab.button.getVisibility() == View.VISIBLE &&
                ViewUtils.isPointInsideView(p, ab.button)) {
                return ab.action;
            }
        }
       
        return 0;
    }
}
