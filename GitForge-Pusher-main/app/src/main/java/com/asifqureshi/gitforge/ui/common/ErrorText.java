package com.asifqureshi.gitforge.ui.common;

import android.content.Context;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;

import com.asifqureshi.gitforge.domain.github.ApiException;

import java.util.Date;

/** Turns API exceptions into display strings (adds the reset time for rate limits). */
public final class ErrorText {

    private ErrorText() {
    }

    @NonNull
    public static String describe(@NonNull Context context, @NonNull ApiException e) {
        return describe(context, e.getMessage(), e.getKind(), e.getRetryAtEpochSeconds());
    }

    @NonNull
    public static String describe(@NonNull Context context, @NonNull String message,
                                  @NonNull ApiException.Kind kind, long retryAtEpochSeconds) {
        if (kind == ApiException.Kind.RATE_LIMITED && retryAtEpochSeconds > 0) {
            String time = DateFormat.getTimeFormat(context).format(new Date(retryAtEpochSeconds * 1000L));
            return message + " You can try again after " + time + ".";
        }
        return message;
    }
}
