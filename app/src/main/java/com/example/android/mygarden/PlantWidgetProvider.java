package com.example.android.mygarden;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.ui.MainActivity;
import com.example.android.mygarden.ui.PlantDetailActivity;


public class PlantWidgetProvider extends AppWidgetProvider {

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int imgRes, long plantId, boolean showWater, int appWidgetId) {
        // Get current width to decide on single plant vs garden grid view
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        RemoteViews rv;
        if (width < 300) {
            rv = getSinglePlantRemoteView(context, imgRes, plantId, showWater);
        } else {
            rv = getGardenGridRemoteView(context);
        }
        appWidgetManager.updateAppWidget(appWidgetId, rv);
    }

    /**
     * Creates and returns the RemoteViews to be displayed in the single plant mode widget
     *
     * @param context   The context
     * @param imgRes    The image resource of the plant image to be displayed
     * @param plantId   The database plant Id for watering button functionality
     * @param showWater Boolean to either show/hide the water drop
     * @return The RemoteViews for the single plant mode widget
     */
    private static RemoteViews getSinglePlantRemoteView(Context context, int imgRes, long plantId, boolean showWater) {

        // Set the click handler to open the DetailActivity for plant ID,
        // or the MainActivity if plant ID is invalid
        Intent intent;
        if (plantId == PlantContract.INVALID_PLANT_ID) {
            intent = new Intent(context, MainActivity.class);
        } else { // Set on click to open the corresponding detail activity
            Log.d(PlantWidgetProvider.class.getSimpleName(), "plantId=" + plantId);
            intent = new Intent(context, PlantDetailActivity.class);
            intent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plantId);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.plant_widget);
        // Update image
        views.setImageViewResource(R.id.widget_plant_image, imgRes);
        // Update plant ID text
        views.setTextViewText(R.id.widget_plant_name, String.valueOf(plantId));
        // Show/Hide the water drop button
        if (showWater) views.setViewVisibility(R.id.widget_water_button, View.VISIBLE);
        else views.setViewVisibility(R.id.widget_water_button, View.INVISIBLE);
        // Widgets allow click handlers to only launch pending intents
        views.setOnClickPendingIntent(R.id.widget_plant_image, pendingIntent);
        // Add the wateringservice click handler
        Intent wateringIntent = new Intent(context, PlantWateringService.class);
        wateringIntent.setAction(PlantWateringService.ACTION_WATER_PLANT);
        wateringIntent.putExtra(PlantWateringService.EXTRA_PLANT_ID, plantId);
        PendingIntent wateringPendingIntent = PendingIntent.getService(context, 0, wateringIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_water_button, wateringPendingIntent);

        return views;
    }

    /**
     * Creates and returns the RemoteViews to be displayed in the GridView mode widget
     *
     * @param context The context
     * @return The RemoteViews for the GridView mode widget
     */
    private static RemoteViews getGardenGridRemoteView(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid_view);
        // Set the GridWidgetService intent to act as the adapter for the GridView
        Intent intent = new Intent(context, GridWidgetService.class);
        views.setRemoteAdapter(R.id.widget_grid_view, intent);
        // Set the PlantDetailActivity intent to launch when clicked
        Intent appIntent = new Intent(context, PlantDetailActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_grid_view, appPendingIntent);
        // Handle empty gardens
        views.setEmptyView(R.id.widget_grid_view, R.id.empty_view);
        return views;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //Start the intent service update widget action, the service takes care of updating the widgets UI
        PlantWateringService.startActionUpdatePlantWidgets(context);
    }

    public static void updatePlantWidgets(Context context, AppWidgetManager appWidgetManager, int imgRes, long plantId, boolean showWater, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, imgRes, plantId, showWater, appWidgetId);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        PlantWateringService.startActionUpdatePlantWidgets(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

