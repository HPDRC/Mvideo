package com.itpa.mvideo.misc;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class Pinpad {

    public interface PinpadCallback {
        void onAllPinCompleted(String code);
    }

    private String code = "";
    private ArrayList<TextView> pinBoxes;
    private TextView pinTitle;
    private String titleText;

    public Pinpad(final ArrayList<Button> pinButtons, final ArrayList<TextView> pinBoxes, final TextView pinTitle, final Button delButton, final PinpadCallback callback) {
        final String deleteText = delButton.getText().toString();
        this.pinBoxes = pinBoxes;
        this.pinTitle = pinTitle;
        this.titleText = pinTitle.getText().toString();
        updateDisplay();
        for (Button btn: pinButtons)
        {
            btn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View view)
                {
                    // update code
                    Button button = (Button) view;
                    String text = button.getText().toString();
                    if (text.equals(deleteText)) {
                        if (code.length() > 0)
                            code = code.substring(0, code.length() - 1);
                    } else {
                        int x = Integer.parseInt(text);
                        if (code.length() < pinBoxes.size()) {
                            code += Integer.toString(x);
                        }
                    }

                    if (code.length() >= pinBoxes.size()) {
                        callback.onAllPinCompleted(code);
                        code = "";
                    }

                    // update display
                    updateDisplay();
                }
            });
        }
    }

    private void updateDisplay() {
        int pinBoxCount = pinBoxes.size();
        for (int i=0; i<code.length(); i++) {
            pinBoxes.get(i).setText("*");
        }
        for (int i=code.length(); i<pinBoxCount; i++) {
            pinBoxes.get(i).setText("");
        }
        pinTitle.setText(code.length() == 0 ? titleText : "");
    }

    public void clear() {
        code = "";
        updateDisplay();
    }
}
