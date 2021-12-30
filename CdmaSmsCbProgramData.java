package com.quectel.jnitestexec.cdma2;

import android.os.Parcel;
import android.os.Parcelable;

public class CdmaSmsCbProgramData implements Parcelable {

    /** Delete the specified service category from the list of enabled categories. */
    public static final int OPERATION_DELETE_CATEGORY   = 0;

    /** Add the specified service category to the list of enabled categories. */
    public static final int OPERATION_ADD_CATEGORY      = 1;

    /** Clear all service categories from the list of enabled categories. */
    public static final int OPERATION_CLEAR_CATEGORIES  = 2;

    /** Alert option: no alert. */
    public static final int ALERT_OPTION_NO_ALERT               = 0;

    /** Alert option: default alert. */
    public static final int ALERT_OPTION_DEFAULT_ALERT          = 1;

    /** Alert option: vibrate alert once. */
    public static final int ALERT_OPTION_VIBRATE_ONCE           = 2;

    /** Alert option: vibrate alert - repeat. */
    public static final int ALERT_OPTION_VIBRATE_REPEAT         = 3;

    /** Alert option: visual alert once. */
    public static final int ALERT_OPTION_VISUAL_ONCE            = 4;

    /** Alert option: visual alert - repeat. */
    public static final int ALERT_OPTION_VISUAL_REPEAT          = 5;

    /** Alert option: low-priority alert once. */
    public static final int ALERT_OPTION_LOW_PRIORITY_ONCE      = 6;

    /** Alert option: low-priority alert - repeat. */
    public static final int ALERT_OPTION_LOW_PRIORITY_REPEAT    = 7;

    /** Alert option: medium-priority alert once. */
    public static final int ALERT_OPTION_MED_PRIORITY_ONCE      = 8;

    /** Alert option: medium-priority alert - repeat. */
    public static final int ALERT_OPTION_MED_PRIORITY_REPEAT    = 9;

    /** Alert option: high-priority alert once. */
    public static final int ALERT_OPTION_HIGH_PRIORITY_ONCE     = 10;

    /** Alert option: high-priority alert - repeat. */
    public static final int ALERT_OPTION_HIGH_PRIORITY_REPEAT   = 11;

    /** Service category operation (add/delete/clear). */
    private final int mOperation;

    /** Service category to modify. */
    private final int mCategory;

    /** Language used for service category name (defined in BearerData.LANGUAGE_*). */
    private final int mLanguage;

    /** Maximum number of messages to store for this service category. */
    private final int mMaxMessages;

    /** Service category alert option. */
    private final int mAlertOption;

    /** Name of service category. */
    private final String mCategoryName;

    /** Create a new CdmaSmsCbProgramData object with the specified values. */
    public CdmaSmsCbProgramData(int operation, int category, int language, int maxMessages,
                                int alertOption, String categoryName) {
        mOperation = operation;
        mCategory = category;
        mLanguage = language;
        mMaxMessages = maxMessages;
        mAlertOption = alertOption;
        mCategoryName = categoryName;
    }

    /** Create a new CdmaSmsCbProgramData object from a Parcel. */
    CdmaSmsCbProgramData(Parcel in) {
        mOperation = in.readInt();
        mCategory = in.readInt();
        mLanguage = in.readInt();
        mMaxMessages = in.readInt();
        mAlertOption = in.readInt();
        mCategoryName = in.readString();
    }

    /**
     * Flatten this object into a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written (ignored).
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mOperation);
        dest.writeInt(mCategory);
        dest.writeInt(mLanguage);
        dest.writeInt(mMaxMessages);
        dest.writeInt(mAlertOption);
        dest.writeString(mCategoryName);
    }

    /**
     * Returns the service category operation, e.g. {@link #OPERATION_ADD_CATEGORY}.
     * @return one of the {@code OPERATION_*} values
     */
    public int getOperation() {
        return mOperation;
    }

    /**
     * Returns the CDMA service category to modify.
     * @return a 16-bit CDMA service category value
     */
    public int getCategory() {
        return mCategory;
    }

    /**
     * Returns the CDMA language code for this service category.
     * @return one of the language values defined in BearerData.LANGUAGE_*
     */
    public int getLanguage() {
        return mLanguage;
    }

    /**
     * Returns the maximum number of messages to store for this service category.
     * @return the maximum number of messages to store for this service category
     */
    public int getMaxMessages() {
        return mMaxMessages;
    }

    /**
     * Returns the service category alert option, e.g. {@link #ALERT_OPTION_DEFAULT_ALERT}.
     * @return one of the {@code ALERT_OPTION_*} values
     */
    public int getAlertOption() {
        return mAlertOption;
    }

    /**
     * Returns the service category name, in the language specified by {@link #getLanguage()}.
     * @return an optional service category name
     */
    public String getCategoryName() {
        return mCategoryName;
    }

    @Override
    public String toString() {
        return "CdmaSmsCbProgramData{operation=" + mOperation + ", category=" + mCategory
                + ", language=" + mLanguage + ", max messages=" + mMaxMessages
                + ", alert option=" + mAlertOption + ", category name=" + mCategoryName + '}';
    }

    /**
     * Describe the kinds of special objects contained in the marshalled representation.
     * @return a bitmask indicating this Parcelable contains no special objects
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /** Creator for unparcelling objects. */
    public static final Parcelable.Creator<CdmaSmsCbProgramData>
            CREATOR = new Parcelable.Creator<CdmaSmsCbProgramData>() {
        @Override
        public CdmaSmsCbProgramData createFromParcel(Parcel in) {
            return new CdmaSmsCbProgramData(in);
        }

        @Override
        public CdmaSmsCbProgramData[] newArray(int size) {
            return new CdmaSmsCbProgramData[size];
        }
    };
}

