package com.example.currencyconverter;

import android.os.AsyncTask;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import static java.lang.Double.parseDouble;

public class MainActivity extends AppCompatActivity {
    Double conversionRate = 1.0;
    Double conversionTop = 1.0;
    Double conversionBot = 1.0;

    ArrayList<String> errors = new ArrayList<String>();
    Double convertedAmount = 1.0;
    EditText focusedEditText = null;

    EditText gbpView;
    EditText usdView;
    CardView buttonContainer;
//    TextView conversionView;

    Spinner spinnerTop;
    Spinner spinnerBot;

    View focusedView = null;

    ArrayList<EditText> editTexts = new ArrayList<>();
    ArrayList<Spinner> spinners = new ArrayList<>();
    ArrayList<String> currencies = new ArrayList<>();
    ArrayList<Double> rates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialise API with base GBP
        PullFromAPI pullFromAPI = new PullFromAPI();
        // based on gbp
        pullFromAPI.execute("https://api.exchangeratesapi.io/latest?base=GBP");

        // Initialise fields
        spinnerTop = (Spinner) findViewById(R.id.gbp_dropdown);
        spinnerBot = (Spinner) findViewById(R.id.usd_dropdown2);
        spinners.add(spinnerTop);
        spinners.add(spinnerBot);

        gbpView = (EditText) findViewById(R.id.gbp_view);
        usdView = (EditText) findViewById(R.id.usd_view);
        editTexts.add(gbpView);
        editTexts.add(usdView);

        buttonContainer = (CardView) findViewById(R.id.buttonContainer);
        gbpView.setOnEditorActionListener(editorListener);
        usdView.setOnEditorActionListener(editorListener);


        // Set focus listener
        // Initialise tag to 0 for no edit text listener
        for (EditText view : editTexts) {
            view.setOnFocusChangeListener(focusListener);
            view.setTag(0);
        }

