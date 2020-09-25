package com.example.currencyconverter;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    EditText gbpView;
    EditText usdView;
    CardView buttonContainer;
    TextView conversionView;

    View focusedView = null;

    ArrayList<EditText> editTexts = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialise field
        gbpView = (EditText) findViewById(R.id.gbp_view);
        usdView = (EditText) findViewById(R.id.usd_view);
        editTexts.add(gbpView);
        editTexts.add(usdView);

        buttonContainer = (CardView) findViewById(R.id.buttonContainer);
        conversionView = (TextView) findViewById(R.id.conversion);
        gbpView.setOnEditorActionListener(editorListener);
        usdView.setOnEditorActionListener(editorListener);


        // Set focus listener
        // Initialise tag to 0 for no edit text listener
        for (EditText view : editTexts){
            view.setOnFocusChangeListener(focusListener);
            view.setTag(0);
        }
    }

    // Listen for focus change
    // tag = 1 has listener
    // tag = 0 no listener
    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus){
                focusedView = v;
                if (gbpView.hasFocus()) {
                    if (usdView.getTag().equals(1)){
                    usdView.removeTextChangedListener(textWatcher);
                    usdView.setTag(0);
                    }
                    gbpView.setText("");
                    gbpView.addTextChangedListener(textWatcher);
                    gbpView.setTag(1);
                    Log.i("info", "focus gbp");
                }
                if (usdView.hasFocus()) {
                    if (gbpView.getTag().equals(1)){
                        gbpView.removeTextChangedListener(textWatcher);
                        gbpView.setTag(0);
                    }
                    usdView.setText("");
                    usdView.addTextChangedListener(textWatcher);
                    usdView.setTag(1);
                    Log.i("info", "focus usd");
                }
            } else {
                focusedView  = null;
                Log.i("info", "focus null");
            }
        }
    };


    protected TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void afterTextChanged(Editable s) {
            // Change in the opposite view
            if (gbpView.hasFocus()) {
                Log.i("info", "converting gbp...");
                convertCurrency(usdView);
            } else if (usdView.hasFocus()) {
                Log.i("info", "converting usd...");
                convertCurrency(gbpView);
            }
        }
    };


    // Listener on text edit
    private void inputListener(EditText editText){
//        if (editText.hasFocus()) {
            editText.addTextChangedListener(textWatcher);
//        }
    }


    // Make the go button do trigger conversion
    private TextView.OnEditorActionListener editorListener = new TextView.OnEditorActionListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            switch (actionId) {
                case EditorInfo
                        .IME_ACTION_GO:
                    convertCurrency(textView);
            }
            return false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void convertButtonPress(View view) {
        convertCurrency(view);
        hideKeyboard(view);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void convertCurrency(View view) {
        ArrayList<String> errors = new ArrayList<String>();
        double conversionRate = 1.27343 ;
        EditText focusedEditText = null;

        if (gbpView.hasFocus()){
            focusedEditText = gbpView;
        }
        else if (usdView.hasFocus()){
            focusedEditText = usdView;
        }

        // Check if field empty
        if (focusedEditText.getText().toString().trim().equals("")){
            errors.add("No amount entered.");
//            conversionView.setText("Error: " + errors.get(0));
//            conversionView.setBackground(getDrawable(R.drawable.rect_error));
            if (focusedEditText.equals(gbpView)){
                usdView.setText("Error: " + errors.get(0));
            }
            else if (focusedEditText.equals(usdView)) {
                gbpView.setText("Error: " + errors.get(0));
            }
//            usdView.setBackground(getDrawable(R.drawable.rect_error));
            Log.i("info", "Error: No amount.");
        }
        else {
            double inputAmount = Double.parseDouble(focusedEditText.getText().toString());
            double convertedAmount = inputAmount * conversionRate;
            if (focusedEditText.equals(gbpView)){
                usdView.setText(formatNumber(2, convertedAmount));
            }
            else if (focusedEditText.equals(usdView)) {
                gbpView.setText(formatNumber(2, convertedAmount));
            }
//            conversionView.setText("£" + formatNumber(2, gbp) + " ⇄ " + "$" + formatNumber(2, convertedUSD));
//            conversionView.setBackgroundColor(getColor(R.color.colorAccent));
//            usdView.setText(formatNumber(2, convertedAmount));
//            hideKeyboard(view);
        }

        removeShadow();
    }

    /*Format money*/
    public String formatNumber(int decimals, double number) {
        StringBuilder sb = new StringBuilder(decimals + 2);
        sb.append("#.");
        for(int i = 0; i < decimals; i++) {
            sb.append("0");
        }
        return new DecimalFormat(sb.toString()).format(number);
    }


    /*Hides keyboard*/
    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(),0);
    }


    public void removeShadow() {
        buttonContainer.setCardElevation(2);

        //PAUSE for 0.2s
        // so requires a handler for this process.
        (new Handler()).postDelayed(() -> {
            buttonContainer.setCardElevation(6); // float 5 is 4.99 so go 6
        }, 200);
    }
}