package com.xuanniao.reader.item;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CatalogItem implements Parcelable {
    String platformName;
    String bookName;
    String bookCode;
    List<String> chapterCodeList;
    List<String> chapterTitleList;




    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public void setBookCode(String bookCode) {
        this.bookCode = bookCode;
    }

    public void setChapterCodeList(List<String> chapterCodeList) {
        this.chapterCodeList = chapterCodeList;
    }

    public void setChapterTitleList(List<String> chapterTitleList) {
        this.chapterTitleList = chapterTitleList;
    }

    public void addChapterCode(String chapterCode) {
        if (chapterCodeList == null) {
            chapterCodeList = new ArrayList<>();
        }
        this.chapterCodeList.add(chapterCode);
    }

    public void addChapterTitle(String chapterTitle) {
        if (chapterTitleList == null) {
            chapterTitleList = new ArrayList<>();
        }
        this.chapterTitleList.add(chapterTitle);
    }


    public String getPlatformName() {
        return platformName;
    }

    public String getBookName() {
        return bookName;
    }

    public String getBookCode() {
        return bookCode;
    }

    public List<String> getChapterCodeList() {
        return chapterCodeList;
    }

    public List<String> getChapterTitleList() {
        return chapterTitleList;
    }

    public String getChapterCode(int i) {
        return chapterCodeList.get(i);
    }

    public String getChapterTitle(int i) {
        return chapterTitleList.get(i);
    }


    @Override
    public int describeContents() {
        return Parcelable.CONTENTS_FILE_DESCRIPTOR;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(platformName);
        parcel.writeString(bookName);
        parcel.writeString(bookCode);
        parcel.writeString(String.join("/cc/", chapterCodeList));
        parcel.writeString(String.join("/cl/", chapterTitleList));
    }

    public static final Parcelable.Creator<CatalogItem> CREATOR = new Creator<CatalogItem>() {
        @Override
        public CatalogItem createFromParcel(Parcel source) {
            CatalogItem platformItem = new CatalogItem();
            platformItem.platformName = source.readString();
            platformItem.bookName = source.readString();
            platformItem.bookCode = source.readString();
            String[] cca = source.readString().split("/cc/");
            List<String> ccaList = new ArrayList<String>(cca.length);
            Collections.addAll(ccaList, cca);
            platformItem.chapterCodeList = ccaList;
            String[] cta = source.readString().split("/cl/");
            List<String> ctaList = new ArrayList<String>(cta.length);
            Collections.addAll(ctaList, cta);
            platformItem.chapterTitleList = ctaList;
            return platformItem;
        }

        @Override
        public CatalogItem[] newArray(int size){
            return new CatalogItem[size];
        }
    };
}
