package com.learnflow.ai;
import java.util.List;
public record AnswerResponse(String answer, List<Source> sources, String mode) {
    public record Source(long noteId, String title, String excerpt) {}
}
