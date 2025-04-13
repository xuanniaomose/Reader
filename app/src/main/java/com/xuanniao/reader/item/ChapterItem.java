package com.xuanniao.reader.item;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChapterItem implements Serializable {
    boolean isLocal;
    String bookName;
    String bookCode;
    String title;
    int chapterNum;
    String chapterCode;
    List<String> chapter;
    List<String> chapterPageCodeList;


    public void setIsLocal(boolean isLocal) {
        this.isLocal = isLocal;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getBookCode() {
        return bookCode;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setChapterCode(String chapterCode) {
        this.chapterCode = chapterCode;
    }

    public void setChapterNum(int chapterNum) {
        this.chapterNum = chapterNum;
    }

    public void setChapter(List<String> chapter) {
        this.chapter = chapter;
    }

    public void addParagraph(String paragraph) {
        if (this.chapter == null) {
            this.chapter = new ArrayList<>();
        }
        this.chapter.add(paragraph);
    }

    public void appendParagraph(List<String> paragraphList) {
        if (this.chapter == null) {
            this.chapter = new ArrayList<>();
        }
        this.chapter.addAll(paragraphList);
    }

    public void setChapterPageCodeList(List<String> chapterPageCodeList) {
        this.chapterPageCodeList = chapterPageCodeList;
    }

    public void addChapterPageCode(String chapterCode) {
        if (chapterPageCodeList == null) {
            chapterPageCodeList = new ArrayList<>();
        }
        this.chapterPageCodeList.add(chapterCode);
    }

    public boolean getIsLocal() {
        return isLocal;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookCode(String bookCode) {
        this.bookCode = bookCode;
    }

    public String getTitle() {
        return title;
    }

    public int getChapterNum() {
        return chapterNum;
    }

    public String getChapterCode() {
        return chapterCode;
    }

    public List<String> getChapter() {
        return chapter;
    }

    public String getParagraph(int i) {
        if (this.chapter == null) {
            return null;
        }
        return chapter.get(i);
    }

    public List<String> getChapterPageCodeList() {
        return chapterPageCodeList;
    }
}
