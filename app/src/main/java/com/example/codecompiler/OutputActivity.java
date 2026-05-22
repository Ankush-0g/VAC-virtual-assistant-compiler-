package com.example.codecompiler;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OutputActivity extends AppCompatActivity {

    private TextView fullOutputText, timeTv, memoryTv;
    private EditText outputInputEditText;
    private Button rerunBtn;
    private ApiService apiService;
    private String sourceCode;
    private String languageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_output);

        fullOutputText = findViewById(R.id.fullOutputText);
        timeTv = findViewById(R.id.timeTv);
        memoryTv = findViewById(R.id.memoryTv);
        outputInputEditText = findViewById(R.id.outputInputEditText);
        rerunBtn = findViewById(R.id.rerunBtn);
        Button backBtn = findViewById(R.id.backBtn);

        sourceCode = getIntent().getStringExtra("source_code");
        languageId = getIntent().getStringExtra("language_id");
        String output = getIntent().getStringExtra("output");
        String error = getIntent().getStringExtra("error");
        String status = getIntent().getStringExtra("status");
        String time = getIntent().getStringExtra("time");
        String memory = getIntent().getStringExtra("memory");

        StringBuilder fullText = new StringBuilder();
        if (output != null && !output.isEmpty()) fullText.append("Output:\n").append(output).append("\n\n");
        if (error != null && !error.isEmpty()) fullText.append("Error:\n").append(error).append("\n\n");
        if (status != null) fullText.append("Status: ").append(status);

        fullOutputText.setText(fullText.toString());

        if (time != null) timeTv.setText("Time: " + time + "s");
        if (memory != null) memoryTv.setText("Memory: " + memory + "KB");

        setupRetrofit();

        rerunBtn.setOnClickListener(v -> executeWithInput());
        backBtn.setOnClickListener(v -> finish());
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://judge0-ce.p.rapidapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    private void executeWithInput() {
        String input = outputInputEditText.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter input", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("source_code", sourceCode);
            json.put("language_id", Integer.parseInt(languageId));
            json.put("stdin", input);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"),
                    json.toString()
            );

            rerunBtn.setEnabled(false);
            rerunBtn.setText("Running...");

            apiService.executeCode(body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    rerunBtn.setEnabled(true);
                    rerunBtn.setText("Run Again with Input");
                    try {
                        if (response.body() != null) {
                            String rawResponse = response.body().string();
                            JSONObject responseJson = new JSONObject(rawResponse);

                            String statusDescription = responseJson.has("status") ? responseJson.getJSONObject("status").getString("description") : "";
                            String stdout = responseJson.optString("stdout", "");
                            String stderr = responseJson.optString("stderr", "");
                            String compileOutput = responseJson.optString("compile_output", "");
                            String time = responseJson.optString("time", "0.0");
                            String memory = responseJson.optString("memory", "0");

                            StringBuilder fullOutput = new StringBuilder();
                            if (!stdout.isEmpty()) fullOutput.append("Output:\n").append(stdout).append("\n\n");
                            if (!stderr.isEmpty()) fullOutput.append("Error:\n").append(stderr).append("\n\n");
                            if (!compileOutput.isEmpty()) fullOutput.append("Compile Output:\n").append(compileOutput).append("\n\n");
                            if (!statusDescription.isEmpty()) fullOutput.append("Status: ").append(statusDescription);

                            fullOutputText.setText(fullOutput.toString());
                            timeTv.setText("Time: " + time + "s");
                            memoryTv.setText("Memory: " + memory + "KB");
                        }
                    } catch (Exception e) {
                        Toast.makeText(OutputActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    rerunBtn.setEnabled(true);
                    rerunBtn.setText("Run Again with Input");
                    Toast.makeText(OutputActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error building request", Toast.LENGTH_SHORT).show();
        }
    }
}
