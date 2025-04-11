package com.xuanniao.reader.item;

import java.util.ArrayList;
import java.util.List;

public class ChapterItem {
    int isLocal;
    String bookName;
    String title;
    int chapterNum;
    String chapterCode;
    List<String> chapter;


    public void setIsLocal(int isLocal) {
        this.isLocal = isLocal;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
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


    public int getIsLocal() {
        return isLocal;
    }

    public String getBookName() {
        return bookName;
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
}