        convertCurrency();
    }

    public Double getConversionRate() {
        return conversionRate;
    }

    public void setConversionRate(double conversionRate) {
        this.conversionRate = conversionRate;
    }

    public Double getConversionTop() {
        return conversionTop;
    }

    public void setConversionTop(double conversionTop) {
        this.conversionTop = conversionTop;
    }

    public Double getConversionBot() {
        return conversionBot;
    }

    public void setConversionBot(double conversionBot) {
        this.conversionBot = conversionBot;
    }

    // Listen for focus change
    // tag = 1 has listener
    // tag = 0 no listener
    private View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus) {
                focusedView = v;
                if (gbpView.hasFocus()) {
                    if (usdView.getTag().equals(1)) {
                        usdView.removeTextChangedListener(textWatcher);
                        usdView.setTag(0);
                    }
                    gbpView.setText("");
                    gbpView.addTextChangedListener(textWatcher);
                    gbpView.setTag(1);
                    Log.i("info", "focus gbp");
                }
                if (usdView.hasFocus()) {
                    if (gbpView.getTag().equals(1)) {
                        gbpView.removeTextChangedListener(textWatcher);
                        gbpView.setTag(0);
                    }
                    usdView.setText("");
                    usdView.addTextChangedListener(textWatcher);
                    usdView.setTag(1);
                    Log.i("info", "focus usd");
                }
            } else {
                focusedView = null;
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

        @Override
        public void afterTextChanged(Editable s) {
            // Change in the opposite view
            if (gbpView.hasFocus()) {
                Log.i("info", "converting gbp...");
                convertCurrency();
            } else if (usdView.hasFocus()) {
                Log.i("info", "converting usd...");
                convertCurrency();
            }
        }
    };


    // Listener on text edit
    private void inputListener(EditText editText) {
//        if (editText.hasFocus()) {
        editText.addTextChangedListener(textWatcher);
//        }
    }


    // Make the go button do trigger conversion
    private TextView.OnEditorActionListener editorListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            switch (actionId) {
                case EditorInfo
                        .IME_ACTION_GO:
                    convertCurrency();
            }
            return false;
        }
    };

    public void convertButtonPress(View view) {
        convertCurrency();
        hideKeyboard(view);
        removeShadow();
    }

    public void convertCurrency() {
        // Define conversion rate depending on focused editText
        if (gbpView.hasFocus()) {
            focusedEditText = gbpView;
        } else if (usdView.hasFocus()) {
            focusedEditText = usdView;
        }

        // If there is a focused field
        if (focusedEditText != null) {
            // Check if field empty
            if (focusedEditText.getText().toString().trim().equals("")) {
                errors.add("No amount entered.");
                if (focusedEditText.equals(gbpView)) {
                    usdView.setText("Error: " + errors.get(0));
                } else if (focusedEditText.equals(usdView)) {
                    gbpView.setText("Error: " + errors.get(0));
                }
                Log.i("info", "Error: No amount.");
            } else {
                double inputAmount = parseDouble(focusedEditText.getText().toString());
                if (focusedEditText.equals(gbpView)) {
                    convertedAmount = inputAmount * (getConversionBot() / getConversionTop());
                    usdView.setText(formatNumber(2, convertedAmount));
                } else if (focusedEditText.equals(usdView)) {
                    convertedAmount = inputAmount * (getConversionTop() / getConversionBot());
                    gbpView.setText(formatNumber(2, convertedAmount));
                }
            }
        }
        // if focus is null
        else {
            // On startup do 1GBP to USD
            if (gbpView.getText().toString().equals("") && usdView.getText().toString().equals("")) {
//                gbpView.setText("1.00");
//                convertedAmount = 1.00 * (getConversionBot() / getConversionTop());
//                usdView.setText(formatNumber(2, convertedAmount));
            }
        }
        if (errors.size() > 0) {
            for (String error : errors) {
                Log.i("errors", errors.get(errors.indexOf(error)));
            }
        }
    }

    /*Format money*/
    public String formatNumber(int decimals, double number) {
        StringBuilder sb = new StringBuilder(decimals + 2);
        sb.append("#.");
        for (int i = 0; i < decimals; i++) {
            sb.append("0");
        }
        return new DecimalFormat(sb.toString()).format(number);
    }
    /*Hides keyboard*/
    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
    }


    public void removeShadow() {
        buttonContainer.setCardElevation(2);

        //PAUSE for 0.2s
        // so requires a handler for this process.
        (new Handler()).postDelayed(() -> {
            buttonContainer.setCardElevation(6); // float 5 is 4.99 so go 6
        }, 200);
    }


    private class PullFromAPI extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpsURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpsURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }
                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                JSONObject jsonObject = new JSONObject(result);

                // JSON
                JSONObject ratesJSON = jsonObject.getJSONObject("rates");

                // JSON -> String for logs
                String rateString = jsonObject.getString("rates");

                // Logs
                Log.i("Rate info", rateString);

                // Loop through JSON
                Iterator<String> keys = ratesJSON.keys();
                while (keys.hasNext()) {
                    try {
                        // define key (currency)
                        String key = keys.next();
                        currencies.add(key);
                        // define value (rate)
                        String value = ratesJSON.get(key).toString();
                        rates.add(parseDouble(value));

                        // Adapter for resulting data
                        ArrayAdapter<String> arrayAdapterCurrency = new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_item, currencies);

                        // Create onselectlistener
                        for (Spinner spinner : spinners) {
                            String defaultCurrencyTop = "GBP";
                            String defaultCurrencyBot = "USD";
                            spinner.setAdapter(arrayAdapterCurrency);
                            // If top spinner default to GBP
                            if (spinner.equals(spinnerTop)) {
                                spinner.setSelection(currencies.indexOf("GBP"));
                                setConversionTop(rates.indexOf(currencies.indexOf("GBP")));
                                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                        Log.i("selected top", adapterView.getItemAtPosition(i).toString());
                                        // get corresponding rate at same position in list
                                        setConversionTop(rates.get(i));
                                        Log.i("ConversionTop", getConversionTop().toString());
                                        convertCurrency();
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> adapterView) {
                                        Log.i("selected top", "Nothing selected.");
                                    }
                                });
                            }
                            // If bottom spinner default to USD
                            else {
                                spinner.setSelection(currencies.indexOf("USD"));
                                setConversionBot(rates.indexOf(currencies.indexOf("USD")));
                                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                                        Log.i("selected bot", adapterView.getItemAtPosition(i).toString());
                                        // get corresponding rate at same position in list
                                        setConversionBot(rates.get(i));
                                        Log.i("ConversionBot", getConversionBot().toString());
                                        convertCurrency();
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> adapterView) {
                                        Log.i("selected top", "Nothing selected.");
                                    }
                                });
                            }
                            convertCurrency();
                        }

//                    Log.i("values", value.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                // Log arrays
                Log.i("currencies", currencies.toString());
                Log.i("rates", rates.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}