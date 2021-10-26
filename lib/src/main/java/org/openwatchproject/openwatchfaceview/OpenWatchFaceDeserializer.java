package org.openwatchproject.openwatchfaceview;

import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.openwatchproject.openwatchfaceview.item.AbstractItem;
import org.openwatchproject.openwatchfaceview.item.BalanceRotatableItem;
import org.openwatchproject.openwatchfaceview.item.BatteryRotatableItem;
import org.openwatchproject.openwatchfaceview.item.DayItem;
import org.openwatchproject.openwatchfaceview.item.DayRotatableItem;
import org.openwatchproject.openwatchfaceview.item.DistanceRotatableItem;
import org.openwatchproject.openwatchfaceview.item.KCalRotatableItem;
import org.openwatchproject.openwatchfaceview.item.MoonPhaseItem;
import org.openwatchproject.openwatchfaceview.item.Hour24RotatableItem;
import org.openwatchproject.openwatchfaceview.item.DayOfWeekItem;
import org.openwatchproject.openwatchfaceview.item.StepRotatableItem;
import org.openwatchproject.openwatchfaceview.item.WeatherItem;
import org.openwatchproject.openwatchfaceview.item.WeekdayRotatableItem;
import org.openwatchproject.openwatchfaceview.item.HourItem;
import org.openwatchproject.openwatchfaceview.item.HourMinuteItem;
import org.openwatchproject.openwatchfaceview.item.HourRotatableItem;
import org.openwatchproject.openwatchfaceview.item.HourShadowRotatableItem;
import org.openwatchproject.openwatchfaceview.item.MinuteItem;
import org.openwatchproject.openwatchfaceview.item.MinuteRotatableItem;
import org.openwatchproject.openwatchfaceview.item.MinuteShadowRotatableItem;
import org.openwatchproject.openwatchfaceview.item.MonthDayItem;
import org.openwatchproject.openwatchfaceview.item.MonthItem;
import org.openwatchproject.openwatchfaceview.item.MonthRotatableItem;
import org.openwatchproject.openwatchfaceview.item.RotatableItem;
import org.openwatchproject.openwatchfaceview.item.SecondItem;
import org.openwatchproject.openwatchfaceview.item.SecondRotatableItem;
import org.openwatchproject.openwatchfaceview.item.SecondShadowRotatableItem;
import org.openwatchproject.openwatchfaceview.item.SpecialSecondItem;
import org.openwatchproject.openwatchfaceview.item.StaticItem;
import org.openwatchproject.openwatchfaceview.item.TapActionItem;
import org.openwatchproject.openwatchfaceview.item.YearItem;
import org.openwatchproject.openwatchfaceview.item.YearMonthDayItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class OpenWatchFaceDeserializer {
    private static final String TAG = "OWFDeserializer";

    private final OpenWatchFaceFile watchFaceFile;
    private final Resources resources;
    private OpenWatchFace watchFace;

    public OpenWatchFaceDeserializer(OpenWatchFaceFile watchFaceFile, Resources resources) {
        this.watchFaceFile = watchFaceFile;
        this.resources = resources;
    }

    public OpenWatchFace deserialize() {
        JsonObject json;
        try (InputStreamReader isr = new InputStreamReader(watchFaceFile.getWatchFaceJson())) {
            json = JsonParser.parseReader(isr).getAsJsonObject();
        } catch (IOException e) {
            return null;
        }

        watchFace = new OpenWatchFace(json.get("width").getAsInt(), json.get("height").getAsInt());
        watchFace.setPath(watchFaceFile.getFile().getUri());

        JsonArray items = json.get("items").getAsJsonArray();
        for (JsonElement itemElement : items) {
            JsonObject itemObject = itemElement.getAsJsonObject();
            try {
                AbstractItem item = parseItem(itemObject);
                watchFace.addItem(item);
                if (item instanceof TapActionItem) {
                    watchFace.addTapActionItem((TapActionItem) item);
                }
            } catch (InvalidWatchFaceItemException e) {
                e.printStackTrace();
            }
        }

        return watchFace;
    }

    private AbstractItem parseItem(JsonObject json) throws InvalidWatchFaceItemException {
        AbstractItem item;
        int type;
        int centerX;
        int centerY;
        ArrayList<Drawable> frames = parseFrames(json);

        try {
            type = json.get("type").getAsInt();
            centerX = json.get("center_x").getAsInt();
            centerY = json.get("center_y").getAsInt();
        } catch (Exception e) {
            throw new InvalidWatchFaceItemException(e);
        }

        switch (type) {
            case OpenWatchFaceConstants.TYPE_STATIC:
                item = new StaticItem(centerX, centerY, frames);
                parseStaticItem((StaticItem) item, json);
                break;
            case OpenWatchFaceConstants.TYPE_ROTATABLE:
                item = parseRotatableItem(centerX, centerY, frames, json);
                break;
            case OpenWatchFaceConstants.TYPE_YEAR_MONTH_DAY:
                item = new YearMonthDayItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_MONTH_DAY:
                item = new MonthDayItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_MONTH:
                item = new MonthItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_DAY:
                item = new DayItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_WEEKDAY:
                item = new DayOfWeekItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_HOUR_MINUTE:
                item = new HourMinuteItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_HOUR:
                item = new HourItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_MINUTE:
                item = new MinuteItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_SECOND:
                item = new SecondItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_WEATHER:
                item = new WeatherItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_TEMPERATURE:
                Log.d(TAG, "parseItem: TYPE_TEMPERATURE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_STEPS:
                Log.d(TAG, "parseItem: TYPE_STEPS requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_HEART_RATE:
                Log.d(TAG, "parseItem: TYPE_HEART_RATE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_BATTERY:
                Log.d(TAG, "parseItem: TYPE_BATTERY requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_SPECIAL_SECOND:
                item = new SpecialSecondItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_YEAR:
                item = new YearItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_BATTERY_PICTURE_CIRCLE:
                Log.d(TAG, "parseItem: TYPE_BATTERY_PICTURE_CIRCLE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_STEPS_PICTURE_CIRCLE:
                Log.d(TAG, "parseItem: TYPE_STEPS_PICTURE_CIRCLE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_MOON_PHASE:
                item = new MoonPhaseItem(centerX, centerY, frames);
                break;
            case OpenWatchFaceConstants.TYPE_YEAR_2:
                Log.d(TAG, "parseItem: TYPE_YEAR_2 requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_MISSED_CALLS:
                Log.d(TAG, "parseItem: TYPE_MISSED_CALLS requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_MISSED_SMS:
                Log.d(TAG, "parseItem: TYPE_MISSED_SMS requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_BATTERY_CIRCLE:
                Log.d(TAG, "parseItem: TYPE_BATTERY_CIRCLE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_STEPS_PICTURE_WITH_CIRCLE_2:
                Log.d(TAG, "parseItem: TYPE_STEPS_PICTURE_WITH_CIRCLE_2 requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_KCAL:
                Log.d(TAG, "parseItem: TYPE_KCAL requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_MISSED_CALLS_SMS:
                Log.d(TAG, "parseItem: TYPE_MISSED_CALLS_SMS requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_STEPS_CIRCLE:
                Log.d(TAG, "parseItem: TYPE_STEPS_CIRCLE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_KCAL_CIRCLE:
                Log.d(TAG, "parseItem: TYPE_KCAL_CIRCLE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_POWER_CIRCLE:
                Log.d(TAG, "parseItem: TYPE_POWER_CIRCLE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_DISTANCE_CIRCLE:
                Log.d(TAG, "parseItem: TYPE_DISTANCE_CIRCLE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_DISTANCE:
                Log.d(TAG, "parseItem: TYPE_DISTANCE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_BATTERY_IMAGE:
                Log.d(TAG, "parseItem: TYPE_BATTERY_IMAGE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_UNKNOWN_1:
                Log.d(TAG, "parseItem: TYPE_UNKNOWN_1 requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_BATTERY_IMAGE_CHARGING:
                Log.d(TAG, "parseItem: TYPE_BATTERY_IMAGE_CHARGING requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_TEXT_PEDOMETER:
                Log.d(TAG, "parseItem: TYPE_TEXT_PEDOMETER requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_TEXT_HEARTRATE:
                Log.d(TAG, "parseItem: TYPE_TEXT_HEARTRATE requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_CHARGING:
                Log.d(TAG, "parseItem: TYPE_CHARGING requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            case OpenWatchFaceConstants.TYPE_TAP_ACTION:
                item = new TapActionItem(centerX, centerY, frames);
                parseTapActionItem((TapActionItem) item, json);
                Log.d(TAG, "parseItem: touch parsed");
                break;
            case OpenWatchFaceConstants.TYPE_YEAR_MONTH_DAY_2:
                Log.d(TAG, "parseItem: TYPE_YEAR_MONTH_DAY_2 requested, but not implemented!");
                throw new InvalidWatchFaceItemException();
            default:
                throw new InvalidWatchFaceItemException();
        }

        return item;
    }

    private ArrayList<Drawable> parseFrames(JsonObject json) {
        ArrayList<Drawable> frames = new ArrayList<>();
        JsonArray frameArray = json.get("frames").getAsJsonArray();

        for (JsonElement je : frameArray) {
            String frame = je.getAsString();
            try (InputStream is = watchFaceFile.getZippedFile(frame)) {
                frames.add(new BitmapDrawable(resources, BitmapFactory.decodeStream(is)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return frames;
    }

    private void parseStaticItem(StaticItem item, JsonObject json) {

    }

    private void parseTapActionItem(TapActionItem item, JsonObject jsonObject) {
        parseStaticItem(item, jsonObject);
        item.setPackageName(jsonObject.get("packageName").getAsString());
        item.setClassName(jsonObject.get("className").getAsString());
        item.setRange(jsonObject.get("range").getAsInt());
    }

    private RotatableItem parseRotatableItem(int centerX, int centerY, List<Drawable> frames, JsonObject json) throws InvalidWatchFaceItemException {
        RotatableItem item;
        int rotatableType;
        float startAngle;
        float maxAngle;
        int direction;
        JsonElement tmp;

        tmp = json.get("rotatable_type");
        if (tmp != null) {
            rotatableType = tmp.getAsInt();
        } else {
            throw new InvalidWatchFaceItemException();
        }

        tmp = json.get("start_angle");
        if (tmp != null) {
            startAngle = tmp.getAsFloat();
        } else {
            startAngle = 0;
        }

        tmp = json.get("max_angle");
        if (tmp != null) {
            maxAngle = tmp.getAsFloat();
        } else {
            maxAngle = 360;
        }

        tmp = json.get("direction");
        if (tmp != null) {
            direction = tmp.getAsInt();
        } else {
            direction = OpenWatchFaceConstants.DIRECTION_CLOCKWISE;
        }

        switch (rotatableType) {
            case OpenWatchFaceConstants.ROTATABLE_HOUR:
                item = new HourRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_MINUTE:
                item = new MinuteRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_SECOND:
                item = new SecondRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_MONTH:
                item = new MonthRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_WEEKDAY:
                item = new WeekdayRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_BATTERY:
                item = new BatteryRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_HOUR_24:
                item = new Hour24RotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_HOUR_SHADOW:
                item = new HourShadowRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_MINUTE_SHADOW:
                item = new MinuteShadowRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_SECOND_SHADOW:
                item = new SecondShadowRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_DAY:
                item = new DayRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_BALANCE:
                item = new BalanceRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_STEP:
                item = new StepRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_KCAL:
                item = new KCalRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            case OpenWatchFaceConstants.ROTATABLE_DISTANCE:
                item = new DistanceRotatableItem(centerX, centerY, frames, startAngle, maxAngle, direction);
                break;
            default:
                throw new InvalidWatchFaceItemException();
        }

        return item;
    }
}
