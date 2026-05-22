package com.example.codecompiler;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LanguageAdapter extends ArrayAdapter<String> {

    private final String[] names;
    private final int[] icons;
    private final Context context;

    public LanguageAdapter(Context context, String[] names, int[] icons) {
        super(context, R.layout.spinner_item, names);
        this.context = context;
        this.names = names;
        this.icons = icons;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.spinner_item, parent, false);
        }

        ImageView iconView = convertView.findViewById(R.id.langIcon);
        TextView nameView = convertView.findViewById(R.id.langName);

        nameView.setText(names[position]);
        if (icons[position] != 0) {
            iconView.setImageResource(icons[position]);
        } else {
            iconView.setVisibility(View.GONE);
        }

        return convertView;
    }
}