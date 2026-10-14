package com.quizzy.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private static final int PURPLE = Color.rgb(98, 88, 217);
    private static final String PREFS = "quizzy_settings";
    private static final String NAME_KEY = "display_name";
    private static final String NAME_SET_KEY = "name_was_set";
    private static final String THEME_KEY = "theme";
    private QuizDatabase database;
    private LinearLayout root;
    private List<Question> quizQuestions;
    private int quizIndex;
    private boolean answered;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        database = new QuizDatabase(this);
        applySystemBars();
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        showHome();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(NAME_SET_KEY, false)) root.post(this::showNamePrompt);
    }

    @Override public void onBackPressed() {
        showHome();
    }

    private void setup(String title, String subtitle) {
        applySystemBars();
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(pageColor());
        setContentView(root);
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(22), dp(18), dp(22), dp(14));
        header.setBackgroundColor(pageColor());
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = text(title, 28, inkColor(), true);
        titleRow.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        if ("Quizzy".equals(title)) {
            Button settings = new Button(this);
            settings.setText("⚙");
            settings.setTextSize(22);
            settings.setTextColor(inkColor());
            settings.setAllCaps(false);
            settings.setContentDescription("Settings");
            settings.setMinWidth(dp(48));
            settings.setBackground(round(surfaceColor(), dp(15), outlineColor()));
            titleRow.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
            settings.setOnClickListener(v -> showSettings());
        }
        header.addView(titleRow);
        TextView sub = text(subtitle, 14, mutedColor(), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        header.addView(sub, subtitleParams);
        root.addView(header);
    }

    private void showNamePrompt() {
        EditText name = field("Your name", "", false);
        dialogBuilder()
                .setTitle("Welcome to Quizzy")
                .setMessage("What should we call you?")
                .setView(name)
                .setCancelable(false)
                .setPositiveButton("Continue", (dialog, which) -> saveName(name.getText().toString()))
                .setNegativeButton("Skip", (dialog, which) -> saveName(""))
                .show();
    }

    private void showSettings() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(8), dp(22), dp(4));
        TextView nameLabel = text("Your name", 14, inkColor(), true);
        content.addView(nameLabel);
        EditText name = field("Your name", displayName(), false);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = dp(6);
        content.addView(name, nameParams);
        TextView themeLabel = text("Theme", 14, inkColor(), true);
        LinearLayout.LayoutParams themeLabelParams = new LinearLayout.LayoutParams(-1, -2);
        themeLabelParams.topMargin = dp(18);
        content.addView(themeLabel, themeLabelParams);
        RadioGroup themes = new RadioGroup(this);
        themes.setOrientation(RadioGroup.VERTICAL);
        RadioButton light = new RadioButton(this);
        light.setText("Light");
        light.setTextColor(inkColor());
        light.setId(View.generateViewId());
        RadioButton dark = new RadioButton(this);
        dark.setText("Dark");
        dark.setTextColor(inkColor());
        dark.setId(View.generateViewId());
        themes.addView(light);
        themes.addView(dark);
        themes.check(isDarkTheme() ? dark.getId() : light.getId());
        content.addView(themes);
        AlertDialog dialog = dialogBuilder()
                .setTitle("Settings")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(NAME_KEY, normalizedName(name.getText().toString()))
                    .putBoolean(NAME_SET_KEY, true)
                    .putString(THEME_KEY, dark.isChecked() ? "dark" : "light")
                    .apply();
            dialog.dismiss();
            showHome();
        }));
        dialog.show();
    }

    private void saveName(String value) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(NAME_KEY, normalizedName(value))
                .putBoolean(NAME_SET_KEY, true)
                .apply();
        showHome();
    }

    private String normalizedName(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? "Quizzer" : trimmed;
    }

    private String displayName() {
        return normalizedName(getSharedPreferences(PREFS, MODE_PRIVATE).getString(NAME_KEY, ""));
    }

    private boolean isDarkTheme() {
        return "dark".equals(getSharedPreferences(PREFS, MODE_PRIVATE).getString(THEME_KEY, "light"));
    }

    private int pageColor() { return isDarkTheme() ? Color.rgb(25, 24, 32) : Color.rgb(247, 246, 252); }
    private int surfaceColor() { return isDarkTheme() ? Color.rgb(39, 37, 49) : Color.WHITE; }
    private int inkColor() { return isDarkTheme() ? Color.rgb(244, 242, 250) : Color.rgb(34, 33, 48); }
    private int mutedColor() { return isDarkTheme() ? Color.rgb(183, 179, 198) : Color.rgb(112, 110, 128); }
    private int outlineColor() { return isDarkTheme() ? Color.rgb(70, 67, 85) : Color.rgb(231, 229, 240); }

    private void applySystemBars() {
        boolean dark = isDarkTheme();
        int page = pageColor();
        getWindow().setStatusBarColor(page);
        getWindow().setNavigationBarColor(page);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private AlertDialog.Builder dialogBuilder() {
        int theme = isDarkTheme() ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert;
        return new AlertDialog.Builder(this, theme);
    }

    private void showHome() {
        setup("Quizzy", "Welcome, " + displayName());
        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(18), dp(4), dp(18), dp(112));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        List<Question> questions = database.all();
        if (questions.isEmpty()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(24), dp(60), dp(24), dp(20));
            TextView icon = text("✦", 42, PURPLE, true);
            icon.setGravity(Gravity.CENTER);
            empty.addView(icon);
            TextView message = text("Your quiz starts here", 20, inkColor(), true);
            message.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2);
            mp.topMargin = dp(10);
            empty.addView(message, mp);
            TextView hint = text("Tap + to add your first question and its four answer choices.", 14, mutedColor(), false);
            hint.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, -2);
            hp.topMargin = dp(8);
            empty.addView(hint, hp);
            list.addView(empty);
        } else {
            for (Question question : questions) list.addView(questionCard(question));
        }
        scroll.addView(list);
        content.addView(scroll);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button play = fab("▶", "Practice");
        Button add = fab("+", "");
        actions.addView(play);
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(dp(58), dp(58));
        addParams.leftMargin = dp(10);
        actions.addView(add, addParams);
        FrameLayout.LayoutParams actionParams = new FrameLayout.LayoutParams(-2, dp(64), Gravity.BOTTOM | Gravity.END);
        actionParams.setMargins(dp(12), dp(12), dp(20), dp(18));
        content.addView(actions, actionParams);
        play.setOnClickListener(v -> startQuiz());
        add.setOnClickListener(v -> showEditor(null));
    }

    private View questionCard(Question question) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(15), dp(16), dp(13));
        card.setBackground(round(surfaceColor(), dp(18), outlineColor()));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.bottomMargin = dp(12);
        card.setLayoutParams(cp);
        card.addView(text(question.prompt, 16, inkColor(), true));
        TextView answer = text("✓  " + question.options[0], 14, Color.rgb(35, 158, 96), true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(-1, -2);
        ap.topMargin = dp(10);
        card.addView(answer, ap);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button edit = smallButton("Edit");
        Button delete = smallButton("Delete");
        row.addView(edit);
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(-2, -2);
        deleteParams.leftMargin = dp(7);
        row.addView(delete, deleteParams);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
        rp.topMargin = dp(8);
        card.addView(row, rp);
        edit.setOnClickListener(v -> showEditor(question));
        delete.setOnClickListener(v -> dialogBuilder().setTitle("Delete question?").setMessage("This question will be removed from your quiz.").setNegativeButton("Cancel", null).setPositiveButton("Delete", (d, w) -> { database.delete(question.id); showHome(); }).show());
        return card;
    }

    private void showEditor(Question existing) {
        boolean editing = existing != null;
        setup(editing ? "Edit question" : "New question", "The first answer is always the correct one.");
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(4), dp(20), dp(24));
        EditText prompt = field("Write your question", existing == null ? "" : existing.prompt, false);
        form.addView(label("QUESTION"));
        form.addView(prompt, fieldParams());
        EditText[] options = new EditText[4];
        EditText[] infos = new EditText[4];
        for (int i = 0; i < 4; i++) {
            form.addView(label(i == 0 ? "CORRECT ANSWER" : "ANSWER OPTION " + (i + 1)));
            options[i] = field(i == 0 ? "Correct answer" : "Answer text", existing == null ? "" : existing.options[i], false);
            form.addView(options[i], fieldParams());
            TextView infoLabel = text("Extra info shown after you answer", 12, mutedColor(), false);
            LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(-1, -2);
            ilp.topMargin = dp(3);
            form.addView(infoLabel, ilp);
            infos[i] = field("Why this option matters", existing == null ? "" : existing.info[i], true);
            form.addView(infos[i], fieldParams());
        }
        Button save = new Button(this);
        save.setText(editing ? "Save changes" : "Save question");
        save.setTextColor(Color.WHITE);
        save.setAllCaps(false);
        save.setBackground(round(PURPLE, dp(14), PURPLE));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(52));
        bp.topMargin = dp(14);
        form.addView(save, bp);
        scroll.addView(form);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        save.setOnClickListener(v -> {
            String questionText = prompt.getText().toString().trim();
            String[] answerText = new String[4];
            String[] explanations = new String[4];
            if (questionText.isEmpty()) { prompt.setError("Enter a question"); return; }
            for (int i = 0; i < 4; i++) {
                answerText[i] = options[i].getText().toString().trim();
                explanations[i] = infos[i].getText().toString().trim();
                if (answerText[i].isEmpty()) { options[i].setError("Enter all four answers"); return; }
            }
            Question result = new Question(editing ? existing.id : 0, questionText, answerText, explanations);
            if (editing) database.update(result); else database.insert(result);
            showHome();
        });
    }

    private void startQuiz() {
        quizQuestions = database.all();
        if (quizQuestions.isEmpty()) {
            dialogBuilder().setTitle("No questions yet").setMessage("Add at least one question before starting a practice session.").setPositiveButton("Got it", null).show();
            return;
        }
        Collections.shuffle(quizQuestions);
        quizIndex = 0;
        answered = false;
        showQuizQuestion();
    }

    private void showQuizQuestion() {
        Question q = quizQuestions.get(quizIndex);
        setup("Practice", "Question " + (quizIndex + 1) + " of " + quizQuestions.size());
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(10), dp(20), dp(28));
        TextView prompt = text(q.prompt, 23, inkColor(), true);
        body.addView(prompt);
        ArrayList<Integer> order = new ArrayList<>();
        for (int i = 0; i < 4; i++) order.add(i);
        Collections.shuffle(order);
        for (int optionIndex : order) {
            final int selected = optionIndex;
            LinearLayout option = new LinearLayout(this);
            option.setOrientation(LinearLayout.VERTICAL);
            option.setPadding(dp(16), dp(14), dp(16), dp(14));
            option.setBackground(round(surfaceColor(), dp(15), outlineColor()));
            LinearLayout answerRow = new LinearLayout(this);
            answerRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView answerText = text(q.options[optionIndex], 16, inkColor(), true);
            answerRow.addView(answerText, new LinearLayout.LayoutParams(0, -2, 1));
            TextView mark = text("", 21, inkColor(), true);
            mark.setGravity(Gravity.CENTER);
            mark.setVisibility(View.GONE);
            answerRow.addView(mark, new LinearLayout.LayoutParams(dp(32), -2));
            option.addView(answerRow);
            TextView info = text(q.info[optionIndex].isEmpty() ? "No extra info added." : q.info[optionIndex], 14, mutedColor(), false);
            info.setVisibility(View.GONE);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
            ip.topMargin = dp(7);
            option.addView(info, ip);
            LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, -2);
            op.topMargin = dp(12);
            body.addView(option, op);
            option.setOnClickListener(v -> {
                if (answered) return;
                answered = true;
                for (int i = 0; i < body.getChildCount(); i++) {
                    View child = body.getChildAt(i);
                    if (child instanceof LinearLayout && child != prompt) {
                        LinearLayout answerCard = (LinearLayout) child;
                        int original = order.get(i - 1);
                        boolean correct = original == 0;
                        boolean wrongSelection = original == selected;
                        int fill = correct ? (isDarkTheme() ? Color.rgb(31, 69, 50) : Color.rgb(227, 247, 235)) : (wrongSelection ? (isDarkTheme() ? Color.rgb(78, 42, 47) : Color.rgb(255, 231, 232)) : surfaceColor());
                        int border = correct ? Color.rgb(71, 166, 111) : (wrongSelection ? Color.rgb(224, 102, 108) : outlineColor());
                        answerCard.setBackground(round(fill, dp(15), border));
                        TextView answerMark = (TextView) ((LinearLayout) answerCard.getChildAt(0)).getChildAt(1);
                        if (correct || wrongSelection) {
                            answerMark.setText(correct ? "✓" : "✕");
                            answerMark.setTextColor(correct ? Color.rgb(35, 158, 96) : Color.rgb(224, 76, 85));
                            answerMark.setVisibility(View.VISIBLE);
                        }
                        answerCard.getChildAt(1).setVisibility(View.VISIBLE);
                    }
                }
                String result = selected == 0 ? "Correct!" : "Not quite. The correct answer is highlighted in green.";
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
                root.postDelayed(() -> addNextButton(), 220);
            });
        }
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button quit = new Button(this);
        quit.setText("End practice");
        quit.setAllCaps(false);
        quit.setTextColor(inkColor());
        quit.setBackgroundColor(surfaceColor());
        quit.setOnClickListener(v -> showHome());
        root.addView(quit, new LinearLayout.LayoutParams(-1, dp(52)));
    }

    private void addNextButton() {
        if (!answered || root == null || root.getChildCount() < 2) return;
        Button next = new Button(this);
        next.setText(quizIndex + 1 == quizQuestions.size() ? "Finish quiz" : "Next question");
        next.setAllCaps(false);
        next.setTextColor(Color.WHITE);
        next.setBackground(round(PURPLE, dp(14), PURPLE));
        root.addView(next, new LinearLayout.LayoutParams(-1, dp(54)));
        next.setOnClickListener(v -> {
            if (++quizIndex >= quizQuestions.size()) showHome();
            else { answered = false; showQuizQuestion(); }
        });
    }

    private TextView label(String value) {
        TextView view = text(value, 11, PURPLE, true);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(14);
        formLabelPadding(view);
        return view;
    }

    private void formLabelPadding(TextView view) { view.setPadding(dp(2), 0, 0, 0); }
    private LinearLayout.LayoutParams fieldParams() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(6); return p; }

    private EditText field(String hint, String value, boolean multiline) {
        EditText edit = new EditText(this);
        edit.setSingleLine(!multiline);
        edit.setMinHeight(dp(multiline ? 68 : 52));
        if (multiline) { edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE); edit.setGravity(Gravity.TOP | Gravity.START); }
        else edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        edit.setHint(hint);
        edit.setText(value);
        edit.setTextSize(15);
        edit.setTextColor(inkColor());
        edit.setHintTextColor(mutedColor());
        edit.setPadding(dp(13), dp(11), dp(13), dp(11));
        edit.setTextColor(inkColor());
        edit.setHintTextColor(mutedColor());
        edit.setBackground(round(surfaceColor(), dp(12), outlineColor()));
        return edit;
    }

    private Button fab(String symbol, String title) {
        Button button = new Button(this);
        button.setText(title.isEmpty() ? symbol : symbol + "  " + title);
        button.setTextColor(Color.WHITE);
        button.setTextSize(title.isEmpty() ? 27 : 15);
        button.setAllCaps(false);
        button.setElevation(dp(5));
        button.setBackground(round(PURPLE, dp(30), PURPLE));
        button.setMinWidth(title.isEmpty() ? dp(58) : dp(126));
        button.setLayoutParams(new LinearLayout.LayoutParams(title.isEmpty() ? dp(58) : dp(126), dp(58)));
        return button;
    }

    private Button smallButton(String title) {
        Button button = new Button(this);
        button.setText(title);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setTextColor(PURPLE);
        button.setMinHeight(dp(40));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(isDarkTheme() ? Color.rgb(56, 53, 72) : Color.rgb(244, 243, 250)));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    private static class Question {
        final long id;
        final String prompt;
        final String[] options;
        final String[] info;
        Question(long id, String prompt, String[] options, String[] info) { this.id = id; this.prompt = prompt; this.options = options; this.info = info; }
    }

    private static class QuizDatabase extends SQLiteOpenHelper {
        QuizDatabase(Context context) { super(context, "quizzy.db", null, 1); }
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE questions (_id INTEGER PRIMARY KEY AUTOINCREMENT, prompt TEXT NOT NULL, a0 TEXT NOT NULL, a1 TEXT NOT NULL, a2 TEXT NOT NULL, a3 TEXT NOT NULL, i0 TEXT NOT NULL, i1 TEXT NOT NULL, i2 TEXT NOT NULL, i3 TEXT NOT NULL)");
        }
        @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
        void insert(Question q) { getWritableDatabase().insertOrThrow("questions", null, values(q)); }
        void update(Question q) { getWritableDatabase().update("questions", values(q), "_id=?", new String[]{String.valueOf(q.id)}); }
        void delete(long id) { getWritableDatabase().delete("questions", "_id=?", new String[]{String.valueOf(id)}); }
        List<Question> all() {
            ArrayList<Question> result = new ArrayList<>();
            try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,prompt,a0,a1,a2,a3,i0,i1,i2,i3 FROM questions ORDER BY _id DESC", null)) {
                while (c.moveToNext()) result.add(new Question(c.getLong(0), c.getString(1), new String[]{c.getString(2), c.getString(3), c.getString(4), c.getString(5)}, new String[]{c.getString(6), c.getString(7), c.getString(8), c.getString(9)}));
            }
            return result;
        }
        private ContentValues values(Question q) {
            ContentValues v = new ContentValues();
            v.put("prompt", q.prompt);
            for (int i = 0; i < 4; i++) { v.put("a" + i, q.options[i]); v.put("i" + i, q.info[i]); }
            return v;
        }
    }
}
