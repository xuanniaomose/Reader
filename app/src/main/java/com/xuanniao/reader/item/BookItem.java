package com.xuanniao.reader.item;

import android.os.Parcel;
import android.os.Parcelable;
import com.xuanniao.reader.tools.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BookItem implements Serializable {
    String bookName;
    String uriStr;
    String author;
    String synopsis;
    int renewTime;
    int wordCount;
    String coverUrl;
    List<Integer> chapterReadList;
    List<Integer> chapterSavedList;
    int chapterTotal;
    int bookMark;

    String publisher;
    String classify;
    String status;
    String platformName;
    String bookCode;
    String mimeType;
    String volumeName;
    int volumeIndex;
    int publishDate;
    int downloadTime;


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
        this.renewTime = (int) (renewTime / 1000);
    }
    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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

    public void setBookMark(int bookMark) {
        this.bookMark = bookMark;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setClassify(String classify) {
        this.classify = classify;
    }

    public void setStatus(String status) {
        this.status= status;
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
        this.publishDate = (int) (publishDate / 1000);
    }

    public void setDownloadTime(long downloadTime) {
        this.downloadTime = (int) (downloadTime / 1000);
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
        return renewTime * 1000L;
    }
    public int getWordCount() {
        return wordCount;
    }

    public String getCoverUrl() {
        return coverUrl;
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

    public int getBookMark() {
        return bookMark;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getClassify() {
        return classify;
    }

    public String getStatus() {
        return status;
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
        return publishDate * 1000L;
    }

    public long getDownloadTime() {
        return downloadTime * 1000L;
    }


//    @Override
//    public int describeContents() {
//        return Parcelable.CONTENTS_FILE_DESCRIPTOR;
//    }
//
//    @Override
//    public void writeToParcel(Parcel parcel, int i) {
//        parcel.writeString(bookName);
//        parcel.writeString(Objects.requireNonNullElse(uriStr, ""));
//        parcel.writeString(Objects.requireNonNullElse(author, ""));
//        parcel.writeString(Objects.requireNonNullElse(synopsis, ""));
//        parcel.writeInt(renewTime);
//        parcel.writeInt(wordCount);
//        parcel.writeString(Objects.requireNonNullElse(coverUrl, ""));
//        if (chapterReadList != null && !chapterReadList.isEmpty()) {
//            parcel.writeString(listToString(chapterReadList));
//        } else {
//            parcel.writeString("");
//        }
//        if (chapterSavedList != null && !chapterSavedList.isEmpty()) {
//            parcel.writeString(listToString(chapterSavedList));
//        } else {
//            parcel.writeString("");
//        }
//        parcel.writeInt(chapterTotal);
//        parcel.writeInt(bookMark);
//
//        parcel.writeString(Objects.requireNonNullElse(publisher, ""));
//        parcel.writeString(Objects.requireNonNullElse(classify, ""));
//        parcel.writeString(Objects.requireNonNullElse(status, ""));
//        parcel.writeString(platformName);
//        parcel.writeString(Objects.requireNonNullElse(bookCode, ""));
//        parcel.writeString(Objects.requireNonNullElse(mimeType, ""));
//        parcel.writeString(Objects.requireNonNullElse(volumeName, ""));
//        parcel.writeInt(volumeIndex);
//        parcel.writeInt(publishDate);
//        parcel.writeInt(downloadTime);
//    }
//
//    public static final Parcelable.Creator<BookItem> CREATOR = new Creator<BookItem>() {
//        @Override
//        public BookItem createFromParcel(Parcel source) {
//            BookItem bookItem = new BookItem();
//            bookItem.bookName = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.uriStr = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.author = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.synopsis = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.renewTime = source.readInt();
//            bookItem.wordCount = source.readInt();
//            bookItem.coverUrl = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.chapterReadList = (Objects.equals(source.readString(), ""))?
//                    null :stringToList(source.readString());
//            bookItem.chapterSavedList = (Objects.equals(source.readString(), ""))?
//                    null :stringToList(source.readString());
//            bookItem.chapterTotal = source.readInt();
//            bookItem.bookMark = source.readInt();
//
//            bookItem.publisher = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.classify = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.status = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.platformName = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.bookCode = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.mimeType = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.volumeName = (Objects.equals(source.readString(), ""))?
//                    null : source.readString();
//            bookItem.volumeIndex = source.readInt();
//            bookItem.publishDate = source.readInt();
//            bookItem.downloadTime = source.readInt();
//            return bookItem;
//        }
//
//        @Override
//        public BookItem[] newArray(int size){
//            return new BookItem[size];
//        }
//    };
//
//    public String listToString(List<Integer> list) {
//        if (list == null || list.isEmpty()) return null;
//        StringBuilder string = new StringBuilder();
//        for (Integer numS : list) {
//            string.append(numS).append(Constants.INTEGER_DIVIDER);
//        }
//        string.delete(string.length() - 1, string.length());
//        return string.toString();
//    }
//
//    public static List<Integer> stringToList(String listString) {
//        if (listString == null || listString.isEmpty()) return null;
//        List<Integer> list = new ArrayList<>();
//        String[] listStrArray = listString.split(Constants.INTEGER_DIVIDER);
//        for (String numS : listStrArray) {
//            list.add(Integer.valueOf(numS));
//        }
//        return list;
//    }
}
