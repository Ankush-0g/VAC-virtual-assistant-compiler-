package com.example.codecompiler;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.*;
import retrofit2.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    EditText editor;
    TextView lineNumbers;
    Spinner spinner;
    EditText inputEditText;
    CardView inputCardView;
    Button runBtn, saveBtn, loadBtn;
    ImageButton settingsBtn, undoBtn, redoBtn;
    LinearLayout errorLayout;
    TextView errorHeaderText, errorText;
    
    String selectedLanguageId = "71";

    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private boolean isUndoRedoAction = false;
    private boolean isHighlighting = false;
    private boolean isAutoLoadingTemplate = false;
    private boolean isModifying = false;

    private ActivityResultLauncher<Intent> saveFileLauncher;
    private ActivityResultLauncher<Intent> loadFileLauncher;

    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable errorCheckRunnable;

    private static final Map<String, String> CODE_TEMPLATES = new HashMap<>();
    static {
        CODE_TEMPLATES.put("71", "def main():\n    print(\"Hello, World!\")\n\nif __name__ == \"__main__\":\n    main()"); // Python
        CODE_TEMPLATES.put("62", "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}"); // Java
        CODE_TEMPLATES.put("54", "#include <iostream>\n\nint main() {\n    std::cout << \"Hello, World!\" << std::endl;\n    return 0;\n}"); // C++
        CODE_TEMPLATES.put("50", "#include <stdio.h>\n\nint main() {\n    printf(\"Hello, World!\\n\");\n    return 0;\n}"); // C
        CODE_TEMPLATES.put("60", "package main\n\nimport \"fmt\"\n\nfunc main() {\n    fmt.Println(\"Hello, World!\")\n}"); // Go
        CODE_TEMPLATES.put("80", "print(\"Hello, World!\")"); // R
        CODE_TEMPLATES.put("68", "<?php\necho \"Hello, World!\";\n?>"); // PHP
        CODE_TEMPLATES.put("72", "puts \"Hello, World!\""); // Ruby
        CODE_TEMPLATES.put("73", "fn main() {\n    println!(\"Hello, World!\");\n}"); // Rust
        CODE_TEMPLATES.put("83", "import Foundation\n\nprint(\"Hello, World!\")"); // Swift
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        editor = findViewById(R.id.codeEditor);
        lineNumbers = findViewById(R.id.lineNumbers);
        spinner = findViewById(R.id.languageSpinner);
        inputEditText = findViewById(R.id.inputEditText);
        inputCardView = findViewById(R.id.inputCardView);
        runBtn = findViewById(R.id.runBtn);
        saveBtn = findViewById(R.id.saveBtn);
        loadBtn = findViewById(R.id.loadBtn);
        settingsBtn = findViewById(R.id.settingsBtn);
        undoBtn = findViewById(R.id.undoBtn);
        redoBtn = findViewById(R.id.redoBtn);
        errorLayout = findViewById(R.id.errorLayout);
        errorText = findViewById(R.id.errorText);
        errorHeaderText = findViewById(R.id.errorHeaderText);

        setupSpinner();
        setupRetrofit();
        setupFileLaunchers();
        setupButtons();
        setupLineNumbers();
        setupSettings();
        setupUndoRedo();

        // Push initial empty state
        undoStack.push("");

        // Set initial Python template
        loadTemplate("71");

        // Code Editor Features: Auto-brackets, Auto-indentation, Undo/Redo, Highlighting
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isHighlighting || isAutoLoadingTemplate || isModifying || count != 1) return;

                char c = s.charAt(start);
                String insert = null;
                boolean moveCursorInside = false;

                switch (c) {
                    case '(': insert = ")"; moveCursorInside = true; break;
                    case '{': insert = "}"; moveCursorInside = true; break;
                    case '[': insert = "]"; moveCursorInside = true; break;
                    case '"': insert = "\""; moveCursorInside = true; break;
                    case '\'': insert = "'"; moveCursorInside = true; break;
                    case '\n':
                        // Auto-indentation logic
                        String text = s.toString();
                        int lastNewline = text.lastIndexOf('\n', start - 1);
                        int lineStart = (lastNewline == -1) ? 0 : lastNewline + 1;
                        String previousLine = text.substring(lineStart, start);
                        
                        StringBuilder indent = new StringBuilder();
                        for (char ch : previousLine.toCharArray()) {
                            if (ch == ' ' || ch == '\t') indent.append(ch);
                            else break;
                        }
                        
                        String trimmedLine = previousLine.trim();
                        if (trimmedLine.endsWith("{") || trimmedLine.endsWith(":") || 
                            trimmedLine.endsWith("(") || trimmedLine.endsWith("[")) {
                            indent.append("    ");
                        }
                        
                        insert = indent.toString();
                        break;
                }

                if (insert != null && !insert.isEmpty() || moveCursorInside) {
                    isModifying = true;
                    if (insert != null) {
                        editor.getText().insert(start + 1, insert);
                    }
                    if (moveCursorInside) {
                        editor.setSelection(start + 1);
                    }
                    isModifying = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isHighlighting || isAutoLoadingTemplate || isModifying) return;

                if (!isUndoRedoAction) {
                    String currentText = s.toString();
                    if (undoStack.isEmpty() || !undoStack.peek().equals(currentText)) {
                        undoStack.push(currentText);
                        redoStack.clear();
                    }
                }
                checkForInputKeywords(s.toString());

                isHighlighting = true;
                CodeHighlighter.highlight(s, selectedLanguageId);
                isHighlighting = false;

                scheduleErrorCheck();
            }
        });
    }

    private void loadTemplate(String langId) {
        String template = CODE_TEMPLATES.get(langId);
        if (template != null) {
            isAutoLoadingTemplate = true;
            editor.setText(template);
            isAutoLoadingTemplate = false;
            
            isHighlighting = true;
            CodeHighlighter.highlight(editor.getText(), langId);
            isHighlighting = false;
            
            updateLineNumbers();
        }
    }

    private void scheduleErrorCheck() {
        if (errorCheckRunnable != null) {
            debounceHandler.removeCallbacks(errorCheckRunnable);
        }
        errorCheckRunnable = () -> checkSyntaxErrors();
        debounceHandler.postDelayed(errorCheckRunnable, 2500);
    }

    private void checkSyntaxErrors() {
        String code = editor.getText().toString();
        if (code.trim().isEmpty()) {
            hideError();
            return;
        }

        try {
            String base64Code = Base64.encodeToString(code.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            
            JSONObject json = new JSONObject();
            json.put("source_code", base64Code);
            json.put("language_id", Integer.parseInt(selectedLanguageId));
            
            // Include STDIN from the UI to avoid background execution errors on input lines
            String currentStdin = inputEditText.getText().toString();
            json.put("stdin", Base64.encodeToString(currentStdin.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));

            RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());

            apiService.executeCodeBase64(body).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String rawResponse = response.body().string();
                            JSONObject responseJson = new JSONObject(rawResponse);

                            String compileOutput = decodeBase64(responseJson.optString("compile_output", ""));
                            String stderr = decodeBase64(responseJson.optString("stderr", ""));
                            JSONObject status = responseJson.optJSONObject("status");
                            int statusId = (status != null) ? status.optInt("id", 3) : 3;

                            // Filtering logic to only show real syntax/compile errors
                            // Status 6 is Compilation Error (always show)
                            // Other statuses might be runtime errors triggered by missing input
                            boolean isCompError = statusId == 6;
                            boolean isSyntaxError = stderr.contains("SyntaxError") || stderr.contains("Error") || stderr.contains("error:");
                            
                            // Specifically filter out common missing-input errors
                            boolean isInputError = stderr.contains("EOFError") || 
                                                 stderr.contains("NoSuchElementException") || 
                                                 stderr.contains("EOF") ||
                                                 stderr.contains("stdin: empty");

                            String fullError = (compileOutput + "\n" + stderr).trim();
                            
                            if ((isCompError || isSyntaxError) && !isInputError && !fullError.isEmpty()) {
                                showError(fullError);
                            } else {
                                // If it was an input error or successful execution, don't show an error block
                                // But if it was Accept (3), show success briefly
                                if (statusId == 3) {
                                    showSuccess();
                                } else {
                                    hideError();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String decodeBase64(String encoded) {
        if (encoded == null || encoded.isEmpty() || encoded.equals("null")) return "";
        try {
            byte[] data = Base64.decode(encoded, Base64.DEFAULT);
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private void showError(String error) {
        runOnUiThread(() -> {
            errorLayout.setVisibility(View.VISIBLE);
            errorLayout.setBackgroundResource(R.drawable.bg_error_block);
            
            errorHeaderText.setText("Compilation Error");
            errorHeaderText.setTextColor(Color.parseColor("#FF5252"));
            
            errorText.setText(error);

            int lineNumber = -1;
            Pattern cppPattern = Pattern.compile(":(?:line )?(\\d+):");
            Matcher matcher = cppPattern.matcher(error);
            if (matcher.find()) {
                lineNumber = Integer.parseInt(matcher.group(1));
            } else {
                String[] patterns = {"line (\\d+)", "at line (\\d+)", "on line (\\d+)"};
                for (String p : patterns) {
                    Matcher m = Pattern.compile(p).matcher(error);
                    if (m.find()) {
                        lineNumber = Integer.parseInt(m.group(1));
                        break;
                    }
                }
            }

            if (lineNumber != -1) {
                highlightErrorLine(lineNumber);
            } else {
                removeErrorHighlights();
            }
        });
    }

    private void showSuccess() {
        runOnUiThread(() -> {
            errorLayout.setVisibility(View.VISIBLE);
            errorLayout.setBackgroundResource(R.drawable.bg_success_block);
            
            errorHeaderText.setText("Build Successful");
            errorHeaderText.setTextColor(Color.parseColor("#4CAF50"));
            
            errorText.setText("No issues found.");
            removeErrorHighlights();
            
            debounceHandler.postDelayed(() -> {
                if (errorLayout.getVisibility() == View.VISIBLE && errorHeaderText.getText().toString().equals("Build Successful")) {
                    errorLayout.setVisibility(View.GONE);
                }
            }, 3500);
        });
    }

    private void hideError() {
        runOnUiThread(() -> {
            errorLayout.setVisibility(View.GONE);
            removeErrorHighlights();
        });
    }

    private void highlightErrorLine(int lineNum) {
        Editable editable = editor.getText();
        removeErrorHighlights();
        
        String[] lines = editable.toString().split("\n", -1);
        if (lineNum > 0 && lineNum <= lines.length) {
            int start = 0;
            for (int i = 0; i < lineNum - 1; i++) {
                start += lines[i].length() + 1;
            }
            int end = start + lines[lineNum - 1].length();
            
            if (end > editable.length()) end = editable.length();

            if (start < end) {
                editable.setSpan(new ForegroundColorSpan(Color.RED), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                editable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private void removeErrorHighlights() {
        Editable editable = editor.getText();
        UnderlineSpan[] uSpans = editable.getSpans(0, editable.length(), UnderlineSpan.class);
        for (UnderlineSpan span : uSpans) {
            editable.removeSpan(span);
        }
        
        isHighlighting = true;
        CodeHighlighter.highlight(editable, selectedLanguageId);
        isHighlighting = false;
    }

    private void setupUndoRedo() {
        undoBtn.setOnClickListener(v -> {
            if (undoStack.size() > 1) {
                isUndoRedoAction = true;
                redoStack.push(undoStack.pop());
                editor.setText(undoStack.peek());
                editor.setSelection(editor.getText().length());
                isUndoRedoAction = false;
            } else {
                Toast.makeText(this, "Nothing to undo", Toast.LENGTH_SHORT).show();
            }
        });

        redoBtn.setOnClickListener(v -> {
            if (!redoStack.isEmpty()) {
                isUndoRedoAction = true;
                String text = redoStack.pop();
                undoStack.push(text);
                editor.setText(text);
                editor.setSelection(editor.getText().length());
                isUndoRedoAction = false;
            } else {
                Toast.makeText(this, "Nothing to redo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkForInputKeywords(String code) {
        String[] keywords = {"input(", "Scanner", "cin >>", "scanf(", "readLine("};
        for (String key : keywords) {
            if (code.contains(key)) {
                if (inputCardView.getVisibility() == View.GONE) {
                    inputCardView.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    }

    private void setupSettings() {
        settingsBtn.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(MainActivity.this, settingsBtn);
            popup.getMenu().add(0, 1, 0, "Save File");
            popup.getMenu().add(0, 2, 0, "Load File");

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1) {
                    saveBtn.performClick();
                    return true;
                } else if (id == 2) {
                    loadBtn.performClick();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupLineNumbers() {
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateLineNumbers();
            }
        });
        editor.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> lineNumbers.setScrollY(scrollY));
    }

    private void updateLineNumbers() {
        int lines = editor.getLineCount();
        if (lines <= 0) lines = 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://judge0-ce.p.rapidapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    private void setupFileLaunchers() {
        saveFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) saveFileToUri(uri);
            }
        });
        loadFileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) loadFileFromUri(uri);
            }
        });
    }

    private void saveFileToUri(Uri uri) {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                outputStream.write(editor.getText().toString().getBytes());
                Toast.makeText(this, "File saved successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFileFromUri(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append("\n");
            editor.setText(sb.toString());
            updateLineNumbers();
            Toast.makeText(this, "File loaded successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT).show();
        }
    }

    private ApiService apiService;

    private void setupButtons() {
        runBtn.setOnClickListener(v -> {
            try {
                String sourceCode = editor.getText().toString();
                String base64Code = Base64.encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                
                JSONObject json = new JSONObject();
                json.put("source_code", base64Code);
                json.put("language_id", Integer.parseInt(selectedLanguageId));

                String stdin = inputEditText.getText().toString();
                if (!stdin.trim().isEmpty()) {
                    json.put("stdin", Base64.encodeToString(stdin.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
                }

                RequestBody body = RequestBody.create(MediaType.parse("application/json"), json.toString());
                runBtn.setEnabled(false);
                runBtn.setText("Running...");

                apiService.executeCodeBase64(body).enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        runBtn.setEnabled(true);
                        runBtn.setText("Run Code");
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                String rawResponse = response.body().string();
                                JSONObject responseJson = new JSONObject(rawResponse);

                                String stdout = decodeBase64(responseJson.optString("stdout", ""));
                                String stderr = decodeBase64(responseJson.optString("stderr", ""));
                                String compileOutput = decodeBase64(responseJson.optString("compile_output", ""));
                                String status = responseJson.has("status") ? responseJson.getJSONObject("status").getString("description") : "Unknown";
                                String time = responseJson.optString("time", "0.0");
                                String memory = responseJson.optString("memory", "0");

                                Intent intent = new Intent(MainActivity.this, OutputActivity.class);
                                intent.putExtra("output", stdout);
                                intent.putExtra("error", (compileOutput + "\n" + stderr).trim());
                                intent.putExtra("status", status);
                                intent.putExtra("time", time);
                                intent.putExtra("memory", memory);
                                intent.putExtra("source_code", sourceCode);
                                intent.putExtra("language_id", selectedLanguageId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(MainActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "Execution Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        runBtn.setEnabled(true);
                        runBtn.setText("Run Code");
                        Toast.makeText(MainActivity.this, "Network Error", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error building request", Toast.LENGTH_SHORT).show();
            }
        });

        saveBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "code.txt");
            saveFileLauncher.launch(intent);
        });

        loadBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            loadFileLauncher.launch(intent);
        });
    }

    private void setupSpinner() {
        String[] languages = {"Python", "Java", "C++", "C", "Go", "R", "PHP", "Ruby", "Rust", "Swift"};
        String[] langIds = {"71", "62", "54", "50", "60", "80", "68", "72", "73", "83"};

        int[] icons = {
                getResources().getIdentifier("python_logo", "drawable", getPackageName()),
                getResources().getIdentifier("java_logo", "drawable", getPackageName()),
                getResources().getIdentifier("cpp_logo", "drawable", getPackageName()),
                getResources().getIdentifier("c_logo", "drawable", getPackageName()),
                getResources().getIdentifier("go_logo", "drawable", getPackageName()),
                getResources().getIdentifier("r_logo", "drawable", getPackageName()),
                getResources().getIdentifier("php_logo", "drawable", getPackageName()),
                getResources().getIdentifier("ruby_logo", "drawable", getPackageName()),
                getResources().getIdentifier("rust_logo", "drawable", getPackageName()),
                getResources().getIdentifier("swift_logo", "drawable", getPackageName())
        };

        LanguageAdapter adapter = new LanguageAdapter(this, languages, icons);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLanguageId = langIds[position];
                loadTemplate(selectedLanguageId);
                scheduleErrorCheck();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
}
