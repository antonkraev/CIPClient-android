package com.promomark.cipclient;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;

public class ButtonBar implements OnClickListener {
	List<ImageButton> buttons;
	List<ImageButton> buttonsSelected;
	int selected;
	OnSelectionChanged listener;
	
	public interface OnSelectionChanged {
		void selected(int index);
	}
	
	public ButtonBar(ViewGroup layout, OnSelectionChanged listener) {
		this.listener = listener;
		
		buttons = new ArrayList<ImageButton>();
		buttonsSelected = new ArrayList<ImageButton>();
		
		for (int i=0; i<layout.getChildCount(); i++) {
			ViewGroup child = (ViewGroup) layout.getChildAt(i);
			
			ImageButton button = (ImageButton) child.getChildAt(0);
			button.setEnabled(false);
			button.setOnClickListener(this);
			buttons.add(button);
			
			ImageButton buttonSelected = (ImageButton) child.getChildAt(1);
			buttonSelected.setEnabled(false);
			buttonSelected.setOnClickListener(this);
			buttonsSelected.add(buttonSelected);
		}
		
		setSelected(selected = 0);
	}
	
	public void setEnabled(int index, boolean enabled) {
		buttons.get(index).setEnabled(enabled);
	}
	
	public void setSelected(int index) {
		this.selected = index;
		
		for (int i=0; i<buttons.size(); i++) {
			buttons.get(i).setVisibility((i == index)? View.GONE: View.VISIBLE);
			buttonsSelected.get(i).setVisibility((i == index)? View.VISIBLE: View.GONE);
		}
	}
	
	@Override
	public void onClick(View v) {
		for (int i=0; i<buttons.size(); i++) {
			if (v == buttons.get(i) || v == buttonsSelected.get(i)) {
				setSelected(i);
				listener.selected(i);
				break;
			}
		}
	}
}
