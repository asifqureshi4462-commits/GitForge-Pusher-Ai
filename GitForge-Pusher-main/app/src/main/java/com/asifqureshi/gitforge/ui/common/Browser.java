package com.asifqureshi.gitforge.ui.common;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.asifqureshi.gitforge.R;

public final class Browser {

    private Browser() {
    }

    /** Opens an https URL in the user's browser. Only https URLs are ever opened. */
    public static void open(@NonNull Context context, @NonNull String url) {
        if (!url.startsWith("https://")) {
            return;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_browser, Toast.LENGTH_LONG).show();
        }
    }
}
