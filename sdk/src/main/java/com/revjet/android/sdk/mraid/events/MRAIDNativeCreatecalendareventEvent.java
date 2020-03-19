/*
 * RevJet Android SDK
 *
 * Copyright (c) 2017 RevJet. All rights reserved.
 */

package com.revjet.android.sdk.mraid.events;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import com.revjet.android.sdk.commons.Utils;
import com.revjet.android.sdk.mraid.MRAIDController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static com.revjet.android.sdk.commons.RevJetLogger.LOGGER;

public class MRAIDNativeCreatecalendareventEvent extends MRAIDNativeEvent {

    private static final int MAX_NUMBER_DAYS_IN_MONTH = 31;

    @Override
    public void execute(Map<String, String> parameters, MRAIDController controller) {
        super.execute(parameters, controller);

        try {
            createCalendarEvent(parameters, controller);
        } catch (Exception exception) {
            String errorMessage = "Cannot create calendar event";
            controller.reportError(errorMessage, "createCalendarEvent");
            LOGGER.info(errorMessage);
            return;
        }
    }

    private void createCalendarEvent(Map<String, String> parameters, MRAIDController controller) {
        Context context = controller.getContext();
        if (Utils.isCalendarAvailable(context)) {

            String startDateString = parameters.get("start");
            String description = parameters.get("description");
            if (null == startDateString || null == description) {
                String errorMessage = "Start date and description are missing.";
                controller.reportError(errorMessage, "createCalendarEvent");
                LOGGER.info(errorMessage);
                return;
            }

            Date startDate = parseDate(startDateString);
            if (null == startDate) {
                String errorMessage = "Wrong date format for start date. " +
                        "Available formats: yyyy-MM-dd'T'HH:mm:ssZZZZZ, yyyy-MM-dd'T'HH:mmZZZZZ";
                controller.reportError(errorMessage, "createCalendarEvent");
                LOGGER.info(errorMessage);
                return;
            }

            String endDateString = parameters.get("end");
            Date endDate = null;
            if (null != endDateString) {
                endDate = parseDate(endDateString);
                if (null == endDate) {
                    String errorMessage = "Wrong date format for end date. " +
                            "Available formats: yyyy-MM-dd'T'HH:mm:ssZZZZZ, yyyy-MM-dd'T'HH:mmZZZZZ";
                    controller.reportError(errorMessage, "createCalendarEvent");
                    LOGGER.info(errorMessage);
                    return;
                }
            }

            Intent calendarIntent = new Intent(Intent.ACTION_INSERT).setType(
                    MRAIDNativeEventFactory.ANDROID_CALENDAR_CONTENT_TYPE);
            calendarIntent.putExtra(CalendarContract.Events.TITLE, description);
            calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startDate.getTime());
            if (null != endDate) {
                calendarIntent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endDate.getTime());
            }

            if (parameters.containsKey("location")) {
                calendarIntent.putExtra(CalendarContract.Events.EVENT_LOCATION, parameters.get("location"));
            }

            if (parameters.containsKey("summary")) {
                calendarIntent.putExtra(CalendarContract.Events.DESCRIPTION, parameters.get("summary"));
            }

            if (parameters.containsKey("transparency")) {
                calendarIntent.putExtra(CalendarContract.Events.AVAILABILITY,
                        parameters.get("transparency").equals("transparent") ?
                        CalendarContract.Events.AVAILABILITY_FREE :
                        CalendarContract.Events.AVAILABILITY_BUSY
                );
            }

            try {
                calendarIntent.putExtra(CalendarContract.Events.RRULE, parseRecurrenceRule(parameters));
            } catch (IllegalArgumentException illegalArugemnt) {
                String errorMessage = "Wrong parameter:" + illegalArugemnt.getMessage();
                controller.reportError(errorMessage, "createCalendarEvent");
                LOGGER.info(errorMessage);
                return;
            }

            calendarIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(calendarIntent);
        } else {
            controller.reportError("Not supported.", "createCalendarEvent");
            LOGGER.info("createCalendarEvent is not supported for current device.");
        }
    }

    private Date parseDate(String dateTime) {
        Date result = null;
        String[] dateFormats = {"yyyy-MM-dd'T'HH:mm:ssZZZZZ", "yyyy-MM-dd'T'HH:mmZZZZZ"};
        for (int dateFormatIndex = 0; dateFormatIndex < dateFormats.length; ++dateFormatIndex) {
            try {
                result = new SimpleDateFormat(dateFormats[dateFormatIndex]).parse(dateTime);
                if (result != null) {
                    break;
                }
            } catch (ParseException e) {
            }
        }
        return result;
    }

    private String parseRecurrenceRule(Map<String, String> params) throws IllegalArgumentException {
        StringBuilder rule = new StringBuilder();
        if (params.containsKey("frequency")) {
            String frequency = params.get("frequency");
            int interval = -1;
            if (params.containsKey("interval")) {
                interval = Integer.parseInt(params.get("interval"));
            }
            if ("daily".equals(frequency)) {
                rule.append("FREQ=DAILY;");
                if (interval != -1) {
                    rule.append("INTERVAL=" + interval + ";");
                }
            } else if("weekly".equals(frequency)) {
                rule.append("FREQ=WEEKLY;");
                if (interval != -1) {
                    rule.append("INTERVAL=" + interval + ";");
                }
                if (params.containsKey("daysInWeek")) {
                    String weekdays = translateWeekIntegersToDays(params.get("daysInWeek"));
                    if (weekdays == null) {
                        throw new IllegalArgumentException("invalid ");
                    }
                    rule.append("BYDAY=" + weekdays + ";");
                }
            } else if("monthly".equals(frequency)) {
                rule.append("FREQ=MONTHLY;");
                if (interval != -1) {
                    rule.append("INTERVAL=" + interval + ";");
                }
                if (params.containsKey("daysInMonth")) {
                    String monthDays = translateMonthIntegersToDays(params.get("daysInMonth"));
                    if (monthDays == null) {
                        throw new IllegalArgumentException();
                    }
                    rule.append("BYMONTHDAY=" + monthDays + ";");
                }
            } else {
                throw new IllegalArgumentException("Wrong recurrence rule.");
            }
        }
        return rule.toString();
    }

    private String translateWeekIntegersToDays(String expression) throws IllegalArgumentException {
        StringBuilder daysResult = new StringBuilder();
        boolean[] daysAlreadyCounted = new boolean[7];
        String[] days = expression.split(",");
        int dayNumber;
        for (int i = 0; i< days.length; i++) {
            dayNumber = Integer.parseInt(days[i]);
            dayNumber = dayNumber == 7 ? 0 : dayNumber;
            if (!daysAlreadyCounted[dayNumber]) {
                daysResult.append(dayOfWeekString(dayNumber) + ",");
                daysAlreadyCounted[dayNumber] = true;
            }
        }
        if (days.length == 0) {
            throw new IllegalArgumentException("Does not have day of week.");
        }
        daysResult.deleteCharAt(daysResult.length()-1);
        return daysResult.toString();
    }

    private String translateMonthIntegersToDays(String expression) throws IllegalArgumentException {
        StringBuilder daysResult = new StringBuilder();
        boolean[] daysAlreadyCounted = new boolean[2 * MAX_NUMBER_DAYS_IN_MONTH +1];
        String[] days = expression.split(",");
        int dayNumber;
        for (int i = 0; i< days.length; i++) {
            dayNumber = Integer.parseInt(days[i]);
            if (!daysAlreadyCounted[dayNumber + MAX_NUMBER_DAYS_IN_MONTH]) {
                daysResult.append(dayNumberToDayOfMonthString(dayNumber) + ",");
                daysAlreadyCounted[dayNumber + MAX_NUMBER_DAYS_IN_MONTH] = true;
            }
        }
        if (days.length == 0) {
            throw new IllegalArgumentException("Does not have day of month.");
        }
        daysResult.deleteCharAt(daysResult.length() - 1);
        return daysResult.toString();
    }

    private String dayOfWeekString(int number) throws IllegalArgumentException {
        String dayOfWeek;
        switch(number) {
            case 0: dayOfWeek="SU"; break;
            case 1: dayOfWeek="MO"; break;
            case 2: dayOfWeek="TU"; break;
            case 3: dayOfWeek="WE"; break;
            case 4: dayOfWeek="TH"; break;
            case 5: dayOfWeek="FR"; break;
            case 6: dayOfWeek="SA"; break;
            default: throw new IllegalArgumentException("Wrong day of week " + number);
        }
        return dayOfWeek;
    }

    private String dayNumberToDayOfMonthString(int number) throws IllegalArgumentException {
        String dayOfMonth;
        if (number != 0 && number >= -MAX_NUMBER_DAYS_IN_MONTH && number <= MAX_NUMBER_DAYS_IN_MONTH) {
            dayOfMonth = "" + number;
        } else {
            throw new IllegalArgumentException("Wrong day of month " + number);
        }
        return dayOfMonth;
    }
}
