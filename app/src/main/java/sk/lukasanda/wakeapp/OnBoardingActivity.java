package sk.lukasanda.wakeapp;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

public class OnBoardingActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }
    
    private void requestPermissions() {
        Intent intent = new Intent(this, MapsActivity.class);
        MultiplePermissionsListener dialogPermissionListener =
                new CustomPermissionListener(this,
                        "Location permission",
                        "We need this permission to wake you up at your position",
                        "Close app",
                        getResources().getDrawable(R.mipmap.ic_launcher), intent);
        
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission
                        .READ_PHONE_STATE)
                .withListener(dialogPermissionListener)
                .check();
    }
    
    
    
    private class CustomPermissionListener extends BaseMultiplePermissionsListener {
        private final Activity activity;
        private final String title;
        private final String message;
        private final String positiveButtonText;
        private final Drawable icon;
        private final Intent intent;
        
        private CustomPermissionListener(Activity activity, String title, String message, String
                positiveButtonText, Drawable icon, Intent intent) {
            this.activity = activity;
            this.title = title;
            this.message = message;
            this.positiveButtonText = positiveButtonText;
            this.icon = icon;
            this.intent = intent;
        }
        
        @Override
        public void onPermissionsChecked(MultiplePermissionsReport report) {
            super.onPermissionsChecked(report);
            if (report.areAllPermissionsGranted()) {
                startActivity(intent);
            } else {
                new AlertDialog.Builder(activity)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(positiveButtonText, new DialogInterface
                                .OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                activity.finish();
                            }
                        })
                        .setIcon(icon)
                        .show();
            }
        }
        
    }
}
