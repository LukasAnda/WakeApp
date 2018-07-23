package sk.lukasanda.wakeapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import sk.lukasanda.wakeapp.activities.OnBoardingActivity;

/**
 * Implementation of App Widget functionality.
 */
public class AlarmsWidget extends AppWidgetProvider {
    
    private static String alarms = "";
    
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.alarms_widget);
        Intent intent = new Intent(context, OnBoardingActivity.class);
        views.setTextViewText(R.id.alarms_widget_text, alarms);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.alarms_widget_text, pendingIntent);
    
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.alarms_widget_container);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] recipeIds) {
        // There may be multiple widgets active, so update all of them
        AlarmsWidgetIntentService.startActionUpdateWidget(context);
    }
    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }
    
    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
    
    public static void setAlarms(String alarmss) {
        alarms = alarmss;
    }
    
    public static void updateAlarmWidgets(Context context, AppWidgetManager appWidgetManager,
                                          int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}

