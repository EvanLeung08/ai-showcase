// SlideContent.java
package com.evan.ai.utils;

import java.util.ArrayList;
import java.util.List;

public class SlideContent {
    private String title;
    private List<String> body;

    public SlideContent() {
        this.body = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getBody() {
        return body;
    }

    public void addBody(String bodyText) {
        this.body.add(bodyText);
    }
}