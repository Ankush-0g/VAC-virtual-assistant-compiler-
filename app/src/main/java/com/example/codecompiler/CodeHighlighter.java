package com.example.codecompiler;

import android.graphics.Color;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeHighlighter {

    private static final Pattern PYTHON_KEYWORDS = Pattern.compile(
            "\\b(and|as|assert|async|await|break|class|continue|def|del|elif|else|except|False|finally|for|from|global|if|import|in|is|lambda|None|nonlocal|not|or|pass|raise|return|True|try|while|with|yield)\\b");

    private static final Pattern JAVA_KEYWORDS = Pattern.compile(
            "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\\b");

    private static final Pattern CPP_KEYWORDS = Pattern.compile(
            "\\b(alignas|alignof|and|and_eq|asm|atomic_cancel|atomic_commit|atomic_noexcept|auto|bitand|bitor|bool|break|case|catch|char|char8_t|char16_t|char32_t|class|compl|concept|const|consteval|constexpr|constinit|const_cast|continue|co_await|co_return|co_yield|decltype|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|false|float|for|friend|goto|if|inline|int|long|mutable|namespace|new|noexcept|not|not_eq|nullptr|operator|or|or_eq|private|protected|public|reflexpr|register|reinterpret_cast|requires|return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|synchronized|template|this|thread_local|throw|true|try|typedef|typeid|typename|union|unsigned|using|virtual|void|volatile|wchar_t|while|xor|xor_eq)\\b");

    private static final Pattern GO_KEYWORDS = Pattern.compile(
            "\\b(break|default|func|interface|select|case|defer|go|map|struct|chan|else|goto|package|switch|const|fallthrough|if|range|type|continue|for|import|return|var)\\b");

    private static final Pattern R_KEYWORDS = Pattern.compile(
            "\\b(if|else|repeat|while|function|for|in|next|break|TRUE|FALSE|NULL|Inf|NaN|NA|NA_integer_|NA_real_|NA_complex_|NA_character_|library|require|source|return)\\b");

    private static final Pattern PHP_KEYWORDS = Pattern.compile(
            "\\b(abstract|and|array|as|break|callable|case|catch|class|clone|const|continue|declare|default|die|do|echo|else|elseif|empty|enddeclare|endfor|endforeach|endif|endswitch|endwhile|eval|exit|extends|final|finally|for|foreach|function|global|goto|if|implements|include|include_once|instanceof|insteadof|interface|isset|list|namespace|new|or|print|private|protected|public|require|require_once|return|static|switch|throw|trait|try|unset|use|var|while|xor|yield)\\b");

    private static final Pattern RUBY_KEYWORDS = Pattern.compile(
            "\\b(alias|and|begin|break|case|class|def|defined\\?|do|else|elsif|end|ensure|false|for|if|in|module|next|nil|not|or|redo|rescue|retry|return|self|super|then|true|undef|unless|until|when|while|yield|__FILE__|__LINE__)\\b");

    private static final Pattern RUST_KEYWORDS = Pattern.compile(
            "\\b(as|async|await|break|const|continue|crate|dyn|else|enum|extern|false|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|super|trait|true|type|union|unsafe|use|where|while)\\b");

    private static final Pattern SWIFT_KEYWORDS = Pattern.compile(
            "\\b(associatedtype|class|deinit|enum|extension|fileprivate|func|import|init|inout|internal|let|open|operator|private|precedencegroup|protocol|public|rethrows|static|struct|subscript|typealias|var|break|case|continue|default|defer|do|else|fallthrough|for|guard|if|in|repeat|return|switch|where|while|as|Any|catch|false|is|nil|super|self|Self|throw|throws|true|try)\\b");

    private static final Pattern PHP_VARIABLES = Pattern.compile("\\$[a-zA-Z_\\x7f-\\xff][a-zA-Z0-9_\\x7f-\\xff]*");

    private static final Pattern DATATYPES = Pattern.compile(
            "\\b(int|float|double|char|boolean|long|short|byte|void|String|Integer|Double|Float|Character|Boolean|unsigned|signed|size_t|uint8_t|uint16_t|uint32_t|uint64_t|int8_t|int16_t|int32_t|int64_t|complex|complex64|complex128|error|string|map|chan|interface|byte|rune|bool|float32|float64|uint|int|uintptr|str|list|dict|tuple|set|numeric|logical|complex|character|vector|matrix|array|list|data.frame|factor|bool|int|float|string|array|object|callable|iterable|resource|null|i8|i16|i32|i64|i128|isize|u8|u16|u32|u64|u128|usize|f32|f64|Option|Result|Vec|Int|Double|Float|Bool|Character|Array|Dictionary|Set|Optional)\\b");

    private static final Pattern STRINGS = Pattern.compile("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'");
    private static final Pattern COMMENTS = Pattern.compile("//.*|/\\*(.|\\R)*?\\*/|#.*");

    public static void highlight(Editable s, String languageId) {
        // Clear previous spans
        ForegroundColorSpan[] spans = s.getSpans(0, s.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            s.removeSpan(span);
        }

        Pattern keywords;
        Pattern variables = null;
        switch (languageId) {
            case "71": // Python
                keywords = PYTHON_KEYWORDS;
                break;
            case "62": // Java
                keywords = JAVA_KEYWORDS;
                break;
            case "54": // C++
                keywords = CPP_KEYWORDS;
                break;
            case "50": // C
                keywords = CPP_KEYWORDS;
                break;
            case "60": // Go
                keywords = GO_KEYWORDS;
                break;
            case "80": // R
                keywords = R_KEYWORDS;
                break;
            case "68": // PHP
                keywords = PHP_KEYWORDS;
                variables = PHP_VARIABLES;
                break;
            case "72": // Ruby
                keywords = RUBY_KEYWORDS;
                break;
            case "73": // Rust
                keywords = RUST_KEYWORDS;
                break;
            case "83": // Swift
                keywords = SWIFT_KEYWORDS;
                break;
            default:
                return;
        }

        applySpan(s, keywords, Color.parseColor("#BB86FC")); // Purple for keywords
        applySpan(s, DATATYPES, Color.parseColor("#FFB74D")); // Orange for datatypes
        if (variables != null) {
            applySpan(s, variables, Color.parseColor("#90CAF9")); // Light Blue for PHP variables
        }
        applySpan(s, STRINGS, Color.parseColor("#03DAC5"));  // Teal for strings
        applySpan(s, COMMENTS, Color.parseColor("#808080")); // Gray for comments
    }

    private static void applySpan(Editable s, Pattern pattern, int color) {
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            s.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
