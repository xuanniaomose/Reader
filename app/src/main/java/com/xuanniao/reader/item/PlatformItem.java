package com.xuanniao.reader.item;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Arrays;

public class PlatformItem implements Parcelable {
    static String Tag = "PlatformItem";
    int ID;
    String platformName;
    String platformUrl;
    String platformCookie;
    String charsetName;

    String searchPath;
    String infoPath;
    String catalogPath;
    String chapterPath;

    String[] resultPage;
    String resultError;
    String resultPageFormat;

    String[] infoPage;
    String infoError;
    String infoPageFormat;

    String[] catalogPage;
    String catalogError;
    String catalogPageFormat;

    String[] chapterPage;
    String chapterError;
    String chapterPageFormat;

    String accurateSearch;


    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getPlatformName() {
        return platformName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    public String getSearchPath() {
        return searchPath;
    }

    public void setSearchPath(String searchPath) {
        this.searchPath = searchPath;
    }

    public String getInfoPath() {
        return infoPath;
    }

    public void setInfoPath(String infoPath) {
        this.infoPath = infoPath;
    }

    public String getCatalogPath() {
        return catalogPath;
    }

    public void setCatalogPath(String catalogPath) {
        this.catalogPath = catalogPath;
    }

    public String getChapterPath() {
        return chapterPath;
    }

    public void setChapterPath(String chapterPath) {
        this.chapterPath = chapterPath;
    }

    public String getPlatformCookie() {
        return platformCookie;
    }

    public void setPlatformCookie(String platformCookie) {
        this.platformCookie = platformCookie;
    }

    public String getCharsetName() {
        return charsetName;
    }

    public void setCharsetName(String charsetName) {
        this.charsetName = charsetName;
    }


    public String[] getResultPage() {
        return resultPage;
    }

    public void setResultPage(String[] resultPage) {
        this.resultPage = resultPage;
        Log.d(Tag, "resultPage:" + Arrays.toString(resultPage));
    }

    public String getResultError() {
        return resultError;
    }

    public void setResultError(String resultError) {
        this.resultError = resultError;
    }

    public String getResultPageFormat() {
        return resultPageFormat;
    }

    public void setResultPageFormat(String resultPageFormat) {
        this.resultPageFormat = resultPageFormat;
    }

    public String[] getInfoPage() {
        return infoPage;
    }

    public void setInfoPage(String[] infoPage) {
        this.infoPage = infoPage;
    }

    public String getInfoError() {
        return infoError;
    }

    public void setInfoError(String infoError) {
        this.infoError = infoError;
    }

    public String getInfoPageFormat() {
        return infoPageFormat;
    }

    public void setInfoPageFormat(String infoPageFormat) {
        this.infoPageFormat = infoPageFormat;
    }

    public String[] getCatalogPage() {
        return catalogPage;
    }

    public void setCatalogPage(String[] catalogPage) {
        this.catalogPage = catalogPage;
    }

    public String getCatalogError() {
        return catalogError;
    }

    public void setCatalogError(String catalogError) {
        this.catalogError = catalogError;
    }

    public String getCatalogPageFormat() {
        return catalogPageFormat;
    }

    public void setCatalogPageFormat(String catalogPageFormat) {
        this.catalogPageFormat = catalogPageFormat;
    }


    public String[] getChapterPage() {
        return chapterPage;
    }

    public void setChapterPage(String[] chapterPage) {
        this.chapterPage = chapterPage;
    }

    public String getChapterError() {
        return chapterError;
    }

    public void setChapterError(String chapterError) {
        this.chapterError = chapterError;
    }

    public String getChapterPageFormat() {
        return chapterPageFormat;
    }

    public void setChapterPageFormat(String chapterPageFormat) {
        this.chapterPageFormat = chapterPageFormat;
    }

    public String getAccurateSearch() {
        return accurateSearch;
    }

    public void setAccurateSearch(String accurateSearch) {
        this.accurateSearch = accurateSearch;
    }

    @Override
    public int describeContents() {
        return Parcelable.CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(ID);
        parcel.writeString(platformName);
        parcel.writeString(platformUrl);
        parcel.writeString(searchPath);
        parcel.writeString(platformCookie);
        parcel.writeString(charsetName);

        if (resultPage != null && resultPage.length > 0) {
//            Log.d(Tag, String.join(",", resultPage));
            parcel.writeString(String.join(",", resultPage));
        }
        parcel.writeString(resultError);
        parcel.writeString(resultPageFormat);

        if (infoPage != null && infoPage.length > 0) {
            parcel.writeString(String.join(",", infoPage));
        }
        parcel.writeString(infoError);
        parcel.writeString(infoPageFormat);

        if (catalogPage != null && catalogPage.length > 0) {
            parcel.writeString(String.join(",", catalogPage));
        }
        parcel.writeString(catalogError);
        parcel.writeString(catalogPageFormat);

        if (chapterPage != null && chapterPage.length > 0) {
            parcel.writeString(String.join(",", chapterPage));
        }
        parcel.writeString(chapterError);
        parcel.writeString(chapterPageFormat);
        parcel.writeString(accurateSearch);
    }

    public static final Parcelable.Creator<PlatformItem> CREATOR = new Creator<PlatformItem>() {
        @Override
        public PlatformItem createFromParcel(Parcel source) {
            PlatformItem platformItem = new PlatformItem();
            platformItem.ID = source.readInt();
            platformItem.platformName = source.readString();
            platformItem.platformUrl = source.readString();
            platformItem.searchPath = source.readString();
            platformItem.platformCookie = source.readString();
            platformItem.charsetName = source.readString();

            platformItem.resultPage = source.readString().split(",");
            platformItem.resultError = source.readString();
            platformItem.resultPageFormat = source.readString();

            platformItem.infoPage = source.readString().split(",");
            platformItem.infoError = source.readString();
            platformItem.infoPageFormat = source.readString();

            platformItem.catalogPage = source.readString().split(",");
            platformItem.catalogError = source.readString();
            platformItem.catalogPageFormat = source.readString();

            platformItem.chapterPage = source.readString().split(",");
            platformItem.chapterError = source.readString();
            platformItem.chapterPageFormat = source.readString();

            platformItem.accurateSearch = source.readString();
            return platformItem;
        }

        @Override
        public PlatformItem[] newArray(int size){
            return new PlatformItem[size];
        }
    };
}
