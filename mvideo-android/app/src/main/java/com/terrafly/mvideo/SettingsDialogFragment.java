package com.terrafly.mvideo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by pachevjoseph on 8/25/16.
 */

public class SettingsDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View streamText = inflater.inflate(R.layout.dialog_stream_name, null);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(streamText)
               .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       Log.v("Main Activity", "Ok button was pressed");
                   }
               })
               .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       Log.v("Main Activity", "Was cancelled");
                   }
               });
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
