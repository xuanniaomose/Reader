package com.xuanniao.reader.ui;

import java.util.ArrayList;
import java.util.List;

public class BookItem {
    String bookName;
    String uriStr;
    String author;
    String synopsis;
    long renewTime;
    List<Integer> chapterReadList;
    List<Integer> chapterSavedList;
    int chapterTotal;
    String publisher;
    String classify;
    String platformName;
    String bookCode;
    String mimeType;
    String volumeName;
    int volumeIndex;
    long publishDate;
    long downloadTime;


    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public void setUriStr(String uriStr) {
        this.uriStr = uriStr;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public void setRenewTime(long renewTime) {
        this.renewTime = renewTime;
    }

    public void setChapterReadList(List<Integer> chapterReadList) {
        this.chapterReadList = chapterReadList;
    }

    public void addChapterReadList(int chapterPageNum) {
        if (chapterReadList == null) {
            chapterReadList = new ArrayList<>();
        }
        this.chapterReadList.add(chapterPageNum);
    }

    public void setChapterSavedList(List<Integer> chapterSavedList) {
        this.chapterSavedList = chapterSavedList;
    }

    public void addChapterSavedList(int chapterSavedNum) {
        if (chapterSavedList == null) {
            chapterSavedList = new ArrayList<>();
        }
        this.chapterSavedList.add(chapterSavedNum);
    }

    public void setChapterTotal(int chapterTotal) {
        this.chapterTotal = chapterTotal;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setClassify(String classify) {
        this.classify = classify;
    }

    public void setBookCode(String bookCode) {
        this.bookCode = bookCode;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public void setVolumeIndex(int volumeIndex) {
        this.volumeIndex = volumeIndex;
    }

    public void setPublishDate(long publishDate) {
        this.publishDate = publishDate;
    }

    public void setDownloadTime(long downloadTime) {
        this.downloadTime = downloadTime;
    }



    public String getBookName() {
        return bookName;
    }

    public String getUriStr() {
        return uriStr;
    }

    public String getAuthor() {
        return author;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public long getRenewTime() {
        return renewTime;
    }

    public List<Integer> getChapterReadList() {
        return chapterReadList;
    }

    public List<Integer> getChapterSavedList() {
        return chapterSavedList;
    }

    public int getChapterTotal() {
        return chapterTotal;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getClassify() {
        return classify;
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getBookCode() {
        return bookCode;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public int getVolumeIndex() {
        return volumeIndex;
    }

    public long getPublishDate() {
        return publishDate;
    }

    public long getDownloadTime() {
        return downloadTime;
    }
}
